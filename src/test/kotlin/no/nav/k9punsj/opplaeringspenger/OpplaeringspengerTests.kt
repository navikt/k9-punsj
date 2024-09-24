package no.nav.k9punsj.opplaeringspenger

import com.fasterxml.jackson.module.kotlin.convertValue
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.felles.dto.OpprettNySøknad
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.felles.dto.SendSøknad
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.openapi.OasFeil
import no.nav.k9punsj.openapi.OasSoknadsfeil
import no.nav.k9punsj.util.LesFraFilUtil
import no.nav.k9punsj.util.SøknadJson
import no.nav.k9punsj.util.TestUtils.hentSøknadId
import no.nav.k9punsj.utils.objectMapper
import no.nav.k9punsj.wiremock.JournalpostIds
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.BodyInserters
import java.net.URI
import java.time.LocalDate


@ExtendWith(SpringExtension::class, MockKExtension::class)
@Disabled("OLP er ikke i bruk eller under utvikling")
class OpplaeringspengerTests : AbstractContainerBaseTest() {

    @Autowired
    lateinit var journalpostRepository: JournalpostRepository

    @BeforeEach
    internal fun setUp() {
        cleanUpDB()
    }

    @AfterEach
    internal fun tearDown() {
        cleanUpDB()
    }

    private val api = "api"
    private val søknadTypeUri = "opplaeringspenger-soknad"
    private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"
    private val objectMapper = objectMapper()


    @Test
    fun `Får tom liste når personen ikke har en eksisterende mappe`(): Unit = runBlocking {
        val norskIdent = "01110050053"
        hentMappeMedNorskIdent(norskIdent)
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody().jsonPath("$.søknader").isEmpty
    }


    @Test
    fun `Opprette ny mappe på person`(): Unit = runBlocking {
        val norskIdent = "01010050053"
        val journalpostid = "9999"
        val opprettNySøknad = lagOpprettNySøknadDto(norskIdent, journalpostid)

        webTestClient.post()
            .uri { it.pathSegment(api, søknadTypeUri).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(opprettNySøknad))
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CREATED)
            .expectBody()
    }


    @Test
    fun `Hente eksisterende mappe på person`(): Unit = runBlocking {
        val norskIdent = "02020050163"
        val journalpostid = "9999"
        val opprettNySøknad = lagOpprettNySøknadDto(norskIdent, journalpostid)

        opprettSøknadOgHentSøknadId(opprettNySøknad)

        hentMappeMedNorskIdent(norskIdent)
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody()
            .consumeWith { response ->
                assertNotNull(response.responseBody)
                val svarOlpDto = objectMapper.readValue(response.responseBody, SvarOlpDto::class.java)
                assertThat(svarOlpDto.søknader).isNotEmpty
                assertEquals(journalpostid, svarOlpDto.søknader?.first()?.journalposter?.first())
            }
    }


    @Test
    fun `Hent en søknad`(): Unit = runBlocking {
        val søknad = LesFraFilUtil.søknadFraFrontendOlpFull()
        val norskIdent = "02030050163"
        val journalpostid = "21707da8-a13b-4927-8776-c53399727b29"

        leggPåNySøkerIdOgJournalpostId(søknad, norskIdent, journalpostid)
        val opprettNySøknad = lagOpprettNySøknadDto(norskIdent, journalpostid)

        val location = opprettSøknadOgHentSøknadId(opprettNySøknad)
        val søknadId = hentSøknadId(location)

        hentMappeForSøknadId(søknadId)
            .expectBody()
            .consumeWith { res ->
                val søknadOlpDto = objectMapper.readValue(res.responseBody, OpplaeringspengerSøknadDto::class.java)
                assertNotNull(søknadOlpDto)
                assertEquals(journalpostid, søknadOlpDto.journalposter?.first())
            }
    }


    @Test
    fun `Oppdaterer en søknad`(): Unit = runBlocking {
        val norskIdent = "02030050163"
        val journalpostId = "9999"
        val k9Saksnummer = "12345"

        val søknad = OpprettNySøknad(norskIdent, journalpostId, k9Saksnummer, null)
        val søknadJson = LesFraFilUtil.søknadFraFrontendOlpFull()

        val locationMedSøknadId = opprettSøknadOgHentSøknadId(søknad)

        tilpassSøknadTilTest(søknadJson, norskIdent, journalpostId, locationMedSøknadId)
        oppdaterSøknad(søknadJson)
            .expectStatus().isOk
            .expectBody()
            .consumeWith { res ->
                val oppdatertSøknad = objectMapper.readValue(res.responseBody, OpplaeringspengerSøknadDto::class.java)
                assertNotNull(oppdatertSøknad)
                assertEquals(norskIdent, oppdatertSøknad.soekerId)
                assertEquals(
                    listOf(
                        PeriodeDto(
                            LocalDate.of(2018, 12, 30),
                            LocalDate.of(2019, 10, 20)
                        )
                    ),
                    oppdatertSøknad.soeknadsperiode
                )
            }
    }


    @Test
    fun `Oppdaterer en søknad med metadata`(): Unit = runBlocking {
        val søknadFraFrontend = LesFraFilUtil.søknadFraFrontendOlpFull()
        val norskIdent = "02030050163"
        val journalpostid = "9999"

        leggPåNySøkerIdOgJournalpostId(søknadFraFrontend, norskIdent, journalpostid)
        val opprettNySøknad = lagOpprettNySøknadDto(norskIdent, journalpostid)

        val opprettetSøknadResponse = webTestClient.post()
            .uri { it.pathSegment(api, søknadTypeUri).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(opprettNySøknad))
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CREATED)
            .expectHeader().exists("Location")
            .returnResult(URI::class.java)

        val location = opprettetSøknadResponse.responseHeaders.location
        val søknadId = hentSøknadId(location)
        leggPåNySøknadId(søknadFraFrontend, location)

        val oppdatertSøknadResponse = webTestClient.put()
            .uri { it.pathSegment(api, søknadTypeUri, "oppdater").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(søknadFraFrontend))
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody().consumeWith { res ->
                val oppdatertSøknad = objectMapper.readValue(res.responseBody, OpplaeringspengerSøknadDto::class.java)
                assertNotNull(oppdatertSøknad)
                assertEquals(norskIdent, oppdatertSøknad.soekerId)
                assertEquals(
                    listOf(
                        PeriodeDto(
                            LocalDate.of(2018, 12, 30),
                            LocalDate.of(2019, 10, 20)
                        )
                    ),
                    oppdatertSøknad.soeknadsperiode
                )
            }.returnResult()

        val oppdatertSøknad =
            objectMapper.readValue(oppdatertSøknadResponse.responseBody, OpplaeringspengerSøknadDto::class.java)

        hentMappeForSøknadId(søknadId)
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody().consumeWith { res ->
                val søknad = objectMapper.readValue(res.responseBody, OpplaeringspengerSøknadDto::class.java)
                assertNotNull(søknad)
                assertThat(søknad.metadata).isEqualTo(oppdatertSøknad.metadata)
                assertEquals(norskIdent, søknad.soekerId)
                assertEquals(
                    listOf(
                        PeriodeDto(
                            LocalDate.of(2018, 12, 30),
                            LocalDate.of(2019, 10, 20)
                        )
                    ),
                    søknad.soeknadsperiode
                )
            }
    }


    @Test
    fun `Innsending av søknad returnerer 404 når mappe ikke finnes`(): Unit = runBlocking {
        val norskIdent = "12030050163"
        val søknadId = "d8e2c5a8-b993-4d2d-9cb5-fdb22a653a0c"
        val sendSøknad = lagSendSøknadDto(norskIdent = norskIdent, søknadId = søknadId)

        sendSøknad(sendSøknad)
            .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
    }


    @Test
    fun `Sjekker at mapping funger hele veien`(): Unit = runBlocking {
        val gyldigSøknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOlpFull()
        val visningDto = objectMapper.convertValue<OpplaeringspengerSøknadDto>(gyldigSøknad)

        val mapTilSendingsformat = MapOlpTilK9Format(
            søknadId = visningDto.soeknadId,
            journalpostIder = visningDto.journalposter?.toSet() ?: emptySet(),
            perioderSomFinnesIK9 = emptyList(),
            dto = visningDto
        ).søknadOgFeil()
        assertNotNull(mapTilSendingsformat)

        val tilbake = objectMapper.convertValue<SøknadJson>(visningDto)
        assertEquals(visningDto.soekerId, tilbake.getValue("soekerId").toString())
    }


    @Test
    fun `Prøver å sende søknaden til Kafka når den er gyldig`(): Unit = runBlocking {
        val norskIdent = "02020050121"
        val søknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOlpFull()
        val journalpostId = JournalpostIds.FerdigstiltMedSaksnummer

        val (_, status, body) = opprettOgSendInnSoeknad(
            søknadJson = søknad,
            norskIdent = norskIdent,
            journalpostId = journalpostId
        )
        assertThat(body.feil).isNull()
        assertEquals(HttpStatus.ACCEPTED, status)

        assertThat(journalpostRepository.kanSendeInn(listOf(journalpostId))).isFalse
    }


    @Test
    fun `Skal få 409 når det blir sendt på en journalpost som er sendt fra før, og innsendingen ikke inneholder andre journalposter som kan sendes inn`(): Unit =
        runBlocking {
            val norskIdent = "02020050121"
            val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOlpFull()
            val journalpostId = JournalpostIds.FerdigstiltMedSaksnummer

            val (søknadId, status, body) =
                opprettOgSendInnSoeknad(
                    søknadJson = gyldigSoeknad,
                    norskIdent = norskIdent,
                    journalpostId = journalpostId
                )

            assertThat(body.feil).isNull()
            assertEquals(HttpStatus.ACCEPTED, status)
            assertThat(journalpostRepository.kanSendeInn(listOf(journalpostId))).isFalse

            val sendSøknad = lagSendSøknadDto(norskIdent = norskIdent, søknadId = søknadId)
            sendSøknad(sendSøknad)
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .consumeWith { res ->
                    val oasFeil = objectMapper.readValue(res.responseBody, OasFeil::class.java)
                    assertThat(oasFeil.feil).isEqualTo("Innsendingen må inneholde minst en journalpost som kan sendes inn.")
                }
        }


    @Test
    fun `Skal kunne lagre ned en minimal søknad`(): Unit = runBlocking {
        val norskIdent = "02022352121"
        val minimalSøknad: SøknadJson = LesFraFilUtil.minimalSøknadOlp()
        val journalpostId = JournalpostIds.FerdigstiltMedSaksnummer

        val (_, status, body) =
            opprettOgSendInnSoeknad(
                søknadJson = minimalSøknad,
                norskIdent = norskIdent,
                journalpostId = journalpostId
            )

        assertThat(body.feil).isNull()
        assertEquals(HttpStatus.ACCEPTED, status)
    }


    @Test
    fun `Skal få feil hvis mottattDato ikke er fylt ut`(): Unit = runBlocking {
        val norskIdent = "02022352121"
        val minimalSøknad: SøknadJson = LesFraFilUtil.minimalSøknadOlp()
        settMottattDatoTilNull(minimalSøknad)
        val journalpostId = JournalpostIds.FerdigstiltMedSaksnummer

        val (_, status, body) =
            opprettOgSendInnSoeknad(
                søknadJson = minimalSøknad,
                norskIdent = norskIdent,
                journalpostId = journalpostId
            )

        assertEquals(HttpStatus.BAD_REQUEST, status)
        assertEquals("mottattDato", body.feil?.get(0)?.feilkode)
    }


    @Test
    fun `Skal kunne lagre søknad med tid`(): Unit = runBlocking {
        val norskIdent = "02022352121"
        val søknadMedTid: SøknadJson = LesFraFilUtil.tidSøknadOlp()
        val journalpostId = JournalpostIds.FerdigstiltMedSaksnummer

        val (_, status, body) = opprettOgSendInnSoeknad(
            søknadJson = søknadMedTid,
            norskIdent = norskIdent,
            journalpostId = journalpostId
        )

        assertThat(body.feil).isNull()
        assertEquals(HttpStatus.ACCEPTED, status)
    }

    @Test
    fun `Skal kunne lagre ned ferie fra søknad`(): Unit = runBlocking {
        val norskIdent = "02022352121"
        val søknadJson: SøknadJson = LesFraFilUtil.ferieSøknadOlp()
        val journalpostId = JournalpostIds.FerdigstiltMedSaksnummer

        val oppdatertSøknad = opprettOgLagreSøknad(søknadJson = søknadJson, norskIdent = norskIdent, journalpostId = journalpostId)

        hentMappeForSøknadId(oppdatertSøknad?.soeknadId)
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody()
            .consumeWith{ res ->
                val søknad = objectMapper.readValue(res.responseBody, OpplaeringspengerSøknadDto::class.java)
                assertNotNull(søknad)
                assertEquals(søknad.lovbestemtFerie?.get(0)?.fom!!, LocalDate.of(2021, 4, 14))
            }
    }


    @Test
    fun `Skal kunne lagre ned sn fra søknad`(): Unit = runBlocking {
        val norskIdent = "02022352121"
        val søknadJson: SøknadJson = LesFraFilUtil.snSøknadOlp()
        val journalpostId = JournalpostIds.FerdigstiltMedSaksnummer

        val oppdatertSøknad = opprettOgLagreSøknad(søknadJson = søknadJson, norskIdent = norskIdent, journalpostId = journalpostId)

        val søknadViaGet = hentMappeForSøknadId(oppdatertSøknad?.soeknadId)
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody()
            .returnResult()
            .responseBody
            .let { objectMapper.readValue(it, OpplaeringspengerSøknadDto::class.java) }

        assertNotNull(søknadViaGet)
        assertThat(søknadViaGet.opptjeningAktivitet?.selvstendigNaeringsdrivende?.virksomhetNavn).isEqualTo("FiskerAS")
        assertThat(søknadViaGet.opptjeningAktivitet?.selvstendigNaeringsdrivende?.organisasjonsnummer).isEqualTo("890508087")
        assertThat(søknadViaGet.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.periode?.fom).isEqualTo(
            LocalDate.of(2021, 5, 10)
        )
        assertThat(søknadViaGet.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.landkode).isEqualTo("")
        assertThat(søknadViaGet.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.regnskapsførerNavn).isEqualTo("Regskapsfører")
        assertThat(søknadViaGet.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.regnskapsførerTlf).isEqualTo("88888889")
        assertThat(søknadViaGet.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.registrertIUtlandet).isEqualTo(
            false
        )
        assertThat(søknadViaGet.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.bruttoInntekt).isEqualTo("1200000")
        assertThat(søknadViaGet.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.erNyoppstartet).isEqualTo(false)
        assertThat(søknadViaGet.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.virksomhetstyper).isEqualTo(
            listOf("Fiske", "Jordbruk", "Dagmamma i eget hjem/familiebarnehage", "Annen næringsvirksomhet")
        )
    }


    @Test
    fun `Skal kunne lagre flagg om medisinske og punsjet`(): Unit = runBlocking {
        val norskIdent = "02022352121"
        val søknadJson: SøknadJson = LesFraFilUtil.søknadFraFrontendOlpFull()
        val journalpostId = JournalpostIds.FerdigstiltMedSaksnummer

        val oppdatertSøknad = opprettOgLagreSøknad(søknadJson = søknadJson, norskIdent = norskIdent, journalpostId = journalpostId)

        hentMappeForSøknadId(oppdatertSøknad?.soeknadId)
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody()
            .consumeWith { res ->
                val søknad = objectMapper.readValue(res.responseBody, OpplaeringspengerSøknadDto::class.java)
                assertNotNull(søknad)
                assertThat(søknad.harInfoSomIkkeKanPunsjes).isEqualTo(true)
                assertThat(søknad.harMedisinskeOpplysninger).isEqualTo(false)
            }
    }


    @Test
    fun `Skal verifisere at søknad er ok`(): Unit = runBlocking {
        val norskIdent = "02022352121"
        val søknadJson: SøknadJson = LesFraFilUtil.søknadFraFrontendOlpFull()
        val journalpostId = JournalpostIds.FerdigstiltMedSaksnummer

        opprettOgLagreSøknad(søknadJson = søknadJson, norskIdent = norskIdent, journalpostId = journalpostId)

        webTestClient.post()
            .uri { it.pathSegment(api, søknadTypeUri, "valider").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(søknadJson))
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.ACCEPTED)
    }


//    @Test
//    fun `Skal verifisere at vi utvider men flere journalposter`(): Unit = runBlocking {
//        val norskIdent = "02022352121"
//        val soeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOlpFull()
//        tilpasserSøknadsMalTilTesten(soeknad, norskIdent)
//
//        val søknadId = opprettSoeknad(ident = norskIdent)
//        leggerPåNySøknadId(soeknad, søknadId)
//
//        client.put()
//            .uri { it.pathSegment(api, søknadTypeUri, "oppdater").build() }
//            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
//            .body(BodyInserters.fromValue(soeknad))
//            .awaitExchangeBlocking()
//
//        val med2: SøknadJson = LesFraFilUtil.søknadFraFrontendMed2()
//        tilpasserSøknadsMalTilTesten(med2, norskIdent)
//        leggerPåNySøknadId(med2, søknadId)
//
//        client.put()
//            .uri { it.pathSegment(api, søknadTypeUri, "oppdater").build() }
//            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
//            .body(BodyInserters.fromValue(med2))
//            .awaitExchangeBlocking()
//
//        val id = hentSøknadId(søknadId)
//
//        val søknadViaGet = client.get()
//            .uri { it.pathSegment(api, søknadTypeUri, "mappe", id).build() }
//            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
//            .awaitBodyWithType<OpplaeringspengerSøknadDto>()
//
//        assertThat(søknadViaGet.journalposter).hasSize(2)
//        assertThat(søknadViaGet.journalposter).isEqualTo(listOf("9999", "10000"))
//    }
//
//    @Test
//    @Disabled("TODO")
//    fun `Skal verifisere at alle felter blir lagret`(): Unit = runBlocking {
//        val norskIdent = "12022352121"
//        val soeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOlpFull()
//        tilpasserSøknadsMalTilTesten(soeknad, norskIdent)
//
//        val oppdatertSoeknadDto = opprettOgLagreSøknad(soeknadJson = soeknad, ident = norskIdent)
//
//        val søknadViaGet = client.get()
//            .uri { it.pathSegment(api, søknadTypeUri, "mappe", oppdatertSoeknadDto.soeknadId).build() }
//            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
//            .awaitBodyWithType<OpplaeringspengerSøknadDto>()
//
//        // GUI format
//        assertNotNull(søknadViaGet)
//        assertThat(søknadViaGet.soekerId).isEqualTo(norskIdent)
//        assertThat(søknadViaGet.journalposter!![0]).isEqualTo("99998888")
//        assertThat(søknadViaGet.mottattDato).isEqualTo(LocalDate.of(2020, 10, 12))
//        assertThat(søknadViaGet.barn?.norskIdent).isEqualTo("22222222222")
//        assertThat(søknadViaGet.soeknadsperiode?.first()?.fom).isEqualTo(LocalDate.of(2018, 12, 30))
//        assertThat(søknadViaGet.soeknadsperiode?.first()?.tom).isEqualTo(LocalDate.of(2019, 10, 20))
//        assertThat(søknadViaGet.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.periode?.fom).isEqualTo(
//            LocalDate.of(2018, 12, 30)
//        )
//        assertThat(søknadViaGet.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.periode?.tom).isNull()
//        assertThat(søknadViaGet.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.virksomhetstyper).hasSize(4)
//        assertThat(søknadViaGet.opptjeningAktivitet?.selvstendigNaeringsdrivende?.virksomhetNavn).isEqualTo("FiskerAS")
//        assertThat(søknadViaGet.opptjeningAktivitet?.frilanser?.startdato).isEqualTo("2019-10-10")
//        assertThat(søknadViaGet.opptjeningAktivitet?.arbeidstaker!![0].organisasjonsnummer).isEqualTo("910909088")
//        assertThat(søknadViaGet.arbeidstid?.arbeidstakerList!![0].organisasjonsnummer).isEqualTo("910909088")
//        assertThat(søknadViaGet.arbeidstid?.arbeidstakerList!![0].arbeidstidInfo?.perioder!![0].periode?.fom).isEqualTo(
//            LocalDate.of(2018, 12, 30)
//        )
//        assertThat(søknadViaGet.arbeidstid?.arbeidstakerList!![0].arbeidstidInfo?.perioder!![0].periode?.tom).isEqualTo(
//            LocalDate.of(2019, 10, 20)
//        )
//        assertThat(søknadViaGet.arbeidstid?.arbeidstakerList!![0].arbeidstidInfo?.perioder!![0].faktiskArbeidTimerPerDag).isEqualTo(
//            "7,48"
//        )
//        assertThat(søknadViaGet.arbeidstid?.arbeidstakerList!![0].arbeidstidInfo?.perioder!![0].jobberNormaltTimerPerDag).isEqualTo(
//            "7,48"
//        )
//        assertThat(søknadViaGet.arbeidstid?.frilanserArbeidstidInfo!!.perioder?.first()?.periode?.fom).isEqualTo(
//            LocalDate.of(
//                2018,
//                12,
//                30
//            )
//        )
//        assertThat(søknadViaGet.arbeidstid?.selvstendigNæringsdrivendeArbeidstidInfo!!.perioder?.first()?.jobberNormaltTimerPerDag).isEqualTo(
//            "7"
//        )
//        assertThat(søknadViaGet.uttak?.first()?.timerPleieAvBarnetPerDag).isEqualTo("7,5")
//        assertThat(søknadViaGet.omsorg?.relasjonTilBarnet).isEqualTo("MOR")
//        assertThat(søknadViaGet.bosteder!![0].land).isEqualTo("RU")
//        assertThat(søknadViaGet.lovbestemtFerie!![0].fom).isEqualTo(LocalDate.of(2018, 12, 30))
//        assertThat(søknadViaGet.utenlandsopphold!![0].periode?.fom).isEqualTo(LocalDate.of(2018, 12, 30))
//        assertThat(søknadViaGet.soknadsinfo!!.harMedsoeker).isEqualTo(true)
//        assertThat(søknadViaGet.soknadsinfo!!.samtidigHjemme).isEqualTo(true)
//
//        // k9-format, faktisk søknad format
//        val mapTilEksternFormat = MapOlpTilK9Format(
//            søknadViaGet.soeknadId,
//            søknadViaGet.journalposter!!.toSet(),
//            emptyList(),
//            søknadViaGet
//        )
//
//        assertThat(mapTilEksternFormat.feil()).isEmpty()
//        val søknad = mapTilEksternFormat.søknad()
//
//        assertThat(søknad.søker.personIdent.verdi).isEqualTo(norskIdent)
//        val ytelse = søknad.getYtelse<Opplæringspenger>()
//
//        assertThat(ytelse.barn.personIdent.verdi).isEqualTo("22222222222")
//        assertThat(ytelse.søknadsperiode.iso8601).isEqualTo("2018-12-30/2019-10-20")
//        assertThat(ytelse.opptjeningAktivitet.selvstendigNæringsdrivende?.get(0)?.perioder?.keys?.first()?.iso8601).isEqualTo(
//            "2018-12-30/.."
//        )
//        assertThat(ytelse?.opptjeningAktivitet?.selvstendigNæringsdrivende?.get(0)?.perioder?.values?.first()?.virksomhetstyper).hasSize(
//            4
//        )
//        assertThat(ytelse?.opptjeningAktivitet?.selvstendigNæringsdrivende?.get(0)?.virksomhetNavn).isEqualTo("FiskerAS")
//        assertThat(ytelse.opptjeningAktivitet?.frilanser?.startdato).isEqualTo("2019-10-10")
//        assertThat(ytelse.arbeidstid?.arbeidstakerList!![0].organisasjonsnummer.verdi).isEqualTo("910909088")
//        assertThat(ytelse.arbeidstid?.arbeidstakerList!![0].arbeidstidInfo.perioder?.keys?.first()?.iso8601).isEqualTo("2018-12-30/2019-10-20")
//        assertThat(ytelse.arbeidstid?.arbeidstakerList!![0].arbeidstidInfo.perioder?.values?.first()?.faktiskArbeidTimerPerDag?.toString()).isEqualTo(
//            "PT7H29M"
//        )
//        assertThat(ytelse.arbeidstid?.arbeidstakerList!![0].arbeidstidInfo.perioder?.values?.first()?.jobberNormaltTimerPerDag?.toString()).isEqualTo(
//            "PT7H29M"
//        )
//        assertThat(ytelse.arbeidstid?.selvstendigNæringsdrivendeArbeidstidInfo!!.get().perioder?.values?.first()?.jobberNormaltTimerPerDag).isEqualTo(
//            Duration.ofHours(7)
//        )
//        assertThat(ytelse.arbeidstid?.frilanserArbeidstidInfo!!.get().perioder?.keys?.first()?.iso8601).isEqualTo("2018-12-30/2019-10-20")
//        assertThat(ytelse.uttak?.perioder?.values?.first()?.timerPleieAvBarnetPerDag.toString()).isEqualTo("PT7H30M")
//        assertThat(ytelse.bosteder.perioder.values.first().land.landkode).isEqualTo("RU")
//        assertThat(ytelse.lovbestemtFerie!!.perioder?.get(Periode("2018-12-30/2019-06-20"))?.isSkalHaFerie).isEqualTo(
//            true
//        )
//        assertThat(ytelse.lovbestemtFerie!!.perioder?.get(Periode("2019-06-21/2019-10-20"))?.isSkalHaFerie).isEqualTo(
//            false
//        )
//        assertThat(ytelse.utenlandsopphold!!.perioder.keys.first()?.iso8601).isEqualTo("2018-12-30/2019-01-08")
//        assertThat(ytelse.utenlandsopphold!!.perioder.values.first()?.Årsak).isEqualTo(Utenlandsopphold.UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD)
//        assertThat(ytelse.opptjeningAktivitet.frilanser.startdato).isEqualTo(LocalDate.of(2019, 10, 10))
//        assertThat(ytelse.opptjeningAktivitet.frilanser.sluttdato).isEqualTo(LocalDate.of(2019, 11, 10))
//    }


    private fun opprettOgSendInnSoeknad(
        søknadJson: SøknadJson,
        norskIdent: String,
        journalpostId: String
    ): Triple<String, HttpStatusCode, OasSoknadsfeil> {
        val oppdatertSøknad = opprettOgLagreSøknad(norskIdent, journalpostId, søknadJson)

        val søknadId = oppdatertSøknad?.soeknadId
        assertNotNull(søknadId)
        val sendSøknad = lagSendSøknadDto(norskIdent = norskIdent, søknadId = søknadId!!)

        val journalposter = oppdatertSøknad?.journalposter!!
        val kanSendeInn = journalpostRepository.kanSendeInn(journalposter)
        assertThat(kanSendeInn).isTrue

        // sender en søknad
        val response = sendSøknad(sendSøknad)
            .expectBody()
            .returnResult()

        val httpstatus = response.status
        val oasSoknadsfeil = objectMapper.readValue(response.responseBody, OasSoknadsfeil::class.java)

        return Triple(søknadId, httpstatus, oasSoknadsfeil)
    }

    private fun opprettOgLagreSøknad(
        norskIdent: String,
        journalpostId: String,
        søknadJson: SøknadJson
    ): OpplaeringspengerSøknadDto? {
        val k9Saksnummer = "12345"

        val søknad = OpprettNySøknad(norskIdent, journalpostId, k9Saksnummer, null)
        val locationMedSøknadId = opprettSøknadOgHentSøknadId(søknad)

        tilpassSøknadTilTest(søknadJson, norskIdent, journalpostId, locationMedSøknadId)
        val oppdatertSøknadResponse = oppdaterSøknad(søknadJson)
            .expectStatus().isOk
            .expectBody()
            .returnResult()

        val oppdatertSøknad =
            objectMapper.readValue(oppdatertSøknadResponse.responseBody, OpplaeringspengerSøknadDto::class.java)
        assertNotNull(oppdatertSøknad.soekerId)
        return oppdatertSøknad
    }

    private fun sendSøknad(sendSøknad: SendSøknad) = webTestClient.post()
        .uri { it.pathSegment(api, søknadTypeUri, "send").build() }
        .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
        .body(BodyInserters.fromValue(sendSøknad))
        .exchange()


    private fun opprettSøknadOgHentSøknadId(
        nySøknad: OpprettNySøknad
    ): URI? {
        val response = webTestClient.post()
            .uri { it.pathSegment(api, søknadTypeUri).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(nySøknad))
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CREATED)
            .expectHeader().exists("Location")
            .expectBody()
            .returnResult()

        val location = response.responseHeaders.location

        return location
    }

    private fun oppdaterSøknad(søknadJson: SøknadJson) = webTestClient.put()
        .uri { it.pathSegment(api, søknadTypeUri, "oppdater").build() }
        .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
        .body(BodyInserters.fromValue(søknadJson))
        .exchange()

    private fun hentMappeMedNorskIdent(norskIdent: String) = webTestClient.get()
        .uri { it.pathSegment(api, søknadTypeUri, "mappe").build() }
        .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
        .header("X-Nav-NorskIdent", norskIdent)
        .exchange()

    private fun hentMappeForSøknadId(søknadId: String?) = webTestClient.get()
        .uri { it.pathSegment(api, søknadTypeUri, "mappe", søknadId).build() }
        .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
        .exchange()
}

private fun lagOpprettNySøknadDto(
    personnummer: String,
    journalpostId: String
): OpprettNySøknad {
    return OpprettNySøknad(personnummer, journalpostId, "12345", null)
}

private fun lagSendSøknadDto(
    norskIdent: String,
    søknadId: String
): SendSøknad {
    return SendSøknad(norskIdent, søknadId)
}

private fun settMottattDatoTilNull(søknad: SøknadJson) {
    søknad.replace("mottattDato", null)
}

private fun tilpassSøknadTilTest(
    søknadJson: SøknadJson,
    norskIdent: String,
    journalpostid: String,
    location: URI?
) {
    leggPåNySøkerIdOgJournalpostId(søknadJson, norskIdent, journalpostid)
    leggPåNySøknadId(søknadJson, location)
}

private fun leggPåNySøkerIdOgJournalpostId(
    søknad: SøknadJson,
    norskIdent: String,
    journalpostId: String? = null
) {
    søknad.replace("soekerId", norskIdent)
    if (journalpostId != null) søknad.replace("journalposter", arrayOf(journalpostId))
}

private fun leggPåNySøknadId(søknadFraFrontend: SøknadJson, location: URI?) {
    val path = location?.path
    val søknadId = path?.substring(path.lastIndexOf('/'))
    val trim = søknadId?.trim('/')
    søknadFraFrontend.replace("soeknadId", trim)
}

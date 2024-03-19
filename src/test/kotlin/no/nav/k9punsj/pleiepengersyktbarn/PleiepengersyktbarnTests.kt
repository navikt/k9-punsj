package no.nav.k9punsj.pleiepengersyktbarn

import com.fasterxml.jackson.module.kotlin.convertValue
import kotlinx.coroutines.runBlocking
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.ytelse.psb.v1.Omsorg
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.felles.dto.OpprettNySøknad
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.felles.dto.SendSøknad
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.openapi.OasFeil
import no.nav.k9punsj.openapi.OasSoknadsfeil
import no.nav.k9punsj.util.IdGenerator
import no.nav.k9punsj.util.LesFraFilUtil
import no.nav.k9punsj.util.SøknadJson
import no.nav.k9punsj.util.TestUtils.hentSøknadId
import no.nav.k9punsj.utils.objectMapper
import no.nav.k9punsj.wiremock.JournalpostIds
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.EntityExchangeResult
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import java.net.URI
import java.time.Duration
import java.time.LocalDate

class PleiepengersyktbarnTests : AbstractContainerBaseTest() {

    private val api = "api"
    private val søknadTypeUri = "pleiepenger-sykt-barn-soknad"

    @Autowired
    lateinit var journalpostRepository: JournalpostRepository

    @AfterEach
    fun teardown() {
        cleanUpDB()
    }

    @Test
    fun `Får tom liste når personen ikke har en eksisterende mappe`(): Unit = runBlocking {
        val norskIdent = "01110050053"

        hentMappe(norskIdent)
            .expectStatus().isOk
            .expectBody(SvarPsbDto::class.java)
            .consumeWith {
                assertThat(it.responseBody!!.søknader!!).isEmpty()
            }
    }

    @Test
    fun `Opprette ny mappe på person`(): Unit = runBlocking {
        val norskIdent = "01010050053"
        val opprettNySøknad = opprettSøknad(norskIdent, "999")

        opprettNySøknad(opprettNySøknad).expectStatus().isCreated
    }

    @Test
    fun `Hente eksisterende mappe på person`(): Unit = runBlocking {
        val norskIdent = "02020050163"
        val opprettNySøknad = opprettSøknad(norskIdent, "9999")

        opprettNySøknad(opprettNySøknad).expectStatus().isCreated

        hentMappe(norskIdent)
            .expectStatus().isOk
            .expectBody(SvarPsbDto::class.java)
            .consumeWith {
                val søknader = it.responseBody!!.søknader!!
                assertThat(søknader).hasSize(1)
                assertThat(it.responseBody!!.søknader?.first()?.journalposter?.first()).isEqualTo("9999")
            }
    }

    @Test
    fun `Hent en søknad`(): Unit = runBlocking {
        val søknad = LesFraFilUtil.søknadFraFrontend()
        val norskIdent = "02030050163"
        val journalpostid = "21707da8-a13b-4927-8776-c53399727b29"
        tilpasserSøknadsMalTilTesten(søknad, norskIdent, journalpostid)

        val opprettNySøknad = opprettSøknad(norskIdent, journalpostid)

        val location = opprettNySøknad(opprettNySøknad)
            .expectStatus().isCreated
            .expectHeader().exists("Location")
            .expectBody()
            .returnResult().responseHeaders.location!!

        val søknadViaGet = hentMappeGittSøknadId(hentSøknadId(location)!!)
            .expectStatus().isOk
            .expectBody(PleiepengerSyktBarnSøknadDto::class.java)
            .returnResult().responseBody!!

        assertNotNull(søknadViaGet)
        assertEquals(journalpostid, søknadViaGet.journalposter?.first())
    }

    @Test
    fun `Oppdaterer en søknad`(): Unit = runBlocking {
        val søknadFraFrontend = LesFraFilUtil.søknadFraFrontend()
        val norskIdent = "02030050163"
        val journalpostid = "9999"
        tilpasserSøknadsMalTilTesten(søknadFraFrontend, norskIdent, journalpostid)

        val opprettNySøknad = opprettSøknad(norskIdent, journalpostid)

        val location = opprettNySøknad(opprettNySøknad)
            .expectStatus().isCreated
            .expectHeader().exists("Location")
            .expectBody()
            .returnResult().responseHeaders.location!!

        leggerPåNySøknadId(søknadFraFrontend, location)

        oppdaterSøknad(søknadFraFrontend)
            .expectStatus().isOk
            .expectBody(PleiepengerSyktBarnSøknadDto::class.java)
            .consumeWith {
                val body = it.responseBody!!
                assertEquals(norskIdent, body.soekerId)
                assertEquals(
                    listOf(
                        PeriodeDto(
                            LocalDate.of(2018, 12, 30),
                            LocalDate.of(2019, 10, 20)
                        )
                    ),
                    body.soeknadsperiode
                )
            }
    }

    @Test
    fun `Oppdaterer en søknad med metadata`(): Unit = runBlocking {
        val søknadFraFrontend = LesFraFilUtil.søknadFraFrontend()
        val norskIdent = "02030050163"
        val journalpostid = "9999"
        tilpasserSøknadsMalTilTesten(søknadFraFrontend, norskIdent, journalpostid)

        val opprettNySøknad = opprettSøknad(norskIdent, journalpostid)

        val location = opprettNySøknad(opprettNySøknad)
            .expectStatus().isCreated
            .expectHeader().exists("Location")
            .expectBody()
            .returnResult().responseHeaders.location!!

        leggerPåNySøknadId(søknadFraFrontend, location)

        val oppdatertSoeknadDto = oppdaterSøknad(søknadFraFrontend)
            .expectStatus().isOk
            .expectBody(PleiepengerSyktBarnSøknadDto::class.java)
            .returnResult().responseBody!!

        assertEquals(norskIdent, oppdatertSoeknadDto.soekerId)
        assertEquals(
            listOf(
                PeriodeDto(
                    LocalDate.of(2018, 12, 30),
                    LocalDate.of(2019, 10, 20)
                )
            ),
            oppdatertSoeknadDto.soeknadsperiode
        )

        hentMappeGittSøknadId(hentSøknadId(location)!!)
            .expectStatus().isOk
            .expectBody(PleiepengerSyktBarnSøknadDto::class.java)
            .consumeWith {
                val body = it.responseBody!!
                assertThat(oppdatertSoeknadDto.metadata).isEqualTo(body.metadata)
            }
    }

    @Test
    fun `Innsending av søknad returnerer 400 når mappe ikke finnes`(): Unit = runBlocking {
        val norskIdent = "12030050163"
        val søknadId = "d8e2c5a8-b993-4d2d-9cb5-fdb22a653a0c"

        val sendSøknad = lagSendSøknad(norskIdent = norskIdent, søknadId = søknadId)

        sendInnSøknad(sendSøknad)
            .expectStatus().isBadRequest
            .expectBody(OasSoknadsfeil::class.java)
    }

    @Test
    fun `sjekker at mapping fungre hele veien`(): Unit = runBlocking {
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontend()

        val visningDto = objectMapper().convertValue<PleiepengerSyktBarnSøknadDto>(gyldigSoeknad)
        val mapTilSendingsformat = MapPsbTilK9Format(
            søknadId = visningDto.soeknadId,
            journalpostIder = visningDto.journalposter?.toSet() ?: emptySet(),
            perioderSomFinnesIK9 = emptyList(),
            dto = visningDto
        ).søknadOgFeil()
        assertNotNull(mapTilSendingsformat)

        val tilbake = objectMapper().convertValue<SøknadJson>(visningDto)
        assertEquals(visningDto.soekerId, tilbake["soekerId"].toString())
    }

    @Test
    fun `Prøver å sende søknaden til Kafka når den er gyldig`(): Unit = runBlocking {
        val norskIdent = "02020050121"
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontend()
        tilpasserSøknadsMalTilTesten(gyldigSoeknad, norskIdent)

        opprettOgSendInnSoeknad(
            soeknadJson = gyldigSoeknad,
            ident = norskIdent,
            journalpostid = JournalpostIds.FerdigstiltMedSaksnummer
        ).expectStatus().isAccepted

        assertThat(journalpostRepository.kanSendeInn(listOf(JournalpostIds.FerdigstiltMedSaksnummer))).isFalse
    }

    @Test
    fun `Skal få 409 når det blir sendt på en journalpost som er sendt fra før, og innsendingen ikke inneholder andre journalposter som kan sendes inn`(): Unit =
        runBlocking {
            val norskIdent = "02020050121"
            val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontend()
            tilpasserSøknadsMalTilTesten(gyldigSoeknad, norskIdent)

            val søknad =
                opprettOgSendInnSoeknad(
                    soeknadJson = gyldigSoeknad,
                    ident = norskIdent,
                    journalpostid = JournalpostIds.FerdigstiltMedSaksnummer
                )
                    .expectStatus().isAccepted
                    .expectBody(Søknad::class.java)
                    .returnResult().responseBody!!

            assertThat(journalpostRepository.kanSendeInn(listOf(JournalpostIds.FerdigstiltMedSaksnummer))).isFalse

            val sendSøknad = lagSendSøknad(norskIdent = norskIdent, søknadId = søknad.søknadId.id)

            sendInnSøknad(sendSøknad)
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody(OasFeil::class.java)
                .consumeWith {
                    val body = it.responseBody!!
                    assertEquals("Innsendingen må inneholde minst en journalpost som kan sendes inn.", body.feil)
                }
        }

    @Test
    fun `Skal kunne lagre ned minimal søknad`(): Unit = runBlocking {
        val norskIdent = "02022352121"
        val soeknad: SøknadJson = LesFraFilUtil.minimalSøknad()
        val journalpostId = IdGenerator.nesteId()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent, journalpostId)

        opprettOgSendInnSoeknad(soeknadJson = soeknad, ident = norskIdent, journalpostId)
            .expectStatus().isBadRequest
    }

    @Test
    fun `Skal kunne lagre ned tomt land søknad`(): Unit = runBlocking {
        val norskIdent = "02022352121"
        val soeknad: SøknadJson = LesFraFilUtil.tomtLand()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent)

        opprettOgSendInnSoeknad(
            soeknadJson = soeknad,
            ident = norskIdent,
            journalpostid = JournalpostIds.FerdigstiltMedSaksnummer
        ).expectStatus().isAccepted
    }

    @Test
    fun `Skal kunne lagre med tid søknad`(): Unit = runBlocking {
        val norskIdent = "02022352121"
        val soeknad: SøknadJson = LesFraFilUtil.tidSøknad()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent)

        opprettOgSendInnSoeknad(soeknadJson = soeknad, ident = norskIdent)
            .expectStatus().isBadRequest
            .expectBody(OasSoknadsfeil::class.java)
            .consumeWith {
                val body = it.responseBody!!
                assertThat(body.feil).isNotEmpty
            }
    }

    @Test
    fun `Skal kunne lagre og sette uttak`(): Unit = runBlocking {
        val norskIdent = "02022352121"
        val soeknad: SøknadJson = LesFraFilUtil.utenUttak()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent)

        opprettOgSendInnSoeknad(
            soeknadJson = soeknad,
            ident = norskIdent,
            journalpostid = JournalpostIds.FerdigstiltMedSaksnummer
        ).expectStatus().isAccepted
    }

    @Test
    fun `Skal kunne lagre med ferie null`(): Unit = runBlocking {
        val norskIdent = "02022352121"
        val soeknad: SøknadJson = LesFraFilUtil.ferieNull()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent)

        opprettOgSendInnSoeknad(
            soeknadJson = soeknad,
            ident = norskIdent,
            journalpostid = JournalpostIds.FerdigstiltMedSaksnummer
        ).expectStatus().isAccepted
    }

    @Test
    fun `Skal kunne lagre ned ferie fra søknad`(): Unit = runBlocking {
        val norskIdent = "02022352121"
        val soeknad: SøknadJson = LesFraFilUtil.ferieSøknad()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent)

        val oppdatertSoeknadDto = opprettOgLagreSoeknad(soeknadJson = soeknad, ident = norskIdent)

        hentMappeGittSøknadId(oppdatertSoeknadDto.soeknadId)
            .expectStatus().isOk
            .expectBody(PleiepengerSyktBarnSøknadDto::class.java)
            .consumeWith {
                val body = it.responseBody!!
                assertThat(body.lovbestemtFerie).isNotNull
                assertThat(body.lovbestemtFerie).hasSize(1)
                assertThat(body.lovbestemtFerie!![0].fom).isEqualTo(LocalDate.of(2021, 4, 14))
            }
    }

    @Test
    fun `Skal kunne lagre ned sn fra søknad`(): Unit = runBlocking {
        val norskIdent = "02022352121"
        val soeknad: SøknadJson = LesFraFilUtil.sn()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent)

        val oppdatertSoeknadDto = opprettOgLagreSoeknad(soeknadJson = soeknad, ident = norskIdent)

        hentMappeGittSøknadId(oppdatertSoeknadDto.soeknadId)
            .expectStatus().isOk
            .expectBody(PleiepengerSyktBarnSøknadDto::class.java)
            .consumeWith {
                val søknad = it.responseBody!!
                assertThat(søknad.opptjeningAktivitet).isNotNull
                assertThat(søknad.opptjeningAktivitet?.selvstendigNaeringsdrivende).isNotNull
                assertThat(søknad.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info).isNotNull
                assertThat(søknad.opptjeningAktivitet?.selvstendigNaeringsdrivende?.virksomhetNavn).isEqualTo("FiskerAS")
                assertThat(søknad.opptjeningAktivitet?.selvstendigNaeringsdrivende?.organisasjonsnummer).isEqualTo("890508087")
                assertThat(søknad.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.periode?.fom).isEqualTo(
                    LocalDate.of(2021, 5, 10)
                )
                assertThat(søknad.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.landkode).isEqualTo("")
                assertThat(søknad.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.regnskapsførerNavn).isEqualTo(
                    "Regskapsfører"
                )
                assertThat(søknad.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.regnskapsførerTlf).isEqualTo("88888889")
                assertThat(søknad.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.registrertIUtlandet).isEqualTo(
                    false
                )
                assertThat(søknad.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.bruttoInntekt).isEqualTo("1200000")
                assertThat(søknad.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.erNyoppstartet).isEqualTo(
                    false
                )
                assertThat(søknad.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.virksomhetstyper).isEqualTo(
                    listOf("Fiske", "Jordbruk", "Dagmamma i eget hjem/familiebarnehage", "Annen næringsvirksomhet")
                )
            }
    }

    @Test
    fun `Skal kunne lagre flagg om medisinske og punsjet`(): Unit = runBlocking {
        val norskIdent = "02022352121"
        val soeknad: SøknadJson = LesFraFilUtil.søknadFraFrontend()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent)

        val oppdatertSoeknadDto = opprettOgLagreSoeknad(soeknadJson = soeknad, ident = norskIdent)

        hentMappeGittSøknadId(oppdatertSoeknadDto.soeknadId)
            .expectStatus().isOk
            .expectBody(PleiepengerSyktBarnSøknadDto::class.java)
            .consumeWith {
                val søknad = it.responseBody!!
                assertThat(søknad.harInfoSomIkkeKanPunsjes).isEqualTo(true)
                assertThat(søknad.harMedisinskeOpplysninger).isEqualTo(false)
            }
    }

    @Test
    fun `Skal verifisere at søknad er ok`(): Unit = runBlocking {
        val norskIdent = "02022352121"
        val soeknad: SøknadJson = LesFraFilUtil.søknadFraFrontend()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent)
        opprettOgLagreSoeknad(soeknadJson = soeknad, ident = norskIdent)

        validerSøknad(soeknad).expectStatus().isAccepted
    }

    @Test
    fun `Skal verifisere at vi utvider men flere journalposter`(): Unit = runBlocking {
        val norskIdent = "02022352121"
        val soeknad: SøknadJson = LesFraFilUtil.søknadFraFrontend()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent)

        val søknadId = opprettSoeknad(ident = norskIdent)
        leggerPåNySøknadId(soeknad, søknadId)

        oppdaterSøknad(soeknad).expectStatus().isOk

        val med2: SøknadJson = LesFraFilUtil.søknadFraFrontendMed2()
        tilpasserSøknadsMalTilTesten(med2, norskIdent)
        leggerPåNySøknadId(med2, søknadId)

        oppdaterSøknad(med2).expectStatus().isOk

        hentMappeGittSøknadId(hentSøknadId(søknadId)!!)
            .expectStatus().isOk
            .expectBody(PleiepengerSyktBarnSøknadDto::class.java)
            .consumeWith {
                val body = it.responseBody!!
                assertThat(body.journalposter).hasSize(2)
                assertThat(body.journalposter).isEqualTo(listOf("9999", "10000"))
            }
    }

    @Test
    fun `Skal verifisere at v2 av utelandsopphold blir lagret riktig`(): Unit = runBlocking {
        val norskIdent = "12022352121"
        val soeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendUtenlandsoppholdV2()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent)

        val oppdatertSoeknadDto = opprettOgLagreSoeknad(soeknadJson = soeknad, ident = norskIdent)

        hentMappeGittSøknadId(oppdatertSoeknadDto.soeknadId)
            .expectStatus().isOk
            .expectBody(PleiepengerSyktBarnSøknadDto::class.java)
            .consumeWith { response: EntityExchangeResult<PleiepengerSyktBarnSøknadDto> ->
                val søknad = response.responseBody!!

                // k9-format, faktisk søknad format
                val mapTilEksternFormat = MapPsbTilK9Format(
                    søknad.soeknadId,
                    søknad.journalposter!!.toSet(),
                    emptyList(),
                    søknad
                )

                assertThat(mapTilEksternFormat.feil()).isEmpty()
                val k9Format = mapTilEksternFormat.søknad()
                val ytelse = k9Format.getYtelse<PleiepengerSyktBarn>()
                assertThat(ytelse.utenlandsopphold.perioder.size).isEqualTo(3)
                val filter = ytelse.utenlandsopphold.perioder.values.filter { it.Årsak != null }
                assertThat(filter[0].Årsak).isEqualTo(Utenlandsopphold.UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD)
            }
    }

    @Test
    fun `Skal verifisere at alle felter blir lagret`(): Unit = runBlocking {
        val norskIdent = "12022352121"
        val soeknad: SøknadJson = LesFraFilUtil.søknadFraFrontend()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent)

        val oppdatertSoeknadDto = opprettOgLagreSoeknad(soeknadJson = soeknad, ident = norskIdent)

        hentMappeGittSøknadId(oppdatertSoeknadDto.soeknadId)
            .expectStatus().isOk
            .expectBody(PleiepengerSyktBarnSøknadDto::class.java)
            .consumeWith { response: EntityExchangeResult<PleiepengerSyktBarnSøknadDto> ->
                val søknad = response.responseBody!!
                assertThat(søknad.soekerId).isEqualTo(norskIdent)
                assertThat(søknad.journalposter!![0]).isEqualTo(JournalpostIds.FerdigstiltMedSaksnummer)
                assertThat(søknad.mottattDato).isEqualTo(LocalDate.of(2020, 10, 12))
                assertThat(søknad.barn?.norskIdent).isEqualTo("22222222222")
                assertThat(søknad.soeknadsperiode?.first()?.fom).isEqualTo(LocalDate.of(2018, 12, 30))
                assertThat(søknad.soeknadsperiode?.first()?.tom).isEqualTo(LocalDate.of(2019, 10, 20))
                assertThat(søknad.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.periode?.fom).isEqualTo(
                    LocalDate.of(2018, 12, 30)
                )
                assertThat(søknad.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.periode?.tom).isNull()
                assertThat(søknad.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.virksomhetstyper).hasSize(4)
                assertThat(søknad.opptjeningAktivitet?.selvstendigNaeringsdrivende?.virksomhetNavn).isEqualTo("FiskerAS")
                assertThat(søknad.opptjeningAktivitet?.frilanser?.startdato).isEqualTo("2019-10-10")
                assertThat(søknad.opptjeningAktivitet?.arbeidstaker!![0].organisasjonsnummer).isEqualTo("910909088")
                assertThat(søknad.arbeidstid?.arbeidstakerList!![0].organisasjonsnummer).isEqualTo("910909088")
                assertThat(søknad.arbeidstid?.arbeidstakerList!![0].arbeidstidInfo?.perioder!![0].periode?.fom).isEqualTo(
                    LocalDate.of(2018, 12, 30)
                )
                assertThat(søknad.arbeidstid?.arbeidstakerList!![0].arbeidstidInfo?.perioder!![0].periode?.tom).isEqualTo(
                    LocalDate.of(2019, 10, 20)
                )
                assertThat(søknad.arbeidstid?.arbeidstakerList!![0].arbeidstidInfo?.perioder!![0].faktiskArbeidTimerPerDag).isEqualTo(
                    "7,48"
                )
                assertThat(søknad.arbeidstid?.arbeidstakerList!![0].arbeidstidInfo?.perioder!![0].jobberNormaltTimerPerDag).isEqualTo(
                    "7,48"
                )
                assertThat(søknad.arbeidstid?.frilanserArbeidstidInfo!!.perioder?.first()?.periode?.fom).isEqualTo(
                    LocalDate.of(
                        2018,
                        12,
                        30
                    )
                )
                assertThat(søknad.arbeidstid?.selvstendigNæringsdrivendeArbeidstidInfo!!.perioder?.first()?.jobberNormaltTimerPerDag).isEqualTo(
                    "7"
                )
                assertThat(søknad.beredskap?.first()?.tilleggsinformasjon).isEqualTo("FÅ SLUTT PÅ COVID!!!")
                assertThat(søknad.nattevaak?.first()?.tilleggsinformasjon).isEqualTo("FÅ SLUTT PÅ COVID!!!")
                assertThat(søknad.tilsynsordning?.perioder?.first()?.timer).isEqualTo(7)
                assertThat(søknad.tilsynsordning?.perioder?.first()?.minutter).isEqualTo(30)
                assertThat(søknad.uttak?.first()?.timerPleieAvBarnetPerDag).isEqualTo("7,5")
                assertThat(søknad.omsorg?.relasjonTilBarnet).isEqualTo("MOR")
                assertThat(søknad.bosteder!![0].land).isEqualTo("RU")
                assertThat(søknad.lovbestemtFerie!![0].fom).isEqualTo(LocalDate.of(2018, 12, 30))
                assertThat(søknad.utenlandsopphold!![0].periode?.fom).isEqualTo(LocalDate.of(2018, 12, 30))
                assertThat(søknad.soknadsinfo!!.harMedsoeker).isEqualTo(true)
                assertThat(søknad.soknadsinfo!!.samtidigHjemme).isEqualTo(true)

                // k9-format, faktisk søknad format
                val mapTilEksternFormat = MapPsbTilK9Format(
                    søknad.soeknadId,
                    søknad.journalposter!!.toSet(),
                    emptyList(),
                    søknad
                )

                assertThat(mapTilEksternFormat.feil()).isEmpty()
                val k9Format = mapTilEksternFormat.søknad()

                assertThat(k9Format.søker.personIdent.verdi).isEqualTo(norskIdent)
                val ytelse = k9Format.getYtelse<PleiepengerSyktBarn>()

                assertThat(ytelse.barn.personIdent.verdi).isEqualTo("22222222222")
                assertThat(ytelse.søknadsperiode.iso8601).isEqualTo("2018-12-30/2019-10-20")
                assertThat(ytelse.opptjeningAktivitet.selvstendigNæringsdrivende?.get(0)?.perioder?.keys?.first()?.iso8601).isEqualTo(
                    "2018-12-30/.."
                )
                assertThat(ytelse?.opptjeningAktivitet?.selvstendigNæringsdrivende?.get(0)?.perioder?.values?.first()?.virksomhetstyper).hasSize(
                    4
                )
                assertThat(ytelse?.opptjeningAktivitet?.selvstendigNæringsdrivende?.get(0)?.virksomhetNavn).isEqualTo("FiskerAS")
                assertThat(ytelse.opptjeningAktivitet?.frilanser?.startdato).isEqualTo("2019-10-10")
                assertThat(ytelse.arbeidstid?.arbeidstakerList!![0].organisasjonsnummer.verdi).isEqualTo("910909088")
                assertThat(ytelse.arbeidstid?.arbeidstakerList!![0].arbeidstidInfo.perioder?.keys?.first()?.iso8601).isEqualTo(
                    "2018-12-30/2019-10-20"
                )
                assertThat(ytelse.arbeidstid?.arbeidstakerList!![0].arbeidstidInfo.perioder?.values?.first()?.faktiskArbeidTimerPerDag?.toString()).isEqualTo(
                    "PT7H29M"
                )
                assertThat(ytelse.arbeidstid?.arbeidstakerList!![0].arbeidstidInfo.perioder?.values?.first()?.jobberNormaltTimerPerDag?.toString()).isEqualTo(
                    "PT7H29M"
                )
                assertThat(ytelse.arbeidstid?.selvstendigNæringsdrivendeArbeidstidInfo!!.get().perioder?.values?.first()?.jobberNormaltTimerPerDag).isEqualTo(
                    Duration.ofHours(7)
                )
                assertThat(ytelse.arbeidstid?.frilanserArbeidstidInfo!!.get().perioder?.keys?.first()?.iso8601).isEqualTo(
                    "2018-12-30/2019-10-20"
                )
                assertThat(ytelse.beredskap?.perioder?.values?.first()?.tilleggsinformasjon).isEqualTo("FÅ SLUTT PÅ COVID!!!")
                assertThat(ytelse.nattevåk?.perioder?.values?.first()?.tilleggsinformasjon).isEqualTo("FÅ SLUTT PÅ COVID!!!")
                assertThat(ytelse.tilsynsordning?.perioder?.values?.first()?.etablertTilsynTimerPerDag.toString()).isEqualTo(
                    "PT7H30M"
                )
                assertThat(ytelse.uttak?.perioder?.values?.first()?.timerPleieAvBarnetPerDag.toString()).isEqualTo("PT7H30M")
                assertThat(ytelse.omsorg.relasjonTilBarnet.get()).isEqualTo(Omsorg.BarnRelasjon.MOR)
                assertThat(ytelse.bosteder.perioder.values.first().land.landkode).isEqualTo("RU")
                assertThat(ytelse.lovbestemtFerie!!.perioder?.get(Periode("2018-12-30/2019-06-20"))?.isSkalHaFerie).isEqualTo(
                    true
                )
                assertThat(ytelse.lovbestemtFerie!!.perioder?.get(Periode("2019-06-21/2019-10-20"))?.isSkalHaFerie).isEqualTo(
                    false
                )
                assertThat(ytelse.utenlandsopphold!!.perioder.keys.first()?.iso8601).isEqualTo("2018-12-30/2019-01-08")
                assertThat(ytelse.utenlandsopphold!!.perioder.values.first()?.Årsak).isEqualTo(Utenlandsopphold.UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD)
                assertThat(ytelse.søknadInfo!!.get().samtidigHjemme).isEqualTo(true)
                assertThat(ytelse.søknadInfo!!.get().harMedsøker).isEqualTo(true)
                assertThat(ytelse.opptjeningAktivitet.frilanser.startdato).isEqualTo(LocalDate.of(2019, 10, 10))
                assertThat(ytelse.opptjeningAktivitet.frilanser.sluttdato).isEqualTo(LocalDate.of(2019, 11, 10))
            }
    }

    private suspend fun opprettOgSendInnSoeknad(
        soeknadJson: SøknadJson,
        ident: String,
        journalpostid: String = IdGenerator.nesteId(),
    ): WebTestClient.ResponseSpec {
        val innsendingForOpprettelseAvMappe = opprettSøknad(ident, journalpostid)

        // oppretter en søknad
        val location = opprettNySøknad(innsendingForOpprettelseAvMappe)
            .expectStatus().isCreated
            .expectHeader().exists("Location")
            .expectBody()
            .returnResult().responseHeaders.location!!

        leggerPåNySøknadId(soeknadJson, location)

        // fyller ut en søknad
        val søknadDtoFyltUt = oppdaterSøknad(soeknadJson)
            .expectStatus().isOk
            .expectBody(PleiepengerSyktBarnSøknadDto::class.java)
            .returnResult().responseBody!!

        assertNotNull(søknadDtoFyltUt.soekerId)

        val søknadId = søknadDtoFyltUt.soeknadId
        val sendSøknad = lagSendSøknad(norskIdent = ident, søknadId = søknadId)

        val journalposter = søknadDtoFyltUt.journalposter!!

        val kanSendeInn = journalpostRepository.kanSendeInn(journalposter)
        assertThat(kanSendeInn).isTrue

        // sender en søknad
        return sendInnSøknad(sendSøknad)
    }

    private suspend fun opprettOgLagreSoeknad(
        soeknadJson: SøknadJson,
        ident: String,
        journalpostid: String = IdGenerator.nesteId(),
    ): PleiepengerSyktBarnSøknadDto {
        val innsendingForOpprettelseAvMappe = opprettSøknad(ident, journalpostid)

        // oppretter en søknad
        val location = opprettNySøknad(innsendingForOpprettelseAvMappe)
            .expectStatus().isCreated
            .expectHeader().exists("Location")
            .expectBody()
            .returnResult().responseHeaders.location!!

        leggerPåNySøknadId(soeknadJson, location)

        // fyller ut en søknad
        val søknadDtoFyltUt = oppdaterSøknad(soeknadJson)
            .expectStatus().isOk
            .expectBody(PleiepengerSyktBarnSøknadDto::class.java)
            .returnResult().responseBody!!

        assertNotNull(søknadDtoFyltUt.soekerId)
        return søknadDtoFyltUt
    }

    private suspend fun opprettSoeknad(
        ident: String,
        journalpostid: String = IdGenerator.nesteId(),
    ): URI? {
        val innsendingForOpprettelseAvMappe = opprettSøknad(ident, journalpostid)

        // oppretter en søknad
        return opprettNySøknad(innsendingForOpprettelseAvMappe)
            .expectStatus().isCreated
            .expectHeader().exists("Location")
            .expectBody()
            .returnResult().responseHeaders.location
    }

    private fun hentMappe(norskIdent: String) = webTestClient.get()
        .uri { it.path("/$api/$søknadTypeUri/mappe").build() }
        .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
        .header("X-Nav-NorskIdent", norskIdent)
        .exchange()

    private fun hentMappeGittSøknadId(søknadId: String) = webTestClient.get()
        .uri { it.path("/$api/$søknadTypeUri/mappe/$søknadId").build() }
        .header("Authorization", saksbehandlerAuthorizationHeader)
        .exchange()

    private fun opprettNySøknad(opprettNySøknad: OpprettNySøknad) = webTestClient.post()
        .uri { it.path("/$api/$søknadTypeUri").build() }
        .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
        .bodyValue(opprettNySøknad)
        .exchange()

    private fun oppdaterSøknad(søknadFraFrontend: SøknadJson) = webTestClient.put()
        .uri { it.path("/$api/$søknadTypeUri/oppdater").build() }
        .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
        .bodyValue(søknadFraFrontend)
        .exchange()

    private fun validerSøknad(soeknad: SøknadJson) = webTestClient.post()
        .uri { it.path("/$api/$søknadTypeUri/valider").build() }
        .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
        .bodyValue(soeknad)
        .exchange()

    private fun sendInnSøknad(sendSøknad: SendSøknad) = webTestClient.post()
        .uri { it.path("/$api/$søknadTypeUri/send").build() }
        .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
        .body(BodyInserters.fromValue(sendSøknad))
        .exchange()
}

private fun opprettSøknad(
    personnummer: String,
    journalpostId: String,
): OpprettNySøknad {
    return OpprettNySøknad(personnummer, journalpostId, null, null)
}

private fun lagSendSøknad(
    norskIdent: String,
    søknadId: String,
): SendSøknad {
    return SendSøknad(norskIdent, søknadId)
}

private fun tilpasserSøknadsMalTilTesten(
    søknad: MutableMap<String, Any?>,
    norskIdent: String,
    journalpostId: String? = null,
) {
    søknad.replace("soekerId", norskIdent)
    if (journalpostId != null) søknad.replace("journalposter", arrayOf(journalpostId))
}

private fun leggerPåNySøknadId(søknadFraFrontend: MutableMap<String, Any?>, location: URI?) {
    val path = location?.path
    val søknadId = path?.substring(path.lastIndexOf('/'))
    val trim = søknadId?.trim('/')
    søknadFraFrontend.replace("soeknadId", trim)
}

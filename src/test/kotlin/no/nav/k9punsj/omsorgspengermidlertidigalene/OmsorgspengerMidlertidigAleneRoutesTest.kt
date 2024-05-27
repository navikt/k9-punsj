package no.nav.k9punsj.omsorgspengermidlertidigalene

import kotlinx.coroutines.runBlocking
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.ytelse.omsorgspenger.utvidetrett.v1.OmsorgspengerMidlertidigAlene
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.felles.dto.SendSøknad
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.util.IdGenerator
import no.nav.k9punsj.util.LesFraFilUtil
import no.nav.k9punsj.util.SøknadJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import java.net.URI
import java.util.*
import kotlin.math.abs
import kotlin.random.Random


internal class OmsorgspengerMidlertidigAleneRoutesTest : AbstractContainerBaseTest() {
    private val api = "api"
    private val søknadTypeUri = "omsorgspenger-midlertidig-alene-soknad"

    @Autowired
    lateinit var journalpostRepository: JournalpostRepository

    @AfterEach
    internal fun tearDown() {
        cleanUpDB()
    }

    @Test
    fun `Får tom liste når personen ikke har en eksisterende mappe`(): Unit = runBlocking {
        val norskIdent = "01110050053"

        hentMappe(norskIdent)
            .expectStatus().isOk
            .expectBody(SvarOmsMADto::class.java)
            .consumeWith { Assertions.assertTrue(it.responseBody!!.søknader!!.isEmpty()) }
    }

    @Test
    fun `Opprette ny mappe på person`(): Unit = runBlocking {
        val norskIdent = "01010050053"
        val pleietrengende = "01010050023"
        val opprettNySøknad = opprettSøknad(
            personnummer = norskIdent,
            journalpostId = UUID.randomUUID().toString(),
            annenPart = null,
            barn = listOf(
                OmsorgspengerMidlertidigAleneSøknadDto.BarnDto(
                    norskIdent = pleietrengende,
                    foedselsdato = null
                )
            )
        )

        opprettNySøknad(opprettNySøknad)
            .expectStatus().isCreated
            .expectBody(OmsorgspengerMidlertidigAleneSøknadDto::class.java)
            .consumeWith {
                assertThat(it.responseBody!!.barn).size().isOne
                assertThat(it.responseBody!!.barn.first().norskIdent).isEqualTo(pleietrengende)
            }
    }

    @Test
    fun `Skal verifisere at søknad er ok`(): Unit = runBlocking {
        val norskIdent = "02022352122"
        val pleietrengende = "01010050023"
        val soeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsMA()
        val journalpostid = abs(Random(234234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent, journalpostid)
        opprettOgLagreSoeknad(
            soeknadJson = soeknad,
            ident = norskIdent,
            journalpostid = journalpostid,
            barn = listOf(
                OmsorgspengerMidlertidigAleneSøknadDto.BarnDto(
                    norskIdent = pleietrengende,
                    foedselsdato = null
                )
            )
        )

        validerSøknad(soeknad).expectStatus().isAccepted
    }

    @Test
    fun `Prøver å sende søknaden til Kafka når den er gyldig`(): Unit = runBlocking {
        val norskIdent = "02020050123"
        val pleietrengende = "01010050023"
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsMA()
        val journalpostid = abs(Random(56234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(gyldigSoeknad, norskIdent, journalpostid)

        val body: Søknad =
            opprettOgSendInnSoeknad(soeknadJson = gyldigSoeknad, ident = norskIdent, journalpostid, pleietrengende)
        val ytelse = body.getYtelse<OmsorgspengerMidlertidigAlene>()
        assertThat(ytelse.barn).size().isEqualTo(2)
        assertThat(ytelse.annenForelder).isNotNull
        assertThat(journalpostRepository.kanSendeInn(listOf(journalpostid))).isFalse
    }

    @Test
    fun `Skal sjekke mapping av felter`(): Unit = runBlocking {
        val norskIdent = "02020050123"
        val pleietrengende = "01010050023"
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsMA()
        val journalpostid = abs(Random(56234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(gyldigSoeknad, norskIdent, journalpostid)

        val søknad = opprettOgLagreSoeknad(
            soeknadJson = gyldigSoeknad,
            ident = norskIdent,
            journalpostid = journalpostid,
            barn = listOf(
                OmsorgspengerMidlertidigAleneSøknadDto.BarnDto(
                    norskIdent = pleietrengende,
                    foedselsdato = null
                )
            )
        )

        hentMappeGittSøknadId(søknad.soeknadId)
            .expectStatus().isOk
            .expectBody(OmsorgspengerMidlertidigAleneSøknadDto::class.java)
            .consumeWith {
                assertThat(it.responseBody!!.annenForelder?.norskIdent).isEqualTo("44444444444")
                assertThat(it.responseBody!!.barn.size).isEqualTo(2)
            }
    }

    @Test
    fun `Oppdatere en søknad med metadata`(): Unit = runBlocking {
        val norskIdent = "02022352122"
        val pleietrengende = "01010050023"
        val soeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsMA()
        val journalpostid = abs(Random(234234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent, journalpostid)
        opprettOgLagreSoeknad(
            soeknadJson = soeknad,
            ident = norskIdent,
            journalpostid = journalpostid,
            barn = listOf(
                OmsorgspengerMidlertidigAleneSøknadDto.BarnDto(
                    norskIdent = pleietrengende,
                    foedselsdato = null
                )
            )
        )

        val body = oppdaterSøknad(soeknad)
            .expectStatus().isOk
            .expectBody(OmsorgspengerMidlertidigAleneSøknadDto::class.java)
            .returnResult().responseBody!!

        assertThat(body.soekerId).isEqualTo(norskIdent)

        val søknadViaGet = hentMappeGittSøknadId(soeknad["soeknadId"] as String)
            .expectStatus().isOk
            .expectBody(OmsorgspengerMidlertidigAleneSøknadDto::class.java)
            .returnResult().responseBody!!

        assertThat(body.metadata).isEqualTo(søknadViaGet.metadata)
    }

    private fun opprettSøknad(
        personnummer: String,
        journalpostId: String,
        annenPart: String? = null,
        barn: List<OmsorgspengerMidlertidigAleneSøknadDto.BarnDto>,
    ): NyOmsMASøknad {
        return NyOmsMASøknad(
            norskIdent = personnummer,
            journalpostId = journalpostId,
            annenPart = annenPart,
            barn = barn,
            k9saksnummer = null
        )
    }

    private fun tilpasserSøknadsMalTilTesten(
        søknad: MutableMap<String, Any?>,
        norskIdent: String,
        journalpostId: String? = null,
    ) {
        søknad.replace("soekerId", norskIdent)
        if (journalpostId != null) søknad.replace("journalposter", arrayOf(journalpostId))
    }

    private suspend fun opprettOgLagreSoeknad(
        soeknadJson: SøknadJson,
        ident: String,
        journalpostid: String = IdGenerator.nesteId(),
        barn: List<OmsorgspengerMidlertidigAleneSøknadDto.BarnDto>,
    ): OmsorgspengerMidlertidigAleneSøknadDto {
        val innsendingForOpprettelseAvMappe = NyOmsMASøknad(
            norskIdent = ident,
            journalpostId = journalpostid,
            barn = barn,
            k9saksnummer = null
        )

        // oppretter en søknad
        val location = opprettNySøknad(innsendingForOpprettelseAvMappe)
            .expectStatus().isCreated
            .expectHeader().exists("Location")
            .expectBody(OmsorgspengerMidlertidigAleneSøknadDto::class.java)
            .returnResult().responseHeaders.location

        leggerPåNySøknadId(soeknadJson, location)

        // fyller ut en søknad
        val søknadDtoFyltUt = oppdaterSøknad(soeknadJson)
            .expectStatus().isOk
            .expectBody(OmsorgspengerMidlertidigAleneSøknadDto::class.java)
            .returnResult().responseBody!!

        Assertions.assertNotNull(søknadDtoFyltUt.soekerId)
        return søknadDtoFyltUt
    }

    private fun leggerPåNySøknadId(søknadFraFrontend: MutableMap<String, Any?>, location: URI?) {
        val path = location?.path
        val søknadId = path?.substring(path.lastIndexOf('/'))
        val trim = søknadId?.trim('/')
        søknadFraFrontend.replace("soeknadId", trim)
    }

    private suspend fun opprettOgSendInnSoeknad(
        soeknadJson: SøknadJson,
        ident: String,
        journalpostid: String = IdGenerator.nesteId(),
        pleietrengende: String,
    ): Søknad {
        val innsendingForOpprettelseAvMappe = opprettSøknad(
            personnummer = ident,
            journalpostId = journalpostid,
            barn = listOf(
                OmsorgspengerMidlertidigAleneSøknadDto.BarnDto(
                    norskIdent = pleietrengende,
                    foedselsdato = null
                )
            )
        )

        // oppretter en søknad
        val location = opprettNySøknad(innsendingForOpprettelseAvMappe)
            .expectStatus().isCreated
            .expectHeader().exists("Location")
            .expectBody(OmsorgspengerMidlertidigAleneSøknadDto::class.java)
            .returnResult().responseHeaders.location

        leggerPåNySøknadId(soeknadJson, location)

        // fyller ut en søknad
        val søknadDtoFyltUt: OmsorgspengerMidlertidigAleneSøknadDto = oppdaterSøknad(soeknadJson)
            .expectStatus().isOk
            .expectBody(OmsorgspengerMidlertidigAleneSøknadDto::class.java)
            .returnResult().responseBody!!

        Assertions.assertNotNull(søknadDtoFyltUt.soekerId)

        val søknadId = søknadDtoFyltUt.soeknadId
        val sendSøknad = lagSendSøknad(norskIdent = ident, søknadId = søknadId)
        val journalposter = søknadDtoFyltUt.journalposter!!

        val kanSendeInn = journalpostRepository.kanSendeInn(journalposter)
        assertThat(kanSendeInn).isTrue

        // sender en søknad
        val søknad = sendSøknad(sendSøknad)
            .expectStatus().isAccepted
            .expectBody(Søknad::class.java)
            .returnResult().responseBody!!

        return søknad
    }

    private fun lagSendSøknad(
        norskIdent: String,
        søknadId: String,
    ): SendSøknad {
        return SendSøknad(norskIdent, søknadId)
    }

    private fun hentMappe(norskIdent: String) = webTestClient.get()
        .uri { it.path("/$api/$søknadTypeUri/mappe").build() }
        .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
        .header("X-Nav-NorskIdent", norskIdent)
        .exchange()

    private fun opprettNySøknad(opprettNySøknad: NyOmsMASøknad) =
        webTestClient.post()
            .uri { it.path("/$api/$søknadTypeUri").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .bodyValue(opprettNySøknad)
            .exchange()

    private fun validerSøknad(soeknad: SøknadJson) = webTestClient.post()
        .uri { it.path("/$api/$søknadTypeUri/valider").build() }
        .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
        .bodyValue(soeknad)
        .exchange()

    private fun hentMappeGittSøknadId(søknadId: String) =
        webTestClient.get()
            .uri { it.path("/$api/$søknadTypeUri/mappe/${søknadId}").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .exchange()

    private fun oppdaterSøknad(soeknad: SøknadJson) = webTestClient.put()
        .uri { it.path("/$api/$søknadTypeUri/oppdater").build() }
        .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
        .bodyValue(soeknad)
        .exchange()

    private fun sendSøknad(sendSøknad: SendSøknad) = webTestClient.post()
        .uri { it.path("/$api/$søknadTypeUri/send").build() }
        .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
        .bodyValue(sendSøknad)
        .exchange()
}

package no.nav.k9punsj.domenetjenester

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.felles.dto.OpprettNySøknad
import no.nav.k9punsj.felles.dto.SendSøknad
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.omsorgspengerutbetaling.OmsorgspengerutbetalingSøknadDto
import no.nav.k9punsj.util.IdGenerator
import no.nav.k9punsj.util.LesFraFilUtil
import no.nav.k9punsj.util.SøknadJson
import no.nav.k9punsj.wiremock.JournalpostIds.FerdigstiltMedSaksnummer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.net.URI

internal class SoknadIntegrasjonsTest : AbstractContainerBaseTest() {

    @Autowired
    lateinit var journalpostRepository: JournalpostRepository

    private val api = "api"
    private val søknadTypeUri = "omsorgspengerutbetaling-soknad"

    @Test
    fun `Sender in søknad mottatt 2023 til k9 for OMS 2022 og får riktig saksnummer`(): Unit = runBlocking {
        val norskIdent = "03011939596"
        val soeknadJson: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsUt2022()
        val journalpostid = FerdigstiltMedSaksnummer
        tilpasserSøknadsMalTilTesten(soeknadJson, norskIdent, journalpostid)
        val soeknad = opprettOgLagreSoeknad(soeknadJson = soeknadJson, ident = norskIdent, journalpostid)

        validerSøknad(soeknadJson)

        val søknadId = soeknad.soeknadId
        val sendSøknad = SendSøknad(norskIdent = norskIdent, soeknadId = søknadId)

        val journalposter = soeknad.journalposter!!

        val kanSendeInn = journalpostRepository.kanSendeInn(journalposter)
        assertThat(kanSendeInn).isTrue

        // sender en søknad
        sendInnSøknad(sendSøknad)
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
    ): OmsorgspengerutbetalingSøknadDto {
        val innsendingForOpprettelseAvMappe = OpprettNySøknad(
            norskIdent = ident,
            journalpostId = journalpostid,
            k9saksnummer = "ABC123"
        )

        // oppretter en søknad
        val location = opprettNySøknad(innsendingForOpprettelseAvMappe)

        leggerPåNySøknadId(soeknadJson, location)

        // fyller ut en søknad
        val søknadDtoFyltUt = oppdaterSøknad(soeknadJson)
            .expectStatus().isOk
            .expectBody(OmsorgspengerutbetalingSøknadDto::class.java)
            .returnResult().responseBody!!

        Assertions.assertNotNull(søknadDtoFyltUt.soekerId)

        return søknadDtoFyltUt
    }

    private fun opprettNySøknad(innsendingForOpprettelseAvMappe: OpprettNySøknad): URI = webTestClient.post()
        .uri { it.path("/$api/$søknadTypeUri").build() }
        .header("Authorization", saksbehandlerAuthorizationHeader)
        .bodyValue(innsendingForOpprettelseAvMappe)
        .exchange()
        .expectStatus().isCreated
        .expectHeader().exists("Location")
        .expectBody()
        .returnResult().responseHeaders.location!!

    private fun validerSøknad(soeknadJson: SøknadJson) {
        webTestClient.post()
            .uri { it.path("/$api/$søknadTypeUri/valider").build() }
            .header("Authorization", saksbehandlerAuthorizationHeader)
            .bodyValue(soeknadJson)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.ACCEPTED)
    }

    private fun sendInnSøknad(sendSøknad: SendSøknad) {
        webTestClient.post()
            .uri { it.path("/$api/$søknadTypeUri/send").build() }
            .header("Authorization", saksbehandlerAuthorizationHeader)
            .bodyValue(sendSøknad)
            .exchange()
            .expectStatus().isAccepted
    }

    private fun oppdaterSøknad(soeknadJson: SøknadJson) = webTestClient.put()
        .uri { it.path("/$api/$søknadTypeUri/oppdater").build() }
        .header("Authorization", saksbehandlerAuthorizationHeader)
        .bodyValue(soeknadJson)
        .exchange()

    private fun leggerPåNySøknadId(søknadFraFrontend: MutableMap<String, Any?>, location: URI?) {
        val path = location?.path
        val søknadId = path?.substring(path.lastIndexOf('/'))
        val trim = søknadId?.trim('/')
        søknadFraFrontend.replace("soeknadId", trim)
    }
}

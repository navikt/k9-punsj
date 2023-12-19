package no.nav.k9punsj.domenetjenester

import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9punsj.TestSetup
import no.nav.k9punsj.felles.IdentOgJournalpost
import no.nav.k9punsj.felles.dto.SendSøknad
import no.nav.k9punsj.omsorgspengerutbetaling.OmsorgspengerutbetalingSøknadDto
import no.nav.k9punsj.openapi.OasSoknadsfeil
import no.nav.k9punsj.util.DbContainerInitializer
import no.nav.k9punsj.util.IdGenerator
import no.nav.k9punsj.util.LesFraFilUtil
import no.nav.k9punsj.util.SøknadJson
import no.nav.k9punsj.util.WebClientUtils.postAndAssert
import no.nav.k9punsj.util.WebClientUtils.postAndAssertAwaitWithStatusAndBody
import no.nav.k9punsj.util.WebClientUtils.putAndAssert
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.BodyInserters
import java.net.URI
import kotlin.math.abs
import kotlin.random.Random
/*
@ExtendWith(SpringExtension::class, MockKExtension::class)
internal class SoknadIntegrasjonsTest {

    private val client = TestSetup.client
    private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"
    private val journalpostRepository = DbContainerInitializer.getJournalpostRepo()

    private val api = "api"
    private val søknadTypeUri = "omsorgspengerutbetaling-soknad"

    @Test
    fun `Sender in søknad mottatt 2023 til k9 for OMS 2022 og får riktig saksnummer`(): Unit = runBlocking {
        val norskIdent = "03011939596"
        val soeknadJson: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsUt2022()
        val journalpostid = abs(Random(234234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(soeknadJson, norskIdent, journalpostid)
        val soeknad = opprettOgLagreSoeknad(soeknadJson = soeknadJson, ident = norskIdent, journalpostid)

        client.postAndAssertAwaitWithStatusAndBody<SøknadJson, OasSoknadsfeil>(
            authorizationHeader = saksbehandlerAuthorizationHeader,
            navNorskIdentHeader = null,
            assertStatus = HttpStatus.ACCEPTED,
            requestBody = BodyInserters.fromValue(soeknadJson),
            api,
            søknadTypeUri,
            "valider"
        )

        val søknadId = soeknad.soeknadId
        val sendSøknad = SendSøknad(norskIdent = norskIdent, soeknadId = søknadId)

        val journalposter = soeknad.journalposter!!

        val kanSendeInn = journalpostRepository.kanSendeInn(journalposter)
        assertThat(kanSendeInn).isTrue

        // sender en søknad
        val body = client.postAndAssertAwaitWithStatusAndBody<SendSøknad, OasSoknadsfeil>(
            authorizationHeader = saksbehandlerAuthorizationHeader,
            navNorskIdentHeader = null,
            assertStatus = HttpStatus.ACCEPTED,
            requestBody = BodyInserters.fromValue(sendSøknad),
            api,
            søknadTypeUri,
            "send"
        )

        Assertions.assertTrue(body.feil.isNullOrEmpty())
    }

    private fun tilpasserSøknadsMalTilTesten(
        søknad: MutableMap<String, Any?>,
        norskIdent: String,
        journalpostId: String? = null
    ) {
        søknad.replace("soekerId", norskIdent)
        if (journalpostId != null) søknad.replace("journalposter", arrayOf(journalpostId))
    }

    private suspend fun opprettOgLagreSoeknad(
        soeknadJson: SøknadJson,
        ident: String,
        journalpostid: String = IdGenerator.nesteId()
    ): OmsorgspengerutbetalingSøknadDto {
        val innsendingForOpprettelseAvMappe = IdentOgJournalpost(ident, journalpostid)

        // oppretter en søknad
        val resPost = client.postAndAssert(
            authorizationHeader = saksbehandlerAuthorizationHeader,
            assertStatus = HttpStatus.CREATED,
            requestBody = BodyInserters.fromValue(innsendingForOpprettelseAvMappe),
            api,
            søknadTypeUri
        )

        val location = resPost.headers().asHttpHeaders().location
        Assertions.assertNotNull(location)

        leggerPåNySøknadId(soeknadJson, location)

        // fyller ut en søknad
        val søknadDtoFyltUt = client.putAndAssert<SøknadJson, OmsorgspengerutbetalingSøknadDto>(
            norskIdent = null,
            authorizationHeader = saksbehandlerAuthorizationHeader,
            assertStatus = HttpStatus.OK,
            requestBody = BodyInserters.fromValue(soeknadJson),
            api,
            søknadTypeUri,
            "oppdater"
        )

        Assertions.assertNotNull(søknadDtoFyltUt.soekerId)
        return søknadDtoFyltUt
    }

    private fun leggerPåNySøknadId(søknadFraFrontend: MutableMap<String, Any?>, location: URI?) {
        val path = location?.path
        val søknadId = path?.substring(path.lastIndexOf('/'))
        val trim = søknadId?.trim('/')
        søknadFraFrontend.replace("soeknadId", trim)
    }
}
*/
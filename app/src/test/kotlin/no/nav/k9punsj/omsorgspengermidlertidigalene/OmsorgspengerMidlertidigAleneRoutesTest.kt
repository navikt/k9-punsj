package no.nav.k9punsj.omsorgspengermidlertidigalene

import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.ytelse.omsorgspenger.utvidetrett.v1.OmsorgspengerMidlertidigAlene
import no.nav.k9punsj.TestSetup
import no.nav.k9punsj.felles.dto.SendSøknad
import no.nav.k9punsj.openapi.OasSoknadsfeil
import no.nav.k9punsj.util.DatabaseUtil
import no.nav.k9punsj.util.IdGenerator
import no.nav.k9punsj.util.LesFraFilUtil
import no.nav.k9punsj.util.SøknadJson
import no.nav.k9punsj.util.WebClientUtils.awaitBodyWithType
import no.nav.k9punsj.util.WebClientUtils.getAndAssert
import no.nav.k9punsj.util.WebClientUtils.postAndAssert
import no.nav.k9punsj.util.WebClientUtils.postAndAssertAwaitWithStatusAndBody
import no.nav.k9punsj.util.WebClientUtils.putAndAssert
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.BodyInserters
import java.net.URI
import java.util.*
import kotlin.math.abs
import kotlin.random.Random

@ExtendWith(SpringExtension::class, MockKExtension::class)
internal class OmsorgspengerMidlertidigAleneRoutesTest {

    private val client = TestSetup.client
    private val api = "api"
    private val søknadTypeUri = "omsorgspenger-midlertidig-alene-soknad"
    private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"
    private val journalpostRepository = DatabaseUtil.getJournalpostRepo()

    @AfterEach
    internal fun tearDown() {
        DatabaseUtil.cleanDB()
    }

    @Test
    fun `Får tom liste når personen ikke har en eksisterende mappe`(): Unit = runBlocking {
        val norskIdent = "01110050053"
        val body = client.getAndAssert<SvarOmsMADto>(
            norskIdent = norskIdent,
            authorizationHeader = saksbehandlerAuthorizationHeader,
            assertStatus = HttpStatus.OK,
            api,
            søknadTypeUri,
            "mappe"
        )
        Assertions.assertTrue(body.søknader!!.isEmpty())
    }

    @Test
    fun `Opprette ny mappe på person`(): Unit = runBlocking {
        val norskIdent = "01010050053"
        val pleietrengende = "01010050023"
        val opprettNySøknad = opprettSøknad(
            personnummer = norskIdent,
            journalpostId = UUID.randomUUID().toString(),
            annenPart = null,
            barn = listOf(OmsorgspengerMidlertidigAleneSøknadDto.BarnDto(norskIdent = pleietrengende, foedselsdato = null))
        )

        val response: OmsorgspengerMidlertidigAleneSøknadDto = client.postAndAssertAwaitWithStatusAndBody<NyOmsMASøknad, OmsorgspengerMidlertidigAleneSøknadDto>(
            authorizationHeader = saksbehandlerAuthorizationHeader,
            assertStatus = HttpStatus.CREATED,
            requestBody = BodyInserters.fromValue(opprettNySøknad),
            api,
            søknadTypeUri
        )

        assertThat(response.barn).size().isOne
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
            barn = listOf(OmsorgspengerMidlertidigAleneSøknadDto.BarnDto(norskIdent = pleietrengende, foedselsdato = null))
        )

        val body = client.postAndAssertAwaitWithStatusAndBody<SøknadJson, OasSoknadsfeil>(
            authorizationHeader = saksbehandlerAuthorizationHeader,
            assertStatus = HttpStatus.ACCEPTED,
            requestBody = BodyInserters.fromValue(soeknad),
            api,
            søknadTypeUri,
            "valider"
        )
        assertThat(body.feil).isNull()
    }

    @Test
    fun `Prøver å sende søknaden til Kafka når den er gyldig`(): Unit = runBlocking {
        val norskIdent = "02020050123"
        val pleietrengende = "01010050023"
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsMA()
        val journalpostid = abs(Random(56234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(gyldigSoeknad, norskIdent, journalpostid)

        val body: Søknad = opprettOgSendInnSoeknad(soeknadJson = gyldigSoeknad, ident = norskIdent, journalpostid, pleietrengende)
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
            barn = listOf(OmsorgspengerMidlertidigAleneSøknadDto.BarnDto(norskIdent = pleietrengende, foedselsdato = null))
        )

        val søknadViaGet = client.get()
            .uri { it.pathSegment(api, søknadTypeUri, "mappe", søknad.soeknadId).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .awaitBodyWithType<OmsorgspengerMidlertidigAleneSøknadDto>()

        assertThat(søknadViaGet.annenForelder?.norskIdent).isEqualTo("44444444444")
        assertThat(søknadViaGet.barn.size).isEqualTo(2)
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

        val body = client.putAndAssert<MutableMap<String, Any?>, OmsorgspengerMidlertidigAleneSøknadDto>(
            norskIdent = null,
            authorizationHeader = saksbehandlerAuthorizationHeader,
            assertStatus = HttpStatus.OK,
            requestBody = BodyInserters.fromValue(soeknad),
            api,
            søknadTypeUri,
            "oppdater"
        )

        Assertions.assertNotNull(body)
        Assertions.assertEquals(norskIdent, body.soekerId)

        val søknadViaGet = client.getAndAssert<OmsorgspengerMidlertidigAleneSøknadDto>(
            norskIdent = norskIdent,
            authorizationHeader = saksbehandlerAuthorizationHeader,
            assertStatus = HttpStatus.OK,
            api,
            søknadTypeUri,
            "mappe",
            soeknad["soeknadId"] as String
        )

        Assertions.assertNotNull(søknadViaGet)
        assertThat(body.metadata).isEqualTo(søknadViaGet.metadata)
    }

    private fun opprettSøknad(
        personnummer: String,
        journalpostId: String,
        annenPart: String? = null,
        barn: List<OmsorgspengerMidlertidigAleneSøknadDto.BarnDto>
    ): NyOmsMASøknad {
        return NyOmsMASøknad(
            norskIdent = personnummer,
            journalpostId = journalpostId,
            annenPart = annenPart,
            barn = barn
        )
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
        journalpostid: String = IdGenerator.nesteId(),
        barn: List<OmsorgspengerMidlertidigAleneSøknadDto.BarnDto>
    ): OmsorgspengerMidlertidigAleneSøknadDto {
        val innsendingForOpprettelseAvMappe = NyOmsMASøknad(
            norskIdent = ident,
            journalpostId = journalpostid,
            barn = barn
        )

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
        val søknadDtoFyltUt = client.putAndAssert<SøknadJson, OmsorgspengerMidlertidigAleneSøknadDto>(
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

    private suspend fun opprettOgSendInnSoeknad(
        soeknadJson: SøknadJson,
        ident: String,
        journalpostid: String = IdGenerator.nesteId(),
        pleietrengende: String
    ): Søknad {
        val innsendingForOpprettelseAvMappe = opprettSøknad(
            personnummer = ident,
            journalpostId = journalpostid,
            barn = listOf(OmsorgspengerMidlertidigAleneSøknadDto.BarnDto(norskIdent = pleietrengende, foedselsdato = null))
        )

        // oppretter en søknad
        val response = client.postAndAssert(
            authorizationHeader = saksbehandlerAuthorizationHeader,
            assertStatus = HttpStatus.CREATED,
            requestBody = BodyInserters.fromValue(innsendingForOpprettelseAvMappe),
            api,
            søknadTypeUri
        )

        val location = response.headers().asHttpHeaders().location
        Assertions.assertEquals(HttpStatus.CREATED, response.statusCode())
        Assertions.assertNotNull(location)

        leggerPåNySøknadId(soeknadJson, location)

        // fyller ut en søknad
        val søknadDtoFyltUt: OmsorgspengerMidlertidigAleneSøknadDto = client.putAndAssert(
            norskIdent = null,
            authorizationHeader = saksbehandlerAuthorizationHeader,
            assertStatus = HttpStatus.OK,
            requestBody = BodyInserters.fromValue(soeknadJson),
            api,
            søknadTypeUri,
            "oppdater"
        )

        Assertions.assertNotNull(søknadDtoFyltUt.soekerId)

        val søknadId = søknadDtoFyltUt.soeknadId
        val sendSøknad = lagSendSøknad(norskIdent = ident, søknadId = søknadId)

        val journalposter = søknadDtoFyltUt.journalposter!!

        val kanSendeInn = journalpostRepository.kanSendeInn(journalposter)
        org.assertj.core.api.Assertions.assertThat(kanSendeInn).isTrue

        // sender en søknad
        val body = client.postAndAssertAwaitWithStatusAndBody<SendSøknad, Søknad>(
            authorizationHeader = saksbehandlerAuthorizationHeader,
            assertStatus = HttpStatus.ACCEPTED,
            requestBody = BodyInserters.fromValue(sendSøknad),
            api,
            søknadTypeUri,
            "send"
        )

        return body
    }

    private fun lagSendSøknad(
        norskIdent: String,
        søknadId: String
    ): SendSøknad {
        return SendSøknad(norskIdent, søknadId)
    }
}

package no.nav.k9punsj.integrasjoner.infotrygd

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.JsonUtil.arrayOrEmptyArray
import no.nav.k9punsj.felles.JsonUtil.objectOrEmptyObject
import no.nav.k9punsj.felles.JsonUtil.stringOrNull
import no.nav.k9punsj.hentCallId
import no.nav.k9punsj.integrasjoner.infotrygd.InfotrygdClient.Companion.Behandlingstema.Companion.relevanteBehandlingstemaer
import no.nav.k9punsj.ruting.RutingGrunnlag
import no.nav.k9punsj.tilgangskontroll.helsesjekk
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.ReactiveHealthIndicator
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.net.URI
import java.time.LocalDate
import kotlin.coroutines.coroutineContext

@Configuration
internal class InfotrygdClient(
    @Value("\${no.nav.infotrygd.base_url}") baseUrl: URI,
    @Value("\${no.nav.infotrygd.scope}") private val infotrygdScopes: Set<String>,
    @Qualifier("azure") private val accessTokenClient: AccessTokenClient
) : ReactiveHealthIndicator {

    private val hentSakerUrl = URI("$baseUrl/saker")
    private val hentVedtakForPleietrengende = URI("$baseUrl/vedtakForPleietrengende")
    private val cachedAzureAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    private val client = WebClient
        .builder()
        .build()

    internal suspend fun harLøpendeSakSomInvolvererEnAv(
        fraOgMed: LocalDate,
        søker: String,
        pleietrengende: String?,
        annenPart: String?,
        fagsakYtelseType: FagsakYtelseType,
    ): RutingGrunnlag {
        log.info("DEBUG: Kaller infotrygd med fraOgMed: [$fraOgMed] & fagsakYtelseType: [$fagsakYtelseType]")
        if (harSakSomSøker(søker, fraOgMed, fagsakYtelseType)) {
            return RutingGrunnlag(søker = true)
        }
        if (pleietrengende?.let { harSakSomPleietrengende(it, fraOgMed, fagsakYtelseType) } == true) {
            return RutingGrunnlag(søker = false, pleietrengende = true)
        }
        return RutingGrunnlag(
            søker = false,
            pleietrengende = false,
            annenPart = annenPart?.let { harSakSomSøker(it, fraOgMed, fagsakYtelseType) } ?: false
        )
    }

    private suspend fun harSakSomSøker(
        identitetsnummer: String,
        fraOgMed: LocalDate,
        fagsakYtelseType: FagsakYtelseType
    ): Boolean {
        val jsonPayload = jsonPayloadFraFnrOgFom(identitetsnummer, fraOgMed)
        val response = hentSakFraInfotrygd(jsonPayload, hentSakerUrl)

        return JSONArray(response).inneholderAktuelleSakerEllerVedtak(fagsakYtelseType)
    }

    private suspend fun harSakSomPleietrengende(
        identitetsnummer: String,
        fraOgMed: LocalDate,
        fagsakYtelseType: FagsakYtelseType,
    ): Boolean {
        val jsonPayload = jsonPayloadFraFnrOgFom(identitetsnummer, fraOgMed)
        val response = hentSakFraInfotrygd(jsonPayload, hentVedtakForPleietrengende)

        return JSONArray(response).inneholderAktuelleVedtak(fagsakYtelseType)
    }

    private suspend fun hentSakFraInfotrygd(payload: String, uri: URI): ResponseEntity<String>  {
        val response = client
            .post()
            .uri(uri)
            .header(
                HttpHeaders.AUTHORIZATION,
                cachedAzureAccessTokenClient.getAccessToken(infotrygdScopes).asAuthoriationHeader()
            )
            .header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
            .header("callId", coroutineContext.hentCallId())
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(payload)
            .retrieve()
            .toEntity(String::class.java)
            .awaitFirst()

        require(response.statusCode.is2xxSuccessful) {
            "Feil fra Infotrygd. URL=[$hentVedtakForPleietrengende], HttpStatusCode=[${response.statusCode}], Response=[$response]"
        }

        log.info("DEBUG: Svar fra infotrygd = [${response.body.toString()}]")
        return response
    }

    internal companion object {
        private val log = LoggerFactory.getLogger(InfotrygdClient::class.java)
        private const val ConsumerIdHeaderKey = "Nav-Consumer-Id"
        private const val ConsumerIdHeaderValue = "k9-punsj"

        private enum class Behandlingstema(val infotrygdVerdi: String) {
            PleiepengerSyktBarnGammelOrdning("PB"),
            PleiepengerILivetsSluttfase("PP"),
            Opplæringspenger("OP"),
            Omsorgspenger("OM");

            companion object {
                fun FagsakYtelseType.relevanteBehandlingstemaer() = when (this) {
                    FagsakYtelseType.PLEIEPENGER_SYKT_BARN -> listOf(PleiepengerSyktBarnGammelOrdning)
                    FagsakYtelseType.OMSORGSPENGER -> listOf(Omsorgspenger)
                    FagsakYtelseType.OMSORGSPENGER_KRONISK_SYKT_BARN -> listOf(Omsorgspenger)
                    FagsakYtelseType.OMSORGSPENGER_ALENE_OMSORGEN -> listOf(Omsorgspenger)
                    FagsakYtelseType.OMSORGSPENGER_MIDLERTIDIG_ALENE -> listOf(Omsorgspenger)
                    FagsakYtelseType.PLEIEPENGER_LIVETS_SLUTTFASE -> listOf(PleiepengerILivetsSluttfase)
                    else -> listOf(Omsorgspenger)
                }.map { it.infotrygdVerdi }
            }
        }

        private enum class Tema(val infotrygdVerdi: String) {
            BarnsSykdom("BS");

            companion object {
                internal val relevanteTemaer = listOf(
                    BarnsSykdom
                ).map { it.infotrygdVerdi }
            }
        }

        private enum class Resultat(val infotrygdVerdi: String) {
            HenlagtEllerBortfalt("HB")
        }

        private fun JSONObject.inneholderAktuelle(key: String, fagsakYtelseType: FagsakYtelseType) =
            arrayOrEmptyArray(key)
                .asSequence()
                .map { it as JSONObject }
                .filter { Tema.relevanteTemaer.contains(it.objectOrEmptyObject("tema").stringOrNull("kode")) }
                .filter {
                    fagsakYtelseType.relevanteBehandlingstemaer()
                        .contains(it.objectOrEmptyObject("behandlingstema").stringOrNull("kode"))
                }
                // Om den er henlagt/bortfalt og ikke har noen opphørsdato er det aldri gjort noen utbetalinger
                .filterNot {
                    Resultat.HenlagtEllerBortfalt.infotrygdVerdi == it.objectOrEmptyObject("resultat")
                        .stringOrNull("kode") && it.stringOrNull("opphoerFom") == null
                }
                .toList()
                .isNotEmpty()

        internal fun JSONArray.inneholderAktuelleSakerEllerVedtak(fagsakYtelseType: FagsakYtelseType) =
            map { it as JSONObject }
                .map { it.inneholderAktuelle("saker", fagsakYtelseType) || it.inneholderAktuelle("vedtak", fagsakYtelseType) }
                .any { it }

        internal fun JSONArray.inneholderAktuelleVedtak(fagsakYtelseType: FagsakYtelseType) =
            map { it as JSONObject }
                .map { it.inneholderAktuelle("vedtak", fagsakYtelseType) }
                .any { it }

        internal fun jsonPayloadFraFnrOgFom(identitetsnummer: String, fraOgMed: LocalDate) =
            """
            {
              "fnr": ["$identitetsnummer"],
              "fom": "$fraOgMed"
            }
           """.trimIndent()
    }

    override fun health() = Mono.just(
        accessTokenClient.helsesjekk(
            operasjon = "infotrygd-integrasjon",
            scopes = infotrygdScopes,
            initialHealth = accessTokenClient.helsesjekk(
                operasjon = "infotrygd-integrasjon",
                scopes = infotrygdScopes
            )
        )
    )
}

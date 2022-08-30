package no.nav.k9punsj.integrasjoner.infotrygd

import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpPost
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.jsonBody
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.readTextOrThrow
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.Identitetsnummer
import no.nav.k9punsj.felles.JsonUtil.arrayOrEmptyArray
import no.nav.k9punsj.felles.JsonUtil.objectOrEmptyObject
import no.nav.k9punsj.felles.JsonUtil.stringOrNull
import no.nav.k9punsj.integrasjoner.infotrygd.InfotrygdClient.Companion.Behandlingstema.Companion.relevanteBehandlingstemaer
import no.nav.k9punsj.ruting.RutingGrunnlag
import no.nav.k9punsj.tilgangskontroll.helsesjekk
import org.json.JSONArray
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.ReactiveHealthIndicator
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import reactor.core.publisher.Mono
import java.net.URI
import java.time.LocalDate

@Configuration
internal class InfotrygdClient(
    @Value("\${no.nav.infotrygd.base_url}") baseUrl: URI,
    @Value("\${no.nav.infotrygd.scope}") private val infotrygdScopes: Set<String>,
    @Qualifier("sts") private val accessTokenClient: AccessTokenClient
) : ReactiveHealthIndicator {

    private val HentSakerUrl = URI("$baseUrl/saker")
    private val HentVedtakForPleietrengende = URI("$baseUrl/vedtakForPleietrengende")
    private val cachedAzureAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    internal suspend fun harLøpendeSakSomInvolvererEnAv(
        fraOgMed: LocalDate,
        søker: Identitetsnummer,
        pleietrengende: Identitetsnummer?,
        annenPart: Identitetsnummer?,
        fagsakYtelseType: FagsakYtelseType,
    ): RutingGrunnlag {
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
        identitetsnummer: Identitetsnummer,
        fraOgMed: LocalDate,
        fagsakYtelseType: FagsakYtelseType
    ): Boolean {

        val url = URI("$HentSakerUrl")
        val jsonPayload = jsonPayloadFraFnrOgFom(identitetsnummer, fraOgMed)

        val (httpStatusCode, response) = url.toString().httpPost {
            it.header(HttpHeaders.AUTHORIZATION, cachedAzureAccessTokenClient.getAccessToken(infotrygdScopes).asAuthoriationHeader())
            //it.header(CorrelationIdHeaderKey, "$correlationId") TODO: Legg på callId
            it.header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
            it.accept(ContentType.Application.Json)
            it.jsonBody(jsonPayload)
        }.readTextOrThrow()

        require(httpStatusCode.isSuccess()) {
            "Feil fra Infotrygd. URL=[$HentSakerUrl], HttpStatusCode=[${httpStatusCode.value}], Response=[$response]"
        }

        return JSONArray(response).inneholderAktuelleSakerEllerVedtak(fagsakYtelseType)
    }

    private suspend fun harSakSomPleietrengende(
        identitetsnummer: Identitetsnummer,
        fraOgMed: LocalDate,
        fagsakYtelseType: FagsakYtelseType,
    ): Boolean {

        val url = URI("$HentVedtakForPleietrengende")
        val jsonPayload = jsonPayloadFraFnrOgFom(identitetsnummer, fraOgMed)

        val (httpStatusCode, response) = url.toString().httpPost {
            it.header(HttpHeaders.AUTHORIZATION, cachedAzureAccessTokenClient.getAccessToken(infotrygdScopes).asAuthoriationHeader())
            //it.header(CorrelationIdHeaderKey, "$correlationId") TODO: Legg på callId
            it.header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
            it.accept(ContentType.Application.Json)
            it.jsonBody(jsonPayload)
        }.readTextOrThrow()

        require(httpStatusCode.isSuccess()) {
            "Feil fra Infotrygd. URL=[$HentVedtakForPleietrengende], HttpStatusCode=[${httpStatusCode.value}], Response=[$response]"
        }

        return JSONArray(response).inneholderAktuelleVedtak(fagsakYtelseType)
    }

    internal companion object {
        private const val ConsumerIdHeaderKey = "Nav-Consumer-Id"
        private const val ConsumerIdHeaderValue = "k9-punsj"
        private const val CorrelationIdHeaderKey = "Nav-Callid"

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

        internal fun jsonPayloadFraFnrOgFom(identitetsnummer: Identitetsnummer, fraOgMed: LocalDate) =
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

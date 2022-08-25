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
import no.nav.k9punsj.felles.CorrelationId
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
import org.springframework.boot.actuate.health.Health
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
        punsjbolleSøknadstype: PunsjbolleSøknadstype,
        correlationId: CorrelationId,
    ): RutingGrunnlag {
        if (harSakSomSøker(søker, fraOgMed, punsjbolleSøknadstype, correlationId)) {
            return RutingGrunnlag(søker = true)
        }
        if (pleietrengende?.let { harSakSomPleietrengende(it, fraOgMed, punsjbolleSøknadstype, correlationId) } == true) {
            return RutingGrunnlag(søker = false, pleietrengende = true)
        }
        return RutingGrunnlag(
            søker = false,
            pleietrengende = false,
            annenPart = annenPart?.let { harSakSomSøker(it, fraOgMed, punsjbolleSøknadstype, correlationId) } ?: false
        )
    }

    private suspend fun harSakSomSøker(
        identitetsnummer: Identitetsnummer,
        fraOgMed: LocalDate,
        punsjbolleSøknadstype: PunsjbolleSøknadstype,
        correlationId: CorrelationId
    ): Boolean {

        val url = URI("$HentSakerUrl")
        val jsonPayload = jsonPayloadFraFnrOgFom(identitetsnummer, fraOgMed)

        val (httpStatusCode, response) = url.toString().httpPost {
            it.header(HttpHeaders.AUTHORIZATION, cachedAzureAccessTokenClient.getAccessToken(infotrygdScopes).asAuthoriationHeader())
            it.header(CorrelationIdHeaderKey, "$correlationId")
            it.header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
            it.accept(ContentType.Application.Json)
            it.jsonBody(jsonPayload)
        }.readTextOrThrow()

        require(httpStatusCode.isSuccess()) {
            "Feil fra Infotrygd. URL=[$HentSakerUrl], HttpStatusCode=[${httpStatusCode.value}], Response=[$response]"
        }

        return JSONArray(response).inneholderAktuelleSakerEllerVedtak(punsjbolleSøknadstype)
    }

    private suspend fun harSakSomPleietrengende(
        identitetsnummer: Identitetsnummer,
        fraOgMed: LocalDate,
        punsjbolleSøknadstype: PunsjbolleSøknadstype,
        correlationId: CorrelationId
    ): Boolean {

        val url = URI("$HentVedtakForPleietrengende")
        val jsonPayload = jsonPayloadFraFnrOgFom(identitetsnummer, fraOgMed)

        val (httpStatusCode, response) = url.toString().httpPost {
            it.header(HttpHeaders.AUTHORIZATION, cachedAzureAccessTokenClient.getAccessToken(infotrygdScopes).asAuthoriationHeader())
            it.header(CorrelationIdHeaderKey, "$correlationId")
            it.header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
            it.accept(ContentType.Application.Json)
            it.jsonBody(jsonPayload)
        }.readTextOrThrow()

        require(httpStatusCode.isSuccess()) {
            "Feil fra Infotrygd. URL=[$HentVedtakForPleietrengende], HttpStatusCode=[${httpStatusCode.value}], Response=[$response]"
        }

        return JSONArray(response).inneholderAktuelleVedtak(punsjbolleSøknadstype)
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
                fun PunsjbolleSøknadstype.relevanteBehandlingstemaer() = when (this) {
                    PunsjbolleSøknadstype.PleiepengerSyktBarn -> listOf(PleiepengerSyktBarnGammelOrdning)
                    PunsjbolleSøknadstype.OmsorgspengerUtbetaling_Korrigering -> listOf(Omsorgspenger)
                    PunsjbolleSøknadstype.OmsorgspengerUtbetaling_Arbeidstaker -> listOf(Omsorgspenger)
                    PunsjbolleSøknadstype.OmsorgspengerUtbetaling_Papirsøknad_Arbeidstaker -> listOf(Omsorgspenger)
                    PunsjbolleSøknadstype.OmsorgspengerKroniskSyktBarn -> listOf(Omsorgspenger)
                    PunsjbolleSøknadstype.OmsorgspengerMidlertidigAlene -> listOf(Omsorgspenger)
                    PunsjbolleSøknadstype.OmsorgspengerAleneOmsorg -> listOf(Omsorgspenger)
                    PunsjbolleSøknadstype.Omsorgspenger -> listOf(Omsorgspenger)
                    PunsjbolleSøknadstype.PleiepengerLivetsSluttfase -> listOf(PleiepengerILivetsSluttfase)
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

        private fun JSONObject.inneholderAktuelle(key: String, punsjbolleSøknadstype: PunsjbolleSøknadstype) =
            arrayOrEmptyArray(key)
                .asSequence()
                .map { it as JSONObject }
                .filter { Tema.relevanteTemaer.contains(it.objectOrEmptyObject("tema").stringOrNull("kode")) }
                .filter {
                    punsjbolleSøknadstype.relevanteBehandlingstemaer()
                        .contains(it.objectOrEmptyObject("behandlingstema").stringOrNull("kode"))
                }
                // Om den er henlagt/bortfalt og ikke har noen opphørsdato er det aldri gjort noen utbetalinger
                .filterNot {
                    Resultat.HenlagtEllerBortfalt.infotrygdVerdi == it.objectOrEmptyObject("resultat")
                        .stringOrNull("kode") && it.stringOrNull("opphoerFom") == null
                }
                .toList()
                .isNotEmpty()

        internal fun JSONArray.inneholderAktuelleSakerEllerVedtak(punsjbolleSøknadstype: PunsjbolleSøknadstype) =
            map { it as JSONObject }
                .map { it.inneholderAktuelle("saker", punsjbolleSøknadstype) || it.inneholderAktuelle("vedtak", punsjbolleSøknadstype) }
                .any { it }

        internal fun JSONArray.inneholderAktuelleVedtak(punsjbolleSøknadstype: PunsjbolleSøknadstype) =
            map { it as JSONObject }
                .map { it.inneholderAktuelle("vedtak", punsjbolleSøknadstype) }
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

package no.nav.k9punsj.integrasjoner.k9sak

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpPost
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9.sak.kontrakt.arbeidsforhold.InntektArbeidYtelseArbeidsforholdV2Dto
import no.nav.k9.sak.typer.Periode
import no.nav.k9punsj.StandardProfil
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.felles.NavHeaders
import no.nav.k9punsj.felles.dto.ArbeidsgiverMedArbeidsforholdId
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.hentCallId
import no.nav.k9punsj.integrasjoner.k9sak.K9SakServiceImpl.Urls.finnFagsak
import no.nav.k9punsj.integrasjoner.k9sak.K9SakServiceImpl.Urls.hentIntektsmelidnger
import no.nav.k9punsj.integrasjoner.k9sak.K9SakServiceImpl.Urls.hentPerioder
import no.nav.k9punsj.integrasjoner.k9sak.K9SakServiceImpl.Urls.hentPerioderForSaksnummer
import no.nav.k9punsj.integrasjoner.k9sak.K9SakServiceImpl.Urls.sokFagsaker
import no.nav.k9punsj.utils.objectMapper
import org.intellij.lang.annotations.Language
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import java.net.URI
import java.time.LocalDate
import java.util.*
import kotlin.coroutines.coroutineContext

@Configuration
@StandardProfil
class K9SakServiceImpl(
    @Value("\${no.nav.k9sak.base_url}") private val baseUrl: URI,
    @Value("\${no.nav.k9sak.scope}") private val k9sakScope: Set<String>,
    @Qualifier("sts") private val accessTokenClient: AccessTokenClient,
    private val personService: PersonService
) : K9SakService {

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)
    private val log = LoggerFactory.getLogger("K9SakService")

    internal object Urls {
        internal const val hentPerioder = "/behandling/soknad/perioder"
        internal const val hentIntektsmelidnger = "/behandling/iay/im-arbeidsforhold-v2"
        internal const val sokFagsaker = "/fagsak/sok"
        internal const val finnFagsak = "/fordel/fagsak/sok"
        internal const val hentPerioderForSaksnummer = "/behandling/soknad/perioder/saksnummer"
    }

    override suspend fun hentPerioderSomFinnesIK9(
        søker: String,
        barn: String?,
        fagsakYtelseType: no.nav.k9punsj.felles.FagsakYtelseType,
    ): Pair<List<PeriodeDto>?, String?> {
        val matchDto = MatchDto(
            ytelseType = FagsakYtelseType.fraKode(fagsakYtelseType.kode),
            bruker = søker,
            pleietrengende = barn
        )

        val body = kotlin.runCatching { objectMapper().writeValueAsString(matchDto) }.getOrNull()
            ?: return Pair(null, "Feilet serialisering")

        val (json, feil) = httpPost(body, hentPerioder)
        return try {
            if (json == null) {
                return Pair(null, feil!!)
            }
            val resultat = objectMapper().readValue<List<Periode>>(json)
            val liste = resultat
                .map { periode -> PeriodeDto(periode.fom, periode.tom) }.toList()
            Pair(liste, null)
        } catch (e: Exception) {
            Pair(null, "Feilet deserialisering $e")
        }
    }

    /*
     * 1. Slår opp saksnummer basert på ytelsetype, periode & søkers aktørId.
     * 2. Henter perioder for saksnummer.
     */
    override suspend fun hentPerioderSomFinnesIK9ForPeriode(
        søker: String,
        barn: String?,
        fagsakYtelseType: no.nav.k9punsj.felles.FagsakYtelseType,
        periode: PeriodeDto
    ): Pair<List<PeriodeDto>?, String?> {
        val søkerAktørId = personService.finnAktørId(søker)
        val finnFagsakDto = FinnFagsakDto(
            ytelseType = FagsakYtelseType.fraKode(fagsakYtelseType.kode),
            aktørId = søkerAktørId,
            pleietrengendeAktørId = barn,
            periode = periode
        )

        val saksnummerBody = kotlin.runCatching { objectMapper().writeValueAsString(finnFagsakDto) }.getOrNull()
            ?: return Pair(null, "Feilet serialisering")

        val (saksnummerJson, saksnummerFeil) = httpPost(saksnummerBody, finnFagsak)
        saksnummerFeil?.let { Pair(null, saksnummerFeil) }
        val saksnummer = saksnummerJson ?: return Pair(null, "Fant ikke saksnummer")

        val (json, feil) = httpPost(saksnummer, hentPerioderForSaksnummer)
        return try {
            if (json == null) {
                return Pair(null, feil!!)
            }
            val resultat = objectMapper().readValue<List<Periode>>(json)
            val liste = resultat
                .map { periode -> PeriodeDto(periode.fom, periode.tom) }.toList()
            Pair(liste, null)
        } catch (e: Exception) {
            Pair(null, "Feilet deserialisering $e")
        }
    }

    override suspend fun hentArbeidsforholdIdFraInntektsmeldinger(
        søker: String,
        fagsakYtelseType: no.nav.k9punsj.felles.FagsakYtelseType,
        periodeDto: PeriodeDto,
    ): Pair<List<ArbeidsgiverMedArbeidsforholdId>?, String?> {
        val matchDto = MatchArbeidsforholdDto(
            ytelseType = FagsakYtelseType.fraKode(fagsakYtelseType.kode),
            bruker = søker,
            periode = periodeDto
        )

        val body = kotlin.runCatching { objectMapper().writeValueAsString(matchDto) }.getOrNull()
            ?: return Pair(null, "Feilet serialisering")

        val (json, feil) = httpPost(body, hentIntektsmelidnger)

        return try {
            if (json == null) {
                return Pair(null, feil)
            }
            val dataSett = objectMapper().readValue<Set<InntektArbeidYtelseArbeidsforholdV2Dto>>(json)
            val map = dataSett.groupBy { it.arbeidsgiver }.map { entry ->
                ArbeidsgiverMedArbeidsforholdId(
                    orgNummerEllerAktørID = entry.key.identifikator,
                    arbeidsforholdId = entry.value.map { it.arbeidsforhold.eksternArbeidsforholdId }
                )
            }
            Pair(map, null)
        } catch (e: Exception) {
            Pair(null, "Feilet deserialisering $e")
        }
    }

    override suspend fun hentFagsaker(søker: String): Pair<Set<Fagsak>?, String?> {
        @Language("json")
        val body = """
            {
              "searchString": "$søker"
            }
        """.trimIndent()
        val (request, _, result) = "$baseUrl$sokFagsaker"
            .httpPost()
            .body(body)
            .header(
                HttpHeaders.ACCEPT to "application/json",
                HttpHeaders.AUTHORIZATION to cachedAccessTokenClient.getAccessToken(emptySet()).asAuthoriationHeader(),
                HttpHeaders.CONTENT_TYPE to "application/json",
                NavHeaders.CallId to hentCallId()
            ).awaitStringResponseResult()

        val (fagsaker: Set<Fagsak>?, feil: String?) = result.fold(
            { success -> Pair(success.fagsaker(), null) },
            { error ->
                log.error(
                    "Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'"
                )
                log.error(error.toString())
                Pair(null, "Feil ved henting av saker fra k9-sak")
            }
        )

        if (fagsaker == null) {
            return Pair(null, feil!!)
        }
        return Pair(fagsaker, null)
    }

    private suspend fun httpPost(body: String, url: String): Pair<String?, String?> {
        val (request, _, result) = "$baseUrl$url"
            .httpPost()
            .body(body)
            .header(
                HttpHeaders.ACCEPT to "application/json",
                HttpHeaders.AUTHORIZATION to cachedAccessTokenClient.getAccessToken(emptySet()).asAuthoriationHeader(),
                HttpHeaders.CONTENT_TYPE to "application/json",
                NavHeaders.CallId to hentCallId()
            ).awaitStringResponseResult()

        return result.fold(
            { success ->
                Pair(success, null)
            },
            { error ->
                log.error(
                    "Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'"
                )
                log.error(error.toString())
                Pair(null, "Feil ved henting av peridoer fra k9-sak")
            }
        )
    }

    private companion object {
        suspend fun hentCallId() = try {
            coroutineContext.hentCallId()
        } catch (e: Exception) {
            UUID.randomUUID().toString()
        }

        fun String.fagsaker() = JSONArray(this)
            .asSequence()
            .map { it as JSONObject }
            .map {
                val saksnummer = it.getString("saksnummer")
                val sakstypeKode = it.getJSONObject("sakstype").getString("kode")
                val pleietrengende = it.getStringOrNull("pleietrengendeAktørId")
                val fagsakYtelseType = FagsakYtelseType.fraKode(sakstypeKode)
                val gyldigPeriode: JSONObject? = it.optJSONObject("gyldigPeriode")
                val periodeDto: PeriodeDto? = gyldigPeriode?.somPeriodeDto()
                Fagsak(
                    saksnummer = saksnummer,
                    sakstype = fagsakYtelseType,
                    pleietrengendeAktorId = pleietrengende,
                    gyldigPeriode = periodeDto
                )
            }.toSet()

        fun JSONObject.getStringOrNull(key: String) = if (this.has(key)) this.getString(key) else null
        private fun JSONObject.somPeriodeDto() = PeriodeDto(
            fom = LocalDate.parse(getString("fom")),
            tom = LocalDate.parse(getString("tom"))
        )

        data class MatchDto(
            val ytelseType: FagsakYtelseType,
            val bruker: String,
            val pleietrengende: String? = null,
        )

        data class FinnFagsakDto(
            val ytelseType: FagsakYtelseType,
            val aktørId: String,
            val pleietrengendeAktørId: String? = null,
            val periode: PeriodeDto
        )

        data class MatchArbeidsforholdDto(
            val ytelseType: FagsakYtelseType,
            val bruker: String,
            val periode: PeriodeDto,
        )
    }
}

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
import no.nav.k9punsj.felles.AktørId
import no.nav.k9punsj.felles.CorrelationId
import no.nav.k9punsj.felles.Identitetsnummer
import no.nav.k9punsj.felles.NavHeaders
import no.nav.k9punsj.felles.dto.ArbeidsgiverMedArbeidsforholdId
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.hentCallId
import no.nav.k9punsj.integrasjoner.infotrygd.PunsjbolleSøknadstype
import no.nav.k9punsj.integrasjoner.k9sak.K9SakServiceImpl.Urls.pleiepengerSyktBarnUnntakslisteUrl
import no.nav.k9punsj.integrasjoner.k9sak.K9SakServiceImpl.Urls.hentIntektsmelidnger
import no.nav.k9punsj.integrasjoner.k9sak.K9SakServiceImpl.Urls.hentPerioder
import no.nav.k9punsj.integrasjoner.k9sak.K9SakServiceImpl.Urls.matchFagsakUrl
import no.nav.k9punsj.integrasjoner.k9sak.K9SakServiceImpl.Urls.sokFagsaker
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.ruting.RutingGrunnlag
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
    @Qualifier("sts") private val accessTokenClient: AccessTokenClient,
) : K9SakService {

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)
    private val log = LoggerFactory.getLogger("K9SakService")

    internal object Urls {
        internal const val hentPerioder = "/behandling/soknad/perioder"
        internal const val hentIntektsmelidnger = "/behandling/iay/im-arbeidsforhold-v2"
        internal const val sokFagsaker = "/fagsak/sok"
        internal const val matchFagsakUrl = "/api/fagsak/match"
        internal const val pleiepengerSyktBarnUnntakslisteUrl ="/api/fordel/psb-infotrygd/finnes"
    }

    override suspend fun hentPerioderSomFinnesIK9(
        søker: String,
        barn: String,
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

    override suspend fun hentArbeidsforholdIdFraInntektsmeldinger(
        søker: String,
        fagsakYtelseType: no.nav.k9punsj.felles.FagsakYtelseType,
        periodeDto: PeriodeDto,
    ): Pair<List<ArbeidsgiverMedArbeidsforholdId>?, String?> {
        val matchDto = MatchMedPeriodeDto(
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

        val (json, feil) = httpPost(body, "$baseUrl$sokFagsaker")

        return if(!json.isNullOrEmpty()) {
            Pair(json.fagsaker(), null)
        } else {
            Pair(null, feil)
        }
    }

    internal suspend fun harLopendeSakSomInvolvererEnAv(
        søker: Identitetsnummer,
        pleietrengende: Identitetsnummer?,
        annenPart: Identitetsnummer?,
        fraOgMed: LocalDate,
        punsjbolleSøknadstype: PunsjbolleSøknadstype,
        correlationId: CorrelationId
    ): RutingGrunnlag {
        if (finnesMatchendeFagsak(
                søker = søker,
                fraOgMed = fraOgMed,
                punsjbolleSøknadstype = punsjbolleSøknadstype
            )
        ) {
            return RutingGrunnlag(søker = true)
        }
        if (pleietrengende?.let {
                finnesMatchendeFagsak(
                    pleietrengende = it,
                    fraOgMed = fraOgMed,
                    punsjbolleSøknadstype = punsjbolleSøknadstype
                )
            } == true) {
            return RutingGrunnlag(søker = false, pleietrengende = true)
        }
        return RutingGrunnlag(
            søker = false,
            pleietrengende = false,
            annenPart = annenPart?.let {
                finnesMatchendeFagsak(
                    søker = it,
                    fraOgMed = fraOgMed,
                    punsjbolleSøknadstype = punsjbolleSøknadstype
                )
            } ?: false
        )
    }

    internal suspend fun inngårIUnntaksliste(
        aktørIder: Set<AktørId>,
        punsjbolleSøknadstype: PunsjbolleSøknadstype,
        correlationId: CorrelationId
    ): Boolean {

        // https://github.com/navikt/k9-sak/tree/3.2.7/web/src/main/java/no/nav/k9/sak/web/app/tjenester/fordeling/FordelRestTjeneste.java#L164
        // https://github.com/navikt/k9-sak/tree/3.2.7/kontrakt/src/main/java/no/nav/k9/sak/kontrakt/mottak/Akt%C3%B8rListeDto.java#L19
        val dto = JSONObject().also {
            it.put("aktører", JSONArray(aktørIder.map { aktørId -> "$aktørId" }))
        }.toString()

        val (resultat, feil) = httpPost(pleiepengerSyktBarnUnntakslisteUrl, dto)

        if(!feil.isNullOrEmpty()) {
            return false
        }

        return (resultat == "true")
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

    private suspend fun finnesMatchendeFagsak(
        søker: Identitetsnummer? = null,
        pleietrengende: Identitetsnummer? = null,
        annenPart: Identitetsnummer? = null,
        @Suppress("UNUSED_PARAMETER") fraOgMed: LocalDate,
        punsjbolleSøknadstype: PunsjbolleSøknadstype
    ): Boolean {

        // https://github.com/navikt/k9-sak/tree/3.1.30/kontrakt/src/main/java/no/nav/k9/sak/kontrakt/fagsak/MatchFagsak.java#L26
        @Language("JSON")
        val dto = """
        {
            "ytelseType": {
                "kode": "${punsjbolleSøknadstype.k9YtelseType}",
                "kodeverk": "FAGSAK_YTELSE"
            },
            "periode": {},
            "bruker": ${søker?.let { """"$it"""" }},
            "pleietrengendeIdenter": ${pleietrengende.jsonArray()},
            "relatertPersonIdenter": ${annenPart.jsonArray()}
        }
        """.trimIndent()

        val (json, feil) = httpPost(dto, matchFagsakUrl)

        if(json.isNullOrEmpty()) return false
            .also { log.error("Inget svar fra K9sak vid søk på matchende fagsak, feil: $feil") }

        return json.inneholderMatchendeFagsak()
    }

    internal companion object {
        private suspend fun hentCallId() = try {
            coroutineContext.hentCallId()
        } catch (e: Exception) {
            UUID.randomUUID().toString()
        }

        private fun String.fagsaker() = JSONArray(this)
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

        private fun JSONObject.getStringOrNull(key: String) = if (this.has(key)) this.getString(key) else null
        private fun JSONObject.somPeriodeDto() = PeriodeDto(
            fom = LocalDate.parse(getString("fom")),
            tom = LocalDate.parse(getString("tom"))
        )

        private data class MatchDto(
            val ytelseType: FagsakYtelseType,
            val bruker: String,
            val pleietrengende: String,
        )

        private data class MatchMedPeriodeDto(
            val ytelseType: FagsakYtelseType,
            val bruker: String,
            val periode: PeriodeDto,
        )

        private fun Identitetsnummer?.jsonArray() = when (this) {
            null -> "[]"
            else -> """["$this"]"""
        }

        fun String.inneholderMatchendeFagsak() = JSONArray(this)
            .asSequence()
            .map { it as JSONObject }
            .filterNot { it.getString("status") == "OPPR" }
            .toSet()
            .isNotEmpty()
    }
}
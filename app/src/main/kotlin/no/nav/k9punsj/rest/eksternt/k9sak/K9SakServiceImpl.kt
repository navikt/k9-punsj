package no.nav.k9punsj.rest.eksternt.k9sak

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpPost
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9.sak.kontrakt.arbeidsforhold.InntektArbeidYtelseArbeidsforholdV2Dto
import no.nav.k9.sak.typer.Periode
import no.nav.k9punsj.StandardProfil
import no.nav.k9punsj.abac.NavHeaders
import no.nav.k9punsj.db.datamodell.NorskIdent
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.eksternt.k9sak.K9SakServiceImpl.Urls.hentIntektsmelidnger
import no.nav.k9punsj.rest.eksternt.k9sak.K9SakServiceImpl.Urls.hentPerioder
import no.nav.k9punsj.rest.web.dto.ArbeidsgiverMedArbeidsforholdId
import no.nav.k9punsj.rest.web.dto.PeriodeDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import java.net.URI
import java.util.*

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
    }

    override suspend fun hentPerioderSomFinnesIK9(
        søker: NorskIdent,
        barn: NorskIdent,
        fagsakYtelseType: no.nav.k9punsj.db.datamodell.FagsakYtelseType,
    ): Pair<List<PeriodeDto>?, String?> {

        val matchDto = MatchDto(FagsakYtelseType.fraKode(fagsakYtelseType.kode),
            søker,
            barn)

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
        søker: NorskIdent,
        fagsakYtelseType: no.nav.k9punsj.db.datamodell.FagsakYtelseType,
        periodeDto: PeriodeDto,
    ): Pair<List<ArbeidsgiverMedArbeidsforholdId>?, String?> {
        val matchDto = MatchMedPeriodeDto(FagsakYtelseType.fraKode(fagsakYtelseType.kode),
            søker,
            periodeDto)

        val body = kotlin.runCatching { objectMapper().writeValueAsString(matchDto) }.getOrNull()
            ?: return Pair(null, "Feilet serialisering")

        val (json, feil) = httpPost(body, hentIntektsmelidnger)

        return try {
            if (json == null) {
                return Pair(null, feil)
            }
            val dataSett = objectMapper().readValue<Set<InntektArbeidYtelseArbeidsforholdV2Dto>>(json)
            val map = dataSett.groupBy { it.arbeidsgiver }.map { entry ->
                ArbeidsgiverMedArbeidsforholdId(entry.key.identifikator,
                    entry.value.map { it.arbeidsforhold.eksternArbeidsforholdId })
            }
            Pair(map, null)
        } catch (e: Exception) {
            Pair(null, "Feilet deserialisering $e")
        }
    }

    private suspend fun httpPost(body: String, url: String): Pair<String?, String?> {
        val (request, _, result) = "$baseUrl$url"
            .httpPost()
            .body(
                body
            )
            .header(
                HttpHeaders.ACCEPT to "application/json",
                HttpHeaders.AUTHORIZATION to cachedAccessTokenClient.getAccessToken(emptySet()).asAuthoriationHeader(),
                HttpHeaders.CONTENT_TYPE to "application/json",
                NavHeaders.CallId to UUID.randomUUID().toString()
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

    data class MatchDto(
        val ytelseType: FagsakYtelseType,
        val bruker: String,
        val pleietrengende: String,
    )

    data class MatchMedPeriodeDto(
        val ytelseType: FagsakYtelseType,
        val bruker: String,
        val periode: PeriodeDto
    )
}

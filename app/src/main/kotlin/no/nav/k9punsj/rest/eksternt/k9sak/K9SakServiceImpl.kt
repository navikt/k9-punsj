package no.nav.k9punsj.rest.eksternt.k9sak

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpPost
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9.sak.typer.Periode
import no.nav.k9punsj.abac.NavHeaders
import no.nav.k9punsj.db.datamodell.NorskIdent
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.dto.PeriodeDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.ReactiveHealthIndicator
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import reactor.core.publisher.Mono
import java.net.URI
import java.util.UUID

@Configuration
@Profile("!test & !local")
class K9SakServiceImpl(
    @Value("\${no.nav.k9sak.base_url}") private val baseUrl: URI,
    @Qualifier("sts") private val accessTokenClient: AccessTokenClient,
) : ReactiveHealthIndicator, K9SakService {

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)
    val log = LoggerFactory.getLogger("K9SakService")

    override fun health(): Mono<Health> {
        TODO("Not yet implemented")
    }

    override suspend fun hentPerioderSomFinnesIK9(
        søker: NorskIdent,
        barn: NorskIdent,
        fagsakYtelseType: no.nav.k9punsj.db.datamodell.FagsakYtelseType,
    ): Pair<List<PeriodeDto>?, String?> {

        val matchDto = MatchDto(FagsakYtelseType.fraKode(fagsakYtelseType.kode),
            søker,
            barn)

        val body = objectMapper().writeValueAsString(matchDto)

        val (request, _, result) = "${baseUrl}/behandling/soknad/perioder"
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

        val json = result.fold(
            { success ->
                Pair(success, null) },
            { error ->
                log.error(
                    "Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'"
                )
                log.error(error.toString())
                Pair(null, "Feil ved henting av peridoer fra k9-sak")
            }
        )
        return try {
            if (json.first == null) {
                return Pair(null, json.second!!)
            }
            val resultat = objectMapper().readValue<List<Periode>>(json.first!!)
            val liste = resultat
                .map { periode -> PeriodeDto(periode.fom, periode.tom) }.toList()
            Pair(liste, null)
        } catch (e: Exception) {
            Pair(null, "Feilet deserialisering $e")
        }
    }

    data class MatchDto(
        val ytelseType: FagsakYtelseType,
        val bruker: String,
        val pleietrengende: String,
    )
}

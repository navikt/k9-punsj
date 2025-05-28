package no.nav.k9punsj.tilgangskontroll.abac

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9punsj.felles.RestKallException
import no.nav.k9punsj.hentCallId
import no.nav.k9punsj.idToken
import no.nav.k9punsj.utils.objectMapper
import no.nav.sif.abac.kontrakt.abac.BeskyttetRessursActionAttributt
import no.nav.sif.abac.kontrakt.abac.ResourceType
import no.nav.sif.abac.kontrakt.abac.dto.OperasjonDto
import no.nav.sif.abac.kontrakt.abac.dto.PersonerOperasjonDto
import no.nav.sif.abac.kontrakt.abac.resultat.Tilgangsbeslutning
import no.nav.sif.abac.kontrakt.person.PersonIdent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.net.URI
import java.util.*
import kotlin.coroutines.coroutineContext

@Service
class SifAbacPdpKlient(
    accessTokenClient: AccessTokenClient,
    @Value("\${no.nav.sif.abac.pdp.base_url}") private val baseUrl: URI,
    @Value("\${no.nav.sif.abac.pdp.scope}") private val scopes: Set<String>,
) {
    val log = LoggerFactory.getLogger("SifAbacPdpKlient")
    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)
    private val om = objectMapper(kodeverdiSomString = true)

    suspend fun harTilgangTilPersoner(action: BeskyttetRessursActionAttributt, personIdenter: List<PersonIdent>): Boolean {
        val request = PersonerOperasjonDto(emptyList(), personIdenter, OperasjonDto(ResourceType.FAGSAK, action))

        val response = httpPostMedOboToken(om.writeValueAsString(request), "${baseUrl}/personer")
        return om.readValue<Tilgangsbeslutning>(response).harTilgang()
    }

    private suspend fun httpPostMedOboToken(body: String, url: String): String {
        val jwt = coroutineContext.idToken().value
        val oboToken = cachedAccessTokenClient.getAccessToken(scopes, jwt)

        val (request, _, result) = url
            .httpPost()
            .body(body)
            .header(
                HttpHeaders.ACCEPT to "application/json",
                HttpHeaders.AUTHORIZATION to oboToken.asAuthoriationHeader(),
                HttpHeaders.CONTENT_TYPE to "application/json",
                "callId" to hentCallId()
            ).awaitStringResponseResult()

        return håndterFuelResult(result, request)
    }

    private fun håndterFuelResult(
        result: Result<String, FuelError>,
        request: Request,
    ) = result.fold(
        { success: String -> success },
        { error: FuelError ->
            log.error(
                "Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'"
            )
            log.error(error.toString())
            throw RestKallException(
                titel = "Restkall mot sif-abac-pdp feilet",
                message = error.response.body().asString("text/plain"),
                httpStatus = HttpStatus.valueOf(error.response.statusCode),
                uri = error.response.url.toURI()
            )
        }
    )

    internal companion object {
        private suspend fun hentCallId() = try {
            coroutineContext.hentCallId()
        } catch (e: Exception) {
            UUID.randomUUID().toString()
        }
    }
}
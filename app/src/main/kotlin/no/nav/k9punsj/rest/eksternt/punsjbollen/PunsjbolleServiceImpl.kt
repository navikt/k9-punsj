package no.nav.k9punsj.rest.eksternt.punsjbollen

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpPost
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9punsj.abac.NavHeaders
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.dto.NorskIdentDto
import no.nav.k9punsj.rest.web.dto.SaksnummerDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import java.net.URI
import java.time.LocalDate
import java.util.UUID


@Configuration
@Profile("!test")
class PunsjbolleServiceImpl(
    @Value("\${no.nav.k9punsjbolle.base_url}") private val baseUrl: URI,
    @Qualifier("azure") private val accessTokenClient: AccessTokenClient,
) : PunsjbolleService {

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)
    val log = LoggerFactory.getLogger("PunsjbollenService")

    override suspend fun opprettEllerHentFagsaksnummer(
        søker: NorskIdentDto,
        barn: NorskIdentDto,
        fraOgMed: LocalDate,
    ): SaksnummerDto? {

        val punsjbollSaksnummerDto = PunsjbollSaksnummerDto(
            søker = PunsjbollSaksnummerDto.PunsjbollePersonDto("", ""),
            pleietrengende = PunsjbollSaksnummerDto.PunsjbollePersonDto("", ""),
            søknadstype = "PleiepengerSyktBarn", "",
            fraOgMed = LocalDate.now())

        val body = objectMapper().writeValueAsString(punsjbollSaksnummerDto)

        val (request, _, result) = "${baseUrl}/saksnummer"
            .httpPost()
            .body(
                body
            )
            .header(
                HttpHeaders.ACCEPT to "application/json",
                HttpHeaders.AUTHORIZATION to cachedAccessTokenClient.getAccessToken(emptySet()).asAuthoriationHeader(),
                HttpHeaders.CONTENT_TYPE to "application/json",
                NavHeaders.XCorrelationId to UUID.randomUUID().toString()
            ).awaitStringResponseResult()


        val json = result.fold(
            { success -> success },
            { error ->
                // conflict
                if (error.response.statusCode == 409) {
                    null
                } else {
                    log.error(
                        "Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'"
                    )
                    log.error(error.toString())
                    throw IllegalStateException("Feil ved henting av fagsaker fra k9-sak")
                }
            }
        )

        try {
            if (json == null) {
                return null
            }
            return objectMapper().readValue(json)
        } catch (e: Exception) {
            log.error("Feilet deserialisering $e")
            throw IllegalStateException("Feilet deserialisering $e")
        }
    }

    data class PunsjbollSaksnummerDto(
        val søker: PunsjbollePersonDto,
        val pleietrengende: PunsjbollePersonDto,
        val søknadstype: String,
        val journalpostId: String,
        val fraOgMed: LocalDate,

        ) {
        data class PunsjbollePersonDto(
            val identitetsnumme: String,
            val aktørId: String,
        )
    }
}

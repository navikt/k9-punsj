package no.nav.k9punsj.rest.eksternt.punsjbollen

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpPost
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9punsj.abac.NavHeaders
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.dto.JournalpostIdDto
import no.nav.k9punsj.rest.web.dto.NorskIdentDto
import no.nav.k9punsj.rest.web.dto.SaksnummerDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import java.net.URI
import java.util.UUID


@Configuration
@Profile("!test & !local")
class PunsjbolleServiceImpl(
    @Value("\${no.nav.k9punsjbolle.base_url}") private val baseUrl: URI,
    @Value("\${no.nav.k9punsjbolle.scope}") private val scope: String,
    @Qualifier("azure") private val accessTokenClient: AccessTokenClient,
    private val personService: PersonService,
) : PunsjbolleService {

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)
    val log = LoggerFactory.getLogger("PunsjbollenService")

    override suspend fun opprettEllerHentFagsaksnummer(
        søker: NorskIdentDto,
        barn: NorskIdentDto,
        journalpostIdDto: JournalpostIdDto,
        annenPart: NorskIdentDto?
    ): SaksnummerDto? {
        val søkerPerson = personService.finnEllerOpprettPersonVedNorskIdent(søker)
        val barnPerson = personService.finnEllerOpprettPersonVedNorskIdent(barn)
        val annenPartPerson = annenPart?.let { personService.finnEllerOpprettPersonVedNorskIdent(annenPart) }

        val punsjbollSaksnummerDto = PunsjbollSaksnummerDto(
            søker = PunsjbollSaksnummerDto.PunsjbollePersonDto(søkerPerson.norskIdent, søkerPerson.aktørId),
            pleietrengende = PunsjbollSaksnummerDto.PunsjbollePersonDto(barnPerson.norskIdent, barnPerson.aktørId),
            annenPart = annenPartPerson?.let { PunsjbollSaksnummerDto.PunsjbollePersonDto(annenPartPerson.norskIdent, annenPartPerson.aktørId) },
            søknadstype = "PleiepengerSyktBarn",
            journalpostId = journalpostIdDto
        )

        val body = objectMapper().writeValueAsString(punsjbollSaksnummerDto)

        val (request, _, result) = "${baseUrl}/saksnummer"
            .httpPost()
            .body(
                body
            )
            .header(
                HttpHeaders.ACCEPT to "application/json",
                HttpHeaders.AUTHORIZATION to cachedAccessTokenClient.getAccessToken(setOf(scope))
                    .asAuthoriationHeader(),
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
                    throw IllegalStateException("Feil ved request av saksnummer fra k9-punsjbolle")
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
        val annenPart: PunsjbollePersonDto?,
        val søknadstype: String,
        val journalpostId: String,

        ) {
        data class PunsjbollePersonDto(
            val identitetsnummer: String,
            val aktørId: String,
        )
    }
}

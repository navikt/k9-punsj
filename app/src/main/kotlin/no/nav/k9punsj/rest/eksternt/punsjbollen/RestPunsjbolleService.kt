package no.nav.k9punsj.rest.eksternt.punsjbollen

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpPost
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9punsj.CorrelationId
import no.nav.k9punsj.abac.NavHeaders
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.dto.JournalpostIdDto
import no.nav.k9punsj.rest.web.dto.NorskIdentDto
import no.nav.k9punsj.rest.web.dto.PeriodeDto
import no.nav.k9punsj.rest.web.dto.SaksnummerDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import java.net.URI
import java.time.LocalDate

@Configuration
@Profile("!test & !local")
class RestPunsjbolleService(
    @Value("\${no.nav.k9punsjbolle.base_url}") private val baseUrl: URI,
    @Value("\${no.nav.k9punsjbolle.scope}") private val scope: String,
    @Qualifier("azure") private val accessTokenClient: AccessTokenClient,
    private val personService: PersonService,
) : PunsjbolleService {

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    override suspend fun opprettEllerHentFagsaksnummer(
        søker: NorskIdentDto,
        barn: NorskIdentDto,
        journalpostId: JournalpostIdDto?,
        periode: PeriodeDto?,
        correlationId: CorrelationId
    ) = opprettEllerHentFagsaksnummer(
        dto = punsjbolleDto(søker, barn, journalpostId, periode),
        correlationId = correlationId
    )

    override suspend fun kanRutesTilK9Sak(
        søker: NorskIdentDto,
        barn: NorskIdentDto,
        journalpostId: JournalpostIdDto?,
        periode: PeriodeDto?,
        correlationId: CorrelationId
    ) = ruting(
        søker = søker,
        barn = barn,
        journalpostId = journalpostId,
        periode = periode,
        correlationId = correlationId
    ) == PunsjbolleRuting.K9SAK

    override suspend fun ruting(
        søker: NorskIdentDto,
        barn: NorskIdentDto,
        journalpostId: JournalpostIdDto?,
        periode: PeriodeDto?,
        correlationId: CorrelationId
    ): PunsjbolleRuting {
        val requestBody = objectMapper().writeValueAsString(punsjbolleDto(søker, barn, journalpostId, periode))

        val (_, response, result) = "${baseUrl}/ruting"
            .httpPost()
            .body(requestBody)
            .header(
                HttpHeaders.ACCEPT to "application/json",
                HttpHeaders.AUTHORIZATION to cachedAccessTokenClient.getAccessToken(setOf(scope)).asAuthoriationHeader(),
                HttpHeaders.CONTENT_TYPE to "application/json",
                NavHeaders.XCorrelationId to correlationId
            ).awaitStringResponseResult()

        val responseBody = result.fold(
            { success -> success },
            { error -> when (error.response.body().isEmpty()) {
                true -> "{}"
                false -> String(error.response.body().toByteArray())
            }}
        )

        val rutingResponse : RutingResponse = try {
            objectMapper().readValue(responseBody)
        } catch (e: Exception) {
            throw IllegalStateException("Uventet response fra Punsjbollen ved ruting. PunsjbolleResponse=$responseBody", e)
        }

        return when {
            response.statusCode == 200 && rutingResponse.destinasjon == "K9Sak" -> PunsjbolleRuting.K9SAK
            response.statusCode == 200 && rutingResponse.destinasjon == "Infotrygd" -> PunsjbolleRuting.INFOTRYGD
            response.statusCode == 409 && rutingResponse.type == "punsjbolle://ikke-støttet-journalpost" -> PunsjbolleRuting.IKKE_STØTTET.also {
                log.error("Ikke støttet journalpost. PunsjbolleResponse=$responseBody")
            }
            else -> throw IllegalStateException("Uventet response fra Punsjbollen ved ruting. PunsjbolleResponse=$responseBody")
        }
    }

    private suspend fun punsjbolleDto(
        søker: NorskIdentDto,
        barn: NorskIdentDto,
        journalpostIdDto: JournalpostIdDto?,
        periode: PeriodeDto?) : PunsjbollSaksnummerDto {
        val søkerPerson = personService.finnEllerOpprettPersonVedNorskIdent(søker)
        val barnPerson = personService.finnEllerOpprettPersonVedNorskIdent(barn)
        return PunsjbollSaksnummerDto(
            søker = PunsjbollSaksnummerDto.PunsjbollePersonDto(søkerPerson.norskIdent, søkerPerson.aktørId),
            pleietrengende = PunsjbollSaksnummerDto.PunsjbollePersonDto(barnPerson.norskIdent, barnPerson.aktørId),
            periode = periode?.let {
               require(it.fom != null || it.tom != null) { "Må sette enten fom eller tom" }
                "${it.fom.iso8601()}/${it.tom.iso8601()}"
            },
            søknadstype = "PleiepengerSyktBarn",
            journalpostId = journalpostIdDto
        )
    }

    private suspend fun opprettEllerHentFagsaksnummer(
        dto: PunsjbollSaksnummerDto,
        correlationId: CorrelationId
    ): SaksnummerDto {
        val body = objectMapper().writeValueAsString(dto)

        val (request, _, result) = "${baseUrl}/saksnummer"
            .httpPost()
            .body(body)
            .header(
                HttpHeaders.ACCEPT to "application/json",
                HttpHeaders.AUTHORIZATION to cachedAccessTokenClient.getAccessToken(setOf(scope)).asAuthoriationHeader(),
                HttpHeaders.CONTENT_TYPE to "application/json",
                NavHeaders.XCorrelationId to correlationId
            ).awaitStringResponseResult()


        val json = result.fold(
            { success -> success },
            { error ->
                log.error("Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}', $error")
                throw IllegalStateException("Feil ved henting av saksnummer fra k9-punsjbolle")
            }
        )

        try {
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
        val journalpostId: String? = null,
        val periode: String? = null) {
        init { require(journalpostId != null || periode != null) {
            "Må sette minst en av journalpostId og periode"
        }}
        data class PunsjbollePersonDto(
            val identitetsnummer: String,
            val aktørId: String,
        )
    }

    data class RutingResponse(
        val destinasjon: String? = null,
        val type: String? = null
    )

    private companion object {
        private fun LocalDate?.iso8601() = when (this) {
            null -> ".."
            else -> "$this"
        }
        private val log = LoggerFactory.getLogger(RestPunsjbolleService::class.java)
    }
}

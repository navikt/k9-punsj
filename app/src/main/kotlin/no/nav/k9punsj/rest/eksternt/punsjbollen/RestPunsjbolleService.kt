package no.nav.k9punsj.rest.eksternt.punsjbollen

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.isSuccessful
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpPost
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9.søknad.Søknad
import no.nav.k9punsj.CorrelationId
import no.nav.k9punsj.StandardProfil
import no.nav.k9punsj.abac.NavHeaders
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.innsending.InnsendingClient.Companion.somMap
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.dto.JournalpostIdDto
import no.nav.k9punsj.rest.web.dto.NorskIdentDto
import no.nav.k9punsj.rest.web.dto.PeriodeDto
import no.nav.k9punsj.rest.web.dto.SaksnummerDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import java.net.URI
import java.time.LocalDate

@Configuration
@StandardProfil
class RestPunsjbolleService(
    @Value("\${no.nav.k9punsjbolle.base_url}") private val baseUrl: URI,
    @Value("\${no.nav.k9punsjbolle.scope}") private val scope: String,
    @Qualifier("azure") private val accessTokenClient: AccessTokenClient,
    private val personService: PersonService,
) : PunsjbolleService {

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    override suspend fun opprettEllerHentFagsaksnummer(
        søker: NorskIdentDto,
        pleietrengende: NorskIdentDto,
        journalpostId: JournalpostIdDto?,
        periode: PeriodeDto?,
        correlationId: CorrelationId,
        fagsakYtelseType: FagsakYtelseType
    ): SaksnummerDto {
        val requestBody = punsjbolleSaksnummerDto(
            søker = søker,
            pleietrengende = pleietrengende,
            journalpostId = journalpostId,
            periode = periode,
            fagsakYtelseType = fagsakYtelseType
        )

        val (url, response, responseBody) = "saksnummer".post(
            requestBody = requestBody,
            correlationId = correlationId
        )

        check(response.isSuccessful) {
            "Feil ved opprettEllerHentFagsaksnummer. Url=[$url], HttpStatus=[${response.statusCode}], Response=$responseBody"
        }

        return responseBody.deserialiser()
    }

    override suspend fun opprettEllerHentFagsaksnummer(
        søker: NorskIdentDto,
        pleietrengende: NorskIdentDto,
        søknad: Søknad,
        correlationId: CorrelationId,
    ): SaksnummerDto {
        val requestBody = punsjbolleSaksnummerFraSøknadDto(
            søker = søker,
            pleietrengende = pleietrengende,
            søknad = søknad
        )

        val (url, response, responseBody) = "saksnummer-fra-søknad".post(
            requestBody = requestBody,
            correlationId = correlationId
        )

        check(response.isSuccessful) {
            "Feil ved opprettEllerHentFagsaksnummer. Url=[$url], HttpStatus=[${response.statusCode}], Response=$responseBody"
        }

        return responseBody.deserialiser()
    }

    override suspend fun ruting(
        søker: NorskIdentDto,
        pleietrengende: NorskIdentDto,
        journalpostId: JournalpostIdDto?,
        periode: PeriodeDto?,
        correlationId: CorrelationId,
        fagsakYtelseType: FagsakYtelseType
    ): PunsjbolleRuting {
        val requestBody = punsjbolleSaksnummerDto(
            søker = søker,
            pleietrengende = pleietrengende,
            journalpostId = journalpostId,
            periode = periode,
            fagsakYtelseType = fagsakYtelseType
        )

        val (url, response, responseBody) = "ruting".post(
            requestBody = requestBody,
            correlationId = correlationId
        )

        val rutingResponse: RutingResponse = responseBody.deserialiser()

        return when {
            response.statusCode == 200 && rutingResponse.destinasjon == "K9Sak" -> PunsjbolleRuting.K9Sak
            response.statusCode == 200 && rutingResponse.destinasjon == "Infotrygd" -> PunsjbolleRuting.Infotrygd
            response.statusCode == 409 && rutingResponse.type == "punsjbolle://ikke-støttet-journalpost" -> PunsjbolleRuting.IkkeStøttet.also {
                log.info("Ikke støttet journalpost ved ruting. Response=$responseBody")
            }
            else -> throw IllegalStateException("Feil ved ruting. Url=[$url], HttpStatus=[${response.statusCode}], Response=$responseBody")
        }
    }

    private fun FagsakYtelseType.somSøknadstype() = when (this) {
        FagsakYtelseType.PLEIEPENGER_SYKT_BARN -> "PleiepengerSyktBarn"
        FagsakYtelseType.OMSORGSPENGER -> "Omsorgspenger"
        else -> throw IllegalArgumentException("Støtter ikke ytelse ${this.navn}")
    }


    private suspend fun punsjbolleSaksnummerDto(
        søker: NorskIdentDto,
        pleietrengende: NorskIdentDto,
        journalpostId: JournalpostIdDto?,
        periode: PeriodeDto?,
        fagsakYtelseType: FagsakYtelseType,
    ): PunsjbolleSaksnummerDto {
        val søkerPerson = personService.finnEllerOpprettPersonVedNorskIdent(søker)
        val pleietrengendePerson = personService.finnEllerOpprettPersonVedNorskIdent(pleietrengende)
        return PunsjbolleSaksnummerDto(
            søker = PunsjbollePersonDto(søkerPerson.norskIdent, søkerPerson.aktørId),
            pleietrengende = PunsjbollePersonDto(pleietrengendePerson.norskIdent, pleietrengendePerson.aktørId),
            periode = periode?.let {
                require(it.fom != null || it.tom != null) { "Må sette enten fom eller tom" }
                "${it.fom.iso8601()}/${it.tom.iso8601()}"
            },
            søknadstype = fagsakYtelseType.somSøknadstype(),
            journalpostId = journalpostId
        )
    }

    private suspend fun punsjbolleSaksnummerFraSøknadDto(
        søker: NorskIdentDto,
        pleietrengende: NorskIdentDto,
        søknad: Søknad,
    ): PunsjbolleSaksnummerFraSøknadDto {
        val søkerPerson = personService.finnEllerOpprettPersonVedNorskIdent(søker)
        val pleietrengendePerson = personService.finnEllerOpprettPersonVedNorskIdent(pleietrengende)
        return PunsjbolleSaksnummerFraSøknadDto(
            søker = PunsjbollePersonDto(søkerPerson.norskIdent, søkerPerson.aktørId),
            pleietrengende = PunsjbollePersonDto(pleietrengendePerson.norskIdent, pleietrengendePerson.aktørId),
            søknad = søknad.somMap()
        )
    }

    private suspend fun String.post(
        requestBody: Any,
        correlationId: CorrelationId,
    ): Triple<URI, Response, String> {
        val url = URI("${baseUrl}/$this")

        val (_, response, result) = "$url"
            .httpPost()
            .body(objectMapper().serialiser(requestBody))
            .header(
                HttpHeaders.ACCEPT to "application/json",
                HttpHeaders.AUTHORIZATION to cachedAccessTokenClient.getAccessToken(setOf(scope))
                    .asAuthoriationHeader(),
                HttpHeaders.CONTENT_TYPE to "application/json",
                NavHeaders.XCorrelationId to correlationId
            ).awaitStringResponseResult()

        val responseBody = result.fold(
            { success -> success },
            { error ->
                when (error.response.body().isEmpty()) {
                    true -> "{}"
                    false -> String(error.response.body().toByteArray())
                }
            }
        )

        return Triple(url, response, responseBody)

    }

    private companion object {
        private fun LocalDate?.iso8601() = when (this) {
            null -> ".."
            else -> "$this"
        }

        inline fun <reified T> ObjectMapper.serialiser(value: T): String = writeValueAsString(value)

        private inline fun <reified T> String.deserialiser() = kotlin.runCatching {
            objectMapper().readValue<T>(this)
        }.fold(
            onSuccess = { it },
            onFailure = {
                throw IllegalStateException("Feil ved deserialisering av Response=$this", it)
            }
        )

        private data class PunsjbollePersonDto(
            val identitetsnummer: String,
            val aktørId: String,
        )

        private data class PunsjbolleSaksnummerDto(
            val søker: PunsjbollePersonDto,
            val pleietrengende: PunsjbollePersonDto,
            val søknadstype: String,
            val journalpostId: String? = null,
            val periode: String? = null,
        ) {
            init {
                require(journalpostId != null || periode != null) {
                    "Må sette minst en av journalpostId og periode"
                }
            }
        }

        private data class PunsjbolleSaksnummerFraSøknadDto(
            val søker: PunsjbollePersonDto,
            val pleietrengende: PunsjbollePersonDto,
            val søknad: Map<String, *>,
        )

        private data class RutingResponse(
            val destinasjon: String? = null,
            val type: String? = null,
        )

        private val log = LoggerFactory.getLogger(RestPunsjbolleService::class.java)
    }
}

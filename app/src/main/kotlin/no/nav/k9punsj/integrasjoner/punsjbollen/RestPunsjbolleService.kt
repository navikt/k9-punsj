package no.nav.k9punsj.integrasjoner.punsjbollen

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.isSuccessful
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpPost
import io.netty.handler.codec.http.HttpResponseStatus
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9.søknad.Søknad
import no.nav.k9punsj.CorrelationId
import no.nav.k9punsj.StandardProfil
import no.nav.k9punsj.felles.NavHeaders
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.felles.IkkeStøttetJournalpost
import no.nav.k9punsj.felles.PunsjbolleRuting
import no.nav.k9punsj.felles.UventetFeil
import no.nav.k9punsj.innsending.InnsendingClient.Companion.somMap
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.felles.dto.PeriodeDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import java.net.URI
import java.time.LocalDate
import java.util.UUID

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
        søker: String,
        pleietrengende: String?,
        annenPart: String?,
        journalpostId: String?,
        periode: PeriodeDto?,
        fagsakYtelseType: FagsakYtelseType,
        correlationId: String
    ): SaksnummerDto {
        val requestBody = punsjbolleSaksnummerDto(
            søker = søker,
            pleietrengende = pleietrengende,
            annenPart = annenPart,
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
        søker: String,
        pleietrengende: String?,
        annenPart: String?,
        søknad: Søknad,
        correlationId: String
    ): SaksnummerDto {
        val requestBody = punsjbolleSaksnummerFraSøknadDto(
            søker = søker,
            pleietrengende = pleietrengende,
            annenPart = annenPart,
            søknad = søknad
        )

        val (url, response, responseBody) = "saksnummer-fra-soknad".post(
            requestBody = requestBody,
            correlationId = correlationId
        )

        check(response.isSuccessful) {
            "Feil ved opprettEllerHentFagsaksnummer. Url=[$url], HttpStatus=[${response.statusCode}], Response=$responseBody"
            if(response.statusCode == HttpStatus.CONFLICT.value()) {
                throw IkkeStøttetJournalpost(responseBody)
            } else {
                throw UventetFeil(responseBody)
            }
        }

        return responseBody.deserialiser()
    }

    override suspend fun ruting(
        søker: String,
        pleietrengende: String?,
        annenPart: String?,
        journalpostId: String?,
        periode: PeriodeDto?,
        fagsakYtelseType: FagsakYtelseType,
        correlationId: String
    ): PunsjbolleRuting {
        val requestBody = punsjbolleSaksnummerDto(
            søker = søker,
            pleietrengende = pleietrengende,
            annenPart = annenPart,
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
        FagsakYtelseType.OMSORGSPENGER_KS -> "OmsorgspengerKroniskSyktBarn"
        FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE -> "PleiepengerLivetsSluttfase"
        FagsakYtelseType.OMSORGSPENGER_MA -> "OmsorgspengerMidlertidigAlene"
        else -> throw IllegalArgumentException("Støtter ikke ytelse ${this.navn}")
    }


    private suspend fun punsjbolleSaksnummerDto(
        søker: String,
        pleietrengende: String?,
        annenPart: String?,
        journalpostId: String?,
        periode: PeriodeDto?,
        fagsakYtelseType: FagsakYtelseType,
    ): PunsjbolleSaksnummerDto {
        val søkerPerson = personService.finnEllerOpprettPersonVedNorskIdent(søker)
        val pleietrengendePerson = if (pleietrengende != null) personService.finnEllerOpprettPersonVedNorskIdent(pleietrengende) else null
        val annenPartPerson = if (annenPart != null) personService.finnEllerOpprettPersonVedNorskIdent(annenPart) else null
        return PunsjbolleSaksnummerDto(
            søker = PunsjbollePersonDto(søkerPerson.norskIdent, søkerPerson.aktørId),
            pleietrengende = if (pleietrengendePerson!= null) PunsjbollePersonDto(pleietrengendePerson.norskIdent, pleietrengendePerson.aktørId) else null,
            annenPart = if (annenPartPerson!= null) PunsjbollePersonDto(annenPartPerson.norskIdent, annenPartPerson.aktørId) else null,
            periode = periode?.let {
                require(it.fom != null || it.tom != null) { "Må sette enten fom eller tom" }
                "${it.fom.iso8601()}/${it.tom.iso8601()}"
            },
            søknadstype = fagsakYtelseType.somSøknadstype(),
            journalpostId = journalpostId
        )
    }

    private suspend fun punsjbolleSaksnummerFraSøknadDto(
        søker: String,
        pleietrengende: String?,
        annenPart: String?,
        søknad: Søknad,
    ): PunsjbolleSaksnummerFraSøknadDto {
        val søkerPerson = personService.finnEllerOpprettPersonVedNorskIdent(søker)
        val pleietrengendePerson = if (pleietrengende != null) personService.finnEllerOpprettPersonVedNorskIdent(pleietrengende) else null
        val annenPartPerson = if (annenPart != null) personService.finnEllerOpprettPersonVedNorskIdent(annenPart) else null
        return PunsjbolleSaksnummerFraSøknadDto(
            søker = PunsjbollePersonDto(søkerPerson.norskIdent, søkerPerson.aktørId),
            pleietrengende = if (pleietrengendePerson!= null) PunsjbollePersonDto(pleietrengendePerson.norskIdent, pleietrengendePerson.aktørId) else null,
            annenPart = if (annenPartPerson!= null) PunsjbollePersonDto(annenPartPerson.norskIdent, annenPartPerson.aktørId) else null,
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
        private val logger = LoggerFactory.getLogger(RestPunsjbolleService::class.java)

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
            val pleietrengende: PunsjbollePersonDto?,
            val annenPart: PunsjbollePersonDto?,
            val søknadstype: String,
            val journalpostId: String? = null,
            val periode: String? = null,
        ) {
            init {
                require(journalpostId != null || periode != null) {
                    "Må sette minst en av journalpostId og periode"
                }

                require(pleietrengende != null || annenPart != null) {
                    "Må sette minst en av pleietrengende og annenPart"
                }
            }
        }

        private data class PunsjbolleSaksnummerFraSøknadDto(
            val søker: PunsjbollePersonDto,
            val pleietrengende: PunsjbollePersonDto?,
            val annenPart: PunsjbollePersonDto?,
            val søknad: Map<String, *>,
        )

        private data class RutingResponse(
            val destinasjon: String? = null,
            val type: String? = null,
        )

        private val log = LoggerFactory.getLogger(RestPunsjbolleService::class.java)
    }
}

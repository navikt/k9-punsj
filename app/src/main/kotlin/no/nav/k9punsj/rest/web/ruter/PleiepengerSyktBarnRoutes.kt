package no.nav.k9punsj.rest.web.ruter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9.søknad.ValideringsFeil
import no.nav.k9punsj.AuthenticationHandler
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.Routes
import no.nav.k9punsj.db.datamodell.*
import no.nav.k9punsj.domenetjenester.MappeService
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.domenetjenester.PleiepengerSyktBarnSoknadService
import no.nav.k9punsj.domenetjenester.mappers.SøknadMapper
import no.nav.k9punsj.rest.web.Innsending
import no.nav.k9punsj.rest.web.dto.MapperSvarDTO
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.*
import java.util.UUID
import kotlin.coroutines.coroutineContext

@Configuration
internal class PleiepengerSyktBarnRoutes(
    private val objectMapper: ObjectMapper,
    private val mappeService: MappeService,
    private val pleiepengerSyktBarnSoknadService: PleiepengerSyktBarnSoknadService,
    private val personService: PersonService,
    private val authenticationHandler: AuthenticationHandler

) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(PleiepengerSyktBarnRoutes::class.java)
        private const val NorskIdentKey = "norsk_ident"
        private const val MappeIdKey = "mappe_id"
        private const val søknadType = FagsakYtelseTypeUri.PLEIEPENGER_SYKT_BARN
    }

    internal object Urls {
        internal const val HenteMapper = "/$søknadType/mapper"
        internal const val NySøknad = "/$søknadType"
        internal const val EksisterendeSøknad = "/$søknadType/mappe/{$MappeIdKey}"
    }

    @Bean
    fun pleiepengerSyktBarnSøknadRoutes() = Routes {

        GET("/api${Urls.HenteMapper}") { request ->
            RequestContext(coroutineContext, request) {
                val personer = personService.finnPersoner(request.norskeIdenter())
                val mapper = mappeService.hentMapper(
                    personIder = personer.map { p -> p.norskIdent }.toSet(),
                    søknadType = FagsakYtelseType.PLEIEPENGER_SYKT_BARN
                ).map { mappe ->
                    mappe.tilDto { personId ->
                        personer.first { p -> p.personId == personId }.norskIdent
                    }
                }

                ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(MapperSvarDTO(mapper))
            }
        }

        GET("/api${Urls.EksisterendeSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                val mappe = mappeService.hent(request.mappeId())
                if (mappe == null) {
                    ServerResponse
                        .notFound()
                        .buildAndAwait()
                } else {
                    val personer = personService.finnPersonerVedPersonId(mappe.personInfo.keys)

                    val mappeDTO = mappe.tilDto { personId: NorskIdent ->
                        val norskIdent =
                            personer.first { p -> p.personId == personId }.norskIdent
                        norskIdent
                    }

                    ServerResponse
                        .ok()
                        .json()
                        .bodyValueAndAwait(mappeDTO)
                }
            }
        }

        PUT("/api${Urls.EksisterendeSøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val innsending = request.innsending()

                val mappe = mappeService.utfyllendeInnsending(
                    mappeId = request.mappeId(),
                    fagsakYtelseType = FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
                    innsending = innsending
                )

                if (mappe == null) {
                    ServerResponse
                        .notFound()
                        .buildAndAwait()
                } else {
                    val personer = personService.finnPersonerVedPersonId(mappe.personInfo.keys)

                    val mappeDTO = mappe.tilDto { personId: NorskIdent ->
                        val norskIdent =
                            personer.first { p -> p.personId == personId }.norskIdent
                        norskIdent
                    }
                    println(mappeDTO)

                    ServerResponse
                        .ok()
                        .json()
                        .bodyValueAndAwait(mappeDTO)
                }
            }
        }

        POST("/api${Urls.EksisterendeSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                val norskIdent = request.norskIdent()
                val mappeId = request.mappeId()
                val mappe = mappeService.hent(mappeId)

                if (mappe == null || norskIdent == null) {
                    ServerResponse
                        .notFound()
                        .buildAndAwait()
                } else {
                    try {
                        val person = mappe.personInfo[norskIdent]
                        val søknad: Søknad = objectMapper.convertValue(person!!.soeknad)
                        val soknad = SøknadMapper.map(søknad.søknadId ?: UUID.randomUUID(), søknad.ytelse!!, norskIdent)
                        pleiepengerSyktBarnSoknadService.sendSøknad(soknad, person.innsendinger)
                        mappeService.fjern(
                            mappeId = mappeId,
                            norskIdent = norskIdent
                        )
                        ServerResponse
                            .accepted()
                            .buildAndAwait()
                    } catch (e: ValideringsFeil) {
                        logger.error("", e)
                        ServerResponse
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .buildAndAwait()
                    }
                }
            }
        }

        POST("/api${Urls.NySøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val innsending = request.innsending()
                val mappe = mappeService.førsteInnsending(
                    innsending = innsending,
                    søknadType = FagsakYtelseType.PLEIEPENGER_SYKT_BARN
                )
                val personer = personService.finnPersonerVedPersonId(mappe.personInfo.keys)

                val mappeDTO = mappe.tilDto { personId: NorskIdent ->
                    return@tilDto personer.first { p -> p.personId == personId }.norskIdent
                }

                return@RequestContext ServerResponse
                    .created(request.mappeLocation(mappe.mappeId))
                    .json()
                    .bodyValueAndAwait(mappeDTO)
            }
        }

        DELETE("/api${Urls.EksisterendeSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                try {
                    mappeService.slett(mappeid = request.mappeId())
                    ServerResponse.noContent().buildAndAwait()
                } catch (e: Exception) {
                    ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).buildAndAwait()
                }
            }
        }
    }


    private suspend fun ServerRequest.mappeId(): MappeId = pathVariable(MappeIdKey)
    private suspend fun ServerRequest.norskIdent(): NorskIdent? =
        if (norskeIdenter().size != 1) null else norskeIdenter().first()

    private suspend fun ServerRequest.norskeIdenter(): Set<NorskIdent> {
        val identer = mutableSetOf<NorskIdent>()
        headers().header("X-Nav-NorskIdent").forEach { it -> identer.addAll(it.split(",").onEach { it.trim() }) }
        return identer.toSet()
    }

    private suspend fun ServerRequest.innsending() = body(BodyExtractors.toMono(Innsending::class.java)).awaitFirst()
    private fun ServerRequest.mappeLocation(mappeId: MappeId) = uriBuilder().pathSegment("mappe", mappeId).build()
}

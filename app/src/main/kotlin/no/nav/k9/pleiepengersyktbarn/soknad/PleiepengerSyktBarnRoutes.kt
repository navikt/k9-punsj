package no.nav.k9.pleiepengersyktbarn.soknad

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9.*
import no.nav.k9.mappe.*
import no.nav.k9.søknad.ValideringsFeil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.*
import java.lang.Exception
import javax.validation.Validator
import kotlin.coroutines.coroutineContext

@Configuration
internal class PleiepengerSyktBarnRoutes(
        private val validator: Validator,
        private val objectMapper: ObjectMapper,
        private val mappeService: MappeService,
        private val pleiepengerSyktBarnSoknadService: PleiepengerSyktBarnSoknadService,
        private val authenticationHandler: AuthenticationHandler,
        private val pleiepengerSyktBarnSoknadConverter: PleiepengerSyktBarnSoknadConverter
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(PleiepengerSyktBarnRoutes::class.java)
        private const val NorskIdentKey = "norsk_ident"
        private const val MappeIdKey = "mappe_id"
        private const val søknadType : SøknadType = "pleiepenger-sykt-barn-soknad"
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
                val mapper = mappeService.hent(
                        norskeIdenter = request.norskeIdenter(),
                        søknadType = søknadType
                ).map { mappe ->
                    mappe.dtoMedValidering(validerFor = request.norskeIdenter())
                }

                ServerResponse
                        .ok()
                        .json()
                        .bodyValueAndAwait(MapperSvarDTO(mapper))
            }
        }

        GET("/api${Urls.EksisterendeSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                val mappeDTO = mappeService.hent(request.mappeId())?.dtoMedValidering()
                if (mappeDTO == null) {
                    ServerResponse
                            .notFound()
                            .buildAndAwait()
                } else {
                    ServerResponse
                            .status(mappeDTO.erKomplett().httpStatus())
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
                        søknadType = søknadType,
                        innsending = innsending
                )

                if (mappe == null) {
                    ServerResponse
                            .notFound()
                            .buildAndAwait()
                } else {
                    val mappeDTO = mappe.dtoMedValidering()
                    ServerResponse
                            .status(mappeDTO.erKomplett().httpStatus())
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
                    val mappeDTO = mappe.dtoMedValidering(validerFor = setOf(norskIdent))

                    when {
                        !mappeDTO.personer.containsKey(norskIdent) -> ServerResponse
                                .notFound()
                                .buildAndAwait()
                        mappeDTO.erKomplett() -> {
                            try {
                                val person = mappe.person[norskIdent]
                                val soknad = pleiepengerSyktBarnSoknadConverter.convert(objectMapper.convertValue(person!!.soeknad), norskIdent)
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
                        else -> ServerResponse
                                .status(HttpStatus.BAD_REQUEST)
                                .json()
                                .bodyValueAndAwait(mappeDTO)
                    }
                }
            }
        }

        POST("/api${Urls.NySøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val innsending = request.innsending()
                val mappe = mappeService.førsteInnsending(
                        innsending = innsending,
                        søknadType = søknadType
                )

                val mappeDTO = mappe.dtoMedValidering()

                ServerResponse
                        .created(request.mappeLocation(mappe.mappeId))
                        .json()
                        .bodyValueAndAwait(mappeDTO)
            }
        }

        DELETE("/api${Urls.EksisterendeSøknad}") {
            request -> RequestContext(coroutineContext, request) {
                try {
                    mappeService.slett(mappeid = request.mappeId())
                    ServerResponse.noContent().buildAndAwait()
                } catch (e: Exception) {
                    ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).buildAndAwait()
                }
            }
        }
    }


    private fun Mappe.dtoMedValidering(validerFor: Set<NorskIdent>? = null) : MappeSvarDTO {
        val personMangler = mutableMapOf<NorskIdent, Set<Mangel>>()
        person.forEach { (norskIdent, Person) ->
            if (validerFor == null || validerFor.contains(norskIdent)) {
                personMangler[norskIdent] = Person.soeknad.valider()
            }
        }
        return dto(
                personMangler = personMangler
        )
    }
    private fun SøknadJson.valider() : Set<Mangel> {
        val søknad : PleiepengerSyktBarnSoknad = objectMapper.convertValue(this)
        return validator.validate(søknad).mangler()
    }

    private suspend fun ServerRequest.mappeId() : MappeId = pathVariable(MappeIdKey)
    private suspend fun ServerRequest.norskIdent() : NorskIdent? = if(norskeIdenter().size != 1) null else norskeIdenter().first()
    private suspend fun ServerRequest.norskeIdenter() : Set<NorskIdent> {
        val identer = mutableSetOf<NorskIdent>()
        headers().header("X-Nav-NorskIdent").forEach { it -> identer.addAll(it.split(",").onEach { it.trim() }) }
        return identer.toSet()
    }
    private suspend fun ServerRequest.innsending() = body(BodyExtractors.toMono(Innsending::class.java)).awaitFirst()
    private fun ServerRequest.mappeLocation(mappeId: MappeId) = uriBuilder().pathSegment("mappe", mappeId).build()
}

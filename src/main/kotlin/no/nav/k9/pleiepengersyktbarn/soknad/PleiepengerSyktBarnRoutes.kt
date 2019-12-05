package no.nav.k9.pleiepengersyktbarn.soknad

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.*
import javax.validation.Validator
import kotlin.coroutines.coroutineContext
import no.nav.k9.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus

@Configuration
internal class PleiepengerSyktBarnRoutes(
        private val validator: Validator,
        private val objectMapper: ObjectMapper,
        private val mappeService: MappeService,
        private val pleiepengerSyktBarnSoknadService: PleiepengerSyktBarnSoknadService,
        private val authenticationHandler: AuthenticationHandler
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(PleiepengerSyktBarnRoutes::class.java)
        private const val NorskIdentKey = "norsk_ident"
        private const val MappeIdKey = "mappe_id"
        private const val innholdType : InnholdType = "pleiepenger-sykt-barn-soknad"
    }

    internal object Urls {
        internal const val HenteMapper = "/$innholdType/mapper"
        internal const val NySøknad = "/$innholdType"
        internal const val EksisterendeSøknad = "/$innholdType/mappe/{$MappeIdKey}"
    }

    @Bean
    fun pleiepengerSyktBarnSøknadRoutes() = Routes {

        GET("/api${Urls.HenteMapper}") { request ->
            RequestContext(coroutineContext, request) {
                val mapper = mappeService.hent(
                        norskeIdenter = request.norskeIdenter(),
                        innholdType = innholdType
                ).map { mappe ->
                    mappe.dtoMedValidering(validerFor = request.norskeIdenter())
                }

                ServerResponse
                        .ok()
                        .json()
                        .bodyValueAndAwait(MapperDTO(mapper))
            }
        }

        PUT("/api${Urls.EksisterendeSøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val innsending = request.innsending()

                val mappe = mappeService.utfyllendeInnsending(
                        mappeId = request.mappeId(),
                        innholdType = innholdType,
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

        POST("/api${Urls.EksisterendeSøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
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

                    if (mappeDTO.erKomplett()) {
                        pleiepengerSyktBarnSoknadService.sendSøknad(
                                norskIdent = norskIdent,
                                mappe = mappe
                        )
                        mappeService.fjern(
                                mappeId = mappeId,
                                norskIdent = norskIdent
                        )
                        ServerResponse
                                .accepted()
                                .buildAndAwait()
                    } else {
                        ServerResponse
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
                        innholdType = innholdType
                )

                val mappeDTO = mappe.dtoMedValidering()

                ServerResponse
                        .created(request.mappeLocation(mappe.mappeId))
                        .json()
                        .bodyValueAndAwait(mappeDTO)
            }
        }
    }


    private fun Mappe.dtoMedValidering(validerFor: Set<NorskIdent>? = null) : MappeDTO {
        val personligInnholdMangler = mutableMapOf<NorskIdent, Set<Mangel>>()
        personlig.forEach { (norskIdent, undermappe) ->
             if (validerFor == null || validerFor.contains(norskIdent)) {
                 personligInnholdMangler[norskIdent] = undermappe.innhold.validerPersonligdel()
             }
        }
        return dto(
                fellesMangler = felles?.innhold?.validerFellesdel()?: setOf(),
                personligMangler = personligInnholdMangler
        )
    }
    private fun Innhold.validerPersonligdel() : Set<Mangel> {
        val personligDel : PersonligDel = objectMapper.convertValue(this)
        return validator.validate(personligDel).mangler()
    }
    private fun Innhold.validerFellesdel() : Set<Mangel> {
        val fellesDel : FellesDel = objectMapper.convertValue(this)
        return validator.validate(fellesDel).mangler()
    }

    private suspend fun ServerRequest.mappeId() : MappeId = pathVariable(MappeIdKey)
    private suspend fun ServerRequest.norskIdent() : NorskIdent? = if(norskeIdenter().size != 1) null else norskeIdenter().first()
    private suspend fun ServerRequest.norskeIdenter() : Set<NorskIdent> = headers().header("X-Nav-NorskIdent").toSet()
    private suspend fun ServerRequest.innsending() = body(BodyExtractors.toMono(Innsending::class.java)).awaitFirst()
    private fun ServerRequest.mappeLocation(mappeId: MappeId) = uriBuilder().pathSegment("mappe", mappeId).build()
}
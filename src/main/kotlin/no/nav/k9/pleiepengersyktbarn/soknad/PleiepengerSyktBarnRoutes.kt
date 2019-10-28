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

@Configuration
internal class PleiepengerSyktBarnRoutes(
        private val validator: Validator,
        private val objectMapper: ObjectMapper,
        private val mappeService: MappeService,
        private val pleiepengerSyktBarnSoknadService: PleiepengerSyktBarnSoknadService
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(PleiepengerSyktBarnRoutes::class.java)
        private const val NorskIdentKey = "norsk_ident"
        private const val MappeIdKey = "mappe_id"
        private const val innholdType : InnholdType = "pleiepenger-sykt-barn-soknad"
    }

    internal object Urls {
        internal const val HenteMapper = "/$innholdType/mapper/{$NorskIdentKey}"
        internal const val NySøknad = "/$innholdType"
        internal const val OppdaterSøknad = "/$innholdType/mappe/{$MappeIdKey}"
    }

    @Bean
    fun pleiepengerSyktBarnSøknadRoutes() = Routes {

        GET("/api${Urls.HenteMapper}") { request ->
            RequestContext(coroutineContext, request) {
                val mapper = mappeService.hent(
                        norskIdent = request.norskIdent(),
                        innholdType = innholdType
                ).map { mappe ->
                    mappe.dto(mappe.innhold.valider())
                }

                ServerResponse
                        .ok()
                        .json()
                        .bodyValueAndAwait(mapper)
            }
        }

        PUT("/api${Urls.OppdaterSøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val innsendingDTO = request.innsendingDTO()
                val innsending = innsendingDTO.domain()

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
                    val mangler = mappe.innhold.valider()
                    if (mangler.isEmpty() && innsendingDTO.sendTilBehandling) {
                        pleiepengerSyktBarnSoknadService.komplettSøknadMedMappe(mappe)
                        mappeService.fjern(
                                mappeId = mappe.mappeId,
                                norskIdent = innsending.norskIdent,
                                innholdType = innholdType
                        )
                        ServerResponse
                                .accepted()
                                .buildAndAwait()
                    } else {
                        ServerResponse
                                .status(mangler.httpStatus())
                                .json()
                                .bodyValueAndAwait(mappe.dto(mangler))
                    }

                }

            }
        }

        POST("/api${Urls.NySøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val innsendingDTO = request.innsendingDTO()
                val innsending = innsendingDTO.domain()
                val mangler = innsending.innhold.valider()

                if (mangler.isEmpty() && innsendingDTO.sendTilBehandling) {
                    pleiepengerSyktBarnSoknadService.komplettSøknadMedEnInnsending(innsending)
                    ServerResponse
                            .accepted()
                            .buildAndAwait()
                } else {
                    val mappe = mappeService.førsteInnsending(
                            innsending = innsending,
                            innholdType = innholdType
                    )

                    ServerResponse
                            .status(mangler.httpStatus())
                            .json()
                            .bodyValueAndAwait(mappe.dto(mangler))
                }
            }
        }
    }

    private fun Innhold.valider() : Set<Mangel> {
        val søknad : Søknad = objectMapper.convertValue(this)
        return validator.validate(søknad).mangler()
    }
    private suspend fun ServerRequest.mappeId() : MappeId = pathVariable(MappeIdKey)
    private suspend fun ServerRequest.norskIdent() : NorskIdent = pathVariable(NorskIdentKey)
    private suspend fun ServerRequest.innsendingDTO() = body(BodyExtractors.toMono(InnsendingDTO::class.java)).awaitFirst()
}
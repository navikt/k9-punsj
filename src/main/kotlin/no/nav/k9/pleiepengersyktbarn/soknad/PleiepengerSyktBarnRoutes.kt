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
        private val innholdType : InnholdType = "pleiepenger-sykt-barn-soknad"
    }

    @Bean
    fun pleiepengerSyktBarnSøknadRoutes() = Routes {

        GET("/api/$innholdType/mapper/{$NorskIdentKey}") { request ->
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

        PUT("/api/$innholdType/mappe/{$MappeIdKey}", contentType(MediaType.APPLICATION_JSON)) { request ->
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
                    val mangler = mappe.innhold.valider()
                    if (mangler.isEmpty()) {
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
                                .badRequest()
                                .json()
                                .bodyValueAndAwait(mappe.dto(mangler))
                    }

                }

            }
        }

        POST("/api/$innholdType", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val innsending = request.innsending()
                val mangler = innsending.innhold.valider()

                if (mangler.isEmpty()) {
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
                            .badRequest()
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
    private suspend fun ServerRequest.innsending() = body(BodyExtractors.toMono(Innsending::class.java)).awaitFirst()
}
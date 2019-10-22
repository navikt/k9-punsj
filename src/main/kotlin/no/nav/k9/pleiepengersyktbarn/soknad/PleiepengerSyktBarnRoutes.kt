package no.nav.k9.pleiepengersyktbarn.soknad

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.*
import javax.validation.ConstraintViolation
import javax.validation.Validator
import kotlin.coroutines.coroutineContext
import no.nav.k9.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory


@Configuration
internal class PleiepengerSyktBarnRoutes(
        private val validator: Validator,
        private val objectMapper: ObjectMapper,
        private val service: PleiepengerSyktBarnSoknadService
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(PleiepengerSyktBarnRoutes::class.java)
        private const val SøknadBasePath = "/api/pleiepenger-sykt-barn/soknad/{journalPostId}"
    }

    @Bean
    fun søknadRoutes() = Routes {
        PUT(SøknadBasePath, contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                logger.info("Mellomlagrer søknader.")
                val journalPostId = request.journalPostId()
                val innsending = request.innsending()
                val oppdatertInnsending = service.oppdater(journalPostId, innsending)
                val violations = oppdatertInnsending.valider()

                val httpStatus = if (violations.isEmpty()) HttpStatus.OK else HttpStatus.BAD_REQUEST

                ServerResponse
                        .status(httpStatus)
                        .json()
                        .bodyValueAndAwait(MellomlagringsResultat(
                                innhold = innsending.innhold,
                                violations = violations
                        ))
            }
        }

        POST(SøknadBasePath, contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                logger.info("Sender inn søknader.")
                val journalPostId = request.journalPostId()
                val innsending = request.innsending()
                val violations = innsending.valider()
                if (violations.isEmpty()) {
                    service.sendUkompletteSøknader(journalPostId, innsending)
                } else {
                    service.sendKompletteSøknader(journalPostId, innsending)
                }
                ServerResponse
                        .accepted()
                        .buildAndAwait()
            }
        }

        GET(SøknadBasePath) { request ->
            RequestContext(coroutineContext, request) {
                logger.info("Henter søknader.")
                val journalPostId = request.journalPostId()
                val innsending = service.hent(journalPostId)
                if (innsending != null) {
                    ServerResponse
                            .ok()
                            .json()
                            .bodyValueAndAwait(innsending)
                } else {
                    ServerResponse
                            .notFound()
                            .buildAndAwait()
                }
            }
        }
    }

    private fun Innsending.valider() : Set<ConstraintViolation<Søknader>> {
        val søknader : Søknader = objectMapper.convertValue(this)
        return validator.validate(søknader)
    }

    private suspend fun ServerRequest.journalPostId() : JournalpostId = pathVariable("journalPostId")
    private suspend fun ServerRequest.innsending() = body(BodyExtractors.toMono(Innsending::class.java)).awaitFirst()
}
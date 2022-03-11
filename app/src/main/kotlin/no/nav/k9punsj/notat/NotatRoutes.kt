package no.nav.k9punsj.notat

import no.nav.k9punsj.AuthenticationHandler
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.rest.web.nyNotat
import no.nav.k9punsj.rest.web.openapi.OasFeil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.json
import kotlin.coroutines.coroutineContext

@Configuration
internal class NotatRoutes(
    private val authenticationHandler: AuthenticationHandler,
    private val notatService: NotatService,
    @Value("\${NOTAT_ENABLED}") private val notatEnabled: Boolean
) {

    internal companion object {
        private val logger: Logger = LoggerFactory.getLogger(NotatRoutes::class.java)
    }

    internal object Urls {
        internal const val OpprettNotat = "/notat/opprett"
    }

    @Bean
    fun NotatRoutes() = SaksbehandlerRoutes(authenticationHandler) {
        if (notatEnabled) {
            POST("/api${Urls.OpprettNotat}") { request ->
                RequestContext(coroutineContext, request) {
                    val nyNotat = request.nyNotat()

                    return@RequestContext kotlin.runCatching {
                        logger.info("Oppretter notat...")
                        notatService.opprettNotat(nyNotat)

                    }
                }.fold(
                    onSuccess = {
                        logger.info("Notat opprettet.")
                        ServerResponse
                            .status(HttpStatus.CREATED)
                            .json()
                            .bodyValueAndAwait(it)
                    },
                    onFailure = {
                        logger.error("Feilet med å opprette notat.", it)
                        ServerResponse
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .json()
                            .bodyValueAndAwait(OasFeil(it.message))
                    }
                )
            }
        } else {
            throw NotImplementedError("Ikke aktivert")
        }
    }
}
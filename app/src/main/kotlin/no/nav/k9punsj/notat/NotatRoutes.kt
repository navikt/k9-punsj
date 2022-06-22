package no.nav.k9punsj.notat

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.openapi.OasFeil
import no.nav.k9punsj.tilgangskontroll.AuthenticationHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
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

        internal suspend fun ServerRequest.nyNotat() =
            body(BodyExtractors.toMono(NyNotat::class.java)).awaitFirst()
    }

    internal object Urls {
        internal const val OpprettNotat = "/notat/opprett"
    }

    @Bean
    fun NotatRoutes() = SaksbehandlerRoutes(authenticationHandler) {
        POST("/api${Urls.OpprettNotat}") { request ->
            if (notatEnabled) {
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
            } else {
                ServerResponse
                    .status(HttpStatus.NOT_IMPLEMENTED)
                    .json()
                    .bodyValueAndAwait("Ikke enablet")
            }
        }
    }
}

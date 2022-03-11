package no.nav.k9punsj.sak

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9punsj.AuthenticationHandler
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.rest.web.openapi.OasFeil
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
internal class SakerRoutes(
    private val authenticationHandler: AuthenticationHandler,
    private val sakService: SakService,
    @Value("\${SAKER_ENABLED}") private val sakerEnabled: Boolean
) {

    internal companion object {
        private val logger: Logger = LoggerFactory.getLogger(SakerRoutes::class.java)

        internal suspend fun ServerRequest.hentSakerRequest() =
            body(BodyExtractors.toMono(HentSakerRequest::class.java)).awaitFirst()
    }

    internal object Urls {
        internal const val HentSaker = "/saker/hent"
    }

    data class HentSakerRequest(val søkerIdent: String)

    @Bean
    fun SakerRoutes() = SaksbehandlerRoutes(authenticationHandler) {
        if (sakerEnabled) {
            POST("/api${Urls.HentSaker}") { request ->
                RequestContext(coroutineContext, request) {
                    val sakerRequest = request.hentSakerRequest()

                    return@RequestContext kotlin.runCatching {
                        logger.info("Henter fagsaker...")
                        sakService.hentSaker(sakerRequest.søkerIdent)

                    }
                }.fold(
                    onSuccess = {
                        logger.info("Saker hentet")
                        ServerResponse
                            .status(HttpStatus.OK)
                            .json()
                            .bodyValueAndAwait(it)
                    },
                    onFailure = {
                        logger.error("Feilet med å hente saker.", it)
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

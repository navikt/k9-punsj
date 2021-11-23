package no.nav.k9punsj.rest.web.ruter

import no.nav.k9punsj.AuthenticationHandler
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.brev.BrevServiceImpl
import no.nav.k9punsj.brev.BrevType
import no.nav.k9punsj.rest.web.brevBestilling
import no.nav.k9punsj.rest.web.openapi.OasFeil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.json
import kotlin.coroutines.coroutineContext

@Configuration
internal class BrevRoutes(
    private val authenticationHandler: AuthenticationHandler,
    private val brevService: BrevServiceImpl,
) {


    private companion object {
        private const val JournalpostIdKey = "journalpost_id"
        private val logger: Logger = LoggerFactory.getLogger(BrevRoutes::class.java)
    }

    internal object Urls {
        internal const val BestillBrev = "/brev/bestill" //post
        internal const val HentAlleBrev = "/brev/hentAlle/{$JournalpostIdKey}" //get
    }

    @Bean
    fun BrevRoutes() = SaksbehandlerRoutes(authenticationHandler) {
        GET("/api${Urls.HentAlleBrev}") { request ->
            RequestContext(coroutineContext, request) {
                return@RequestContext ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait("")
            }
        }

        POST("/api${Urls.BestillBrev}") { request ->
            RequestContext(coroutineContext, request) {
                val bestilling = kotlin.runCatching { request.brevBestilling() }
                    .getOrElse {
                        return@RequestContext ServerResponse
                            .badRequest()
                            .json()
                            .bodyValueAndAwait(OasFeil(it.message!!))
                    }
                val brevEntitet = kotlin.runCatching {
                    brevService.bestillBrev(
                        bestilling.journalpostId,
                        bestilling,
                        BrevType.FRITEKSTBREV)
                }
                    .getOrElse {
                        return@RequestContext ServerResponse
                            .badRequest()
                            .json()
                            .bodyValueAndAwait(OasFeil(it.message!!))
                    }
                return@RequestContext ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(brevEntitet)
            }
        }
    }
}

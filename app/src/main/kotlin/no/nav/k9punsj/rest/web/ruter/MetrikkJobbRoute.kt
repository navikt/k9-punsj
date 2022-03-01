package no.nav.k9punsj.rest.web.ruter

import no.nav.k9punsj.PublicRoutes
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.jobber.MetrikkJobb
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import kotlin.coroutines.coroutineContext

@Configuration
internal class MetrikkJobbRoute(
    private val metrikkJobb: MetrikkJobb
) {

    @Bean
    fun restRoute() = PublicRoutes {
        GET("/internal/metrikk/jobb") { request ->
            RequestContext(coroutineContext, request) {
                metrikkJobb.oppdaterMetrikkMåling()
                return@RequestContext ServerResponse
                    .ok()
                    .bodyValueAndAwait("Kjører metrikkjobb...")
            }
        }
    }
}

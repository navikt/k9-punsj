package no.nav.k9.mappe

import no.nav.k9.AuthenticationHandler
import no.nav.k9.NorskIdent
import no.nav.k9.RequestContext
import no.nav.k9.Routes
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.json
import kotlin.coroutines.coroutineContext

@Configuration
internal class MappeRoutes(
    private val mappeService: MappeService,
    private val authenticationHandler: AuthenticationHandler,
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(MappeRoutes::class.java)
    }

    @Bean
    fun MappeRoutes() = Routes(authenticationHandler) {

        GET("/api/mapper") { request ->
            RequestContext(coroutineContext, request) {
                val mapper = mappeService.finnMapper(
                    norskeIdenter = request.norskeIdenter()
                ).map { mappe ->
                    mappe.dtoUtenMangler()
                }

                ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(MapperSvarDTO(mapper))
            }
        }
    }


    private suspend fun ServerRequest.norskeIdenter() : Set<NorskIdent> {
        val identer = mutableSetOf<NorskIdent>()
        headers().header("X-Nav-NorskIdent").forEach { it -> identer.addAll(it.split(",").onEach { it.trim() }) }
        return identer.toSet()
    }
}

package no.nav.k9punsj.forvaltning

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.domenetjenester.MappeService
import no.nav.k9punsj.tilgangskontroll.AuthenticationHandler
import org.springdoc.core.annotations.RouterOperation
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.buildAndAwait
import kotlin.coroutines.coroutineContext

@Configuration
internal class ForvaltningRoutes(
    private val authenticationHandler: AuthenticationHandler,
    private val mappeService: MappeService,
) {

    internal object Urls {
        private const val basePath = "/forvaltning"
        internal const val SlettMappeMedAlleTilkoblinger = "$basePath/mappe/slett-alt"
    }

    @Bean
    @RouterOperation(
        operation = Operation(
            operationId = "Sletter alle mapper med tilknyttede søknader og bunker",
            summary = "Sletter alle mapper med tilknyttede søknader og bunker",
            tags = ["Forvaltning"],
            responses = [
                ApiResponse(responseCode = "200", description = "Alle mapper med bunker og søknader er slettet"),
                ApiResponse(responseCode = "500", description = "Noe gikk galt")
            ]
        )
    )
    fun ForvaltningRoutes() = SaksbehandlerRoutes(authenticationHandler) {
        POST("/api${Urls.SlettMappeMedAlleTilkoblinger}") { request ->
            RequestContext(coroutineContext, request) {
                mappeService.slettMappeMedAlleKoblinger()
                return@RequestContext ServerResponse.ok().buildAndAwait()
            }
        }
    }
}

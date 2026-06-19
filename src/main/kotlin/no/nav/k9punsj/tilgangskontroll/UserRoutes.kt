package no.nav.k9punsj.tilgangskontroll

import kotlinx.coroutines.currentCoroutineContext
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.idToken
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait

internal data class UserDto(
    val name: String,
    val erSaksbehandler: Boolean,
    val erVeileder: Boolean,
    val harBasistilgang: Boolean,
    val harHistoriskTilgang: Boolean,
)

@Configuration
internal class UserRoutes(
    private val authenticationHandler: AuthenticationHandler,
) {
    @Bean
    fun UserRoutes() = SaksbehandlerRoutes(authenticationHandler) {
        GET("/api/user") { request ->
            RequestContext(currentCoroutineContext(), request) {
                val token = coroutineContext.idToken()
                ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValueAndAwait(
                        UserDto(
                            name = token.getName(),
                            erSaksbehandler = token.erSaksbehandler(),
                            erVeileder = token.erVeileder(),
                            harBasistilgang = token.harBasistilgang(),
                            harHistoriskTilgang = token.harHistoriskTilgang(),
                        )
                    )
            }
        }
    }
}

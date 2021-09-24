package no.nav.k9punsj.arbeidsgivere

import no.nav.k9punsj.AuthenticationHandler
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.*
import java.time.LocalDate
import java.time.ZoneId
import kotlin.coroutines.coroutineContext

@Configuration
internal class ArbeidsgiverRoutes(
    private val authenticationHandler: AuthenticationHandler,
    private val arbeidsgiverService: ArbeidsgiverService,
    @Value("\${ENABLE_ARBEIDSGIVER_APIS}") private val enabled: Boolean) {

    @Bean
    fun hentArbeidsgivereRoute() = SaksbehandlerRoutes(authenticationHandler) {
        GET("/api/arbeidsgivere") { request ->
            RequestContext(coroutineContext, request) {
                when (enabled) {
                    true -> {
                        // TODO: Legge til abac-sjekk
                        ServerResponse
                            .status(HttpStatus.OK)
                            .json()
                            .bodyValueAndAwait(arbeidsgiverService.hentArbeidsgivere(
                                identitetsnummer = request.identitetsnummer(),
                                fom = request.fom(),
                                tom = request.tom()
                            ))
                    }
                    false -> ServerResponse
                        .status(HttpStatus.NOT_IMPLEMENTED)
                        .buildAndAwait()
                }
            }
        }
    }

    private companion object {
        private val Oslo = ZoneId.of("Europe/Oslo")
        private fun ServerRequest.fom() = queryParamOrNull("fom")
            ?.let { LocalDate.parse(it) }
            ?: LocalDate.now(Oslo).minusMonths(6)

        private fun ServerRequest.tom() = queryParamOrNull("tom")
            ?.let { LocalDate.parse(it) }
            ?: LocalDate.now(Oslo).plusMonths(6)

        private fun ServerRequest.identitetsnummer(): String {
            return requireNotNull(headers().header("X-Nav-NorskIdent").firstOrNull()) {
                "Mangler identitetsnummer"
            }.also { require(it.matches("\\d{11,20}".toRegex())) {
                "Ugyldig identitetsnummer"
            }}
        }
    }
}
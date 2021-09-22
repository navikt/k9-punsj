package no.nav.k9punsj.arbeidsgivere

import no.nav.k9punsj.AuthenticationHandler
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
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
    private val arbeidsgiverService: ArbeidsgiverService) {

    @Bean
    fun hentArbeidsgivereRoute() = SaksbehandlerRoutes(authenticationHandler) {
        GET("/api/arbeidsgivere") { request ->
            RequestContext(coroutineContext, request) {
                ServerResponse
                    .status(HttpStatus.OK)
                    .json()
                    .bodyValueAndAwait(ArbeidsgivereResponse(
                        arbeidsgivere = arbeidsgiverService.hentArbeidsgivere(
                            identitetsnummer = request.identitetsnummer(),
                            fom = request.fom(),
                            tom = request.tom()
                        )
                    ))
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
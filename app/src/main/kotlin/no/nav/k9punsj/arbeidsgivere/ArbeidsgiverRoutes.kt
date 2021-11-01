package no.nav.k9punsj.arbeidsgivere

import no.nav.k9punsj.AuthenticationHandler
import no.nav.k9punsj.PublicRoutes
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.abac.IPepClient
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
    private val pepClient: IPepClient) {

    suspend fun String.harTilgang() =
        pepClient.harBasisTilgang(this, ArbeidsgiverePath)

    @Bean
    fun hentArbeidsgivereRoute() = SaksbehandlerRoutes(authenticationHandler) {
        GET(ArbeidsgiverePath) { request ->
            RequestContext(coroutineContext, request) {
                if (request.identitetsnummer().harTilgang()) {
                    ServerResponse
                        .status(HttpStatus.OK)
                        .json()
                        .bodyValueAndAwait(arbeidsgiverService.hentArbeidsgivere(
                            identitetsnummer = request.identitetsnummer(),
                            fom = request.fom(),
                            tom = request.tom()
                        ))
                } else {
                    ServerResponse
                        .status(HttpStatus.FORBIDDEN)
                        .buildAndAwait()
                }
            }
        }
    }

    @Bean
    fun hentArbeidsgivereMedIdRoute() = SaksbehandlerRoutes(authenticationHandler) {
        GET(ArbeidsgivereMedIdPath) { request ->
            RequestContext(coroutineContext, request) {
                if (request.identitetsnummer().harTilgang()) {
                    ServerResponse
                        .status(HttpStatus.OK)
                        .json()
                        .bodyValueAndAwait(arbeidsgiverService.hentArbeidsgivereMedId(
                            identitetsnummer = request.identitetsnummer(),
                            fom = request.fom(),
                            tom = request.tom()
                        ))
                } else {
                    ServerResponse
                        .status(HttpStatus.FORBIDDEN)
                        .buildAndAwait()
                }
            }
        }
    }

    @Bean
    fun hentArbeidsgiverInfoRoute() = PublicRoutes {
        GET("/api/arbeidsgiver") { request ->
            RequestContext(coroutineContext, request) {
                when (val navn = arbeidsgiverService.hentOrganisasjonsnavn(request.organisasjonsnummer())) {
                    null -> ServerResponse.status(HttpStatus.NOT_FOUND).buildAndAwait()
                    else -> ServerResponse.status(HttpStatus.OK).json().bodyValueAndAwait("""{"navn":"$navn"}""")
                }
            }
        }
    }

    private companion object {
        private const val ArbeidsgiverePath = "/api/arbeidsgivere"
        private const val ArbeidsgivereMedIdPath = "/api/arbeidsgivere-med-id"
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
            }.also { require(it.matches("\\d{11}".toRegex())) {
                "Ugyldig identitetsnummer"
            }}
        }

        private fun ServerRequest.organisasjonsnummer() =
            requireNotNull(queryParamOrNull("organisasjonsnummer")) {
                "Mangler organisasjonsnummer"
            }.also { require(it.matches("\\d{9}".toRegex())) {
                "Ugyldig organisasjonsnummer"
            }}
    }
}

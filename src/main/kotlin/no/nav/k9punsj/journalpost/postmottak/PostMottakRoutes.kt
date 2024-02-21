package no.nav.k9punsj.journalpost.postmottak

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.openapi.OasFeil
import no.nav.k9punsj.tilgangskontroll.AuthenticationHandler
import no.nav.k9punsj.tilgangskontroll.InnloggetUtils
import no.nav.k9punsj.utils.ServerRequestUtils.hentNorskIdentHeader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import kotlin.coroutines.coroutineContext

@Configuration
internal class PostMottakRoutes(
    private val authenticationHandler: AuthenticationHandler,
    private val postMottakService: PostMottakService,
    private val innlogget: InnloggetUtils,
) {

    internal object Urls {
        internal const val Mottak = "/journalpost/mottak"
    }

    @Bean
    fun PostMottakRoutes() = SaksbehandlerRoutes(authenticationHandler) {
        POST("/api${Urls.Mottak}") { request ->
            RequestContext(coroutineContext, request) {
                val norskIdent = request.hentNorskIdentHeader()
                innlogget.harInnloggetBrukerTilgangTilOgSendeInn(
                    norskIdent = norskIdent,
                    url = Urls.Mottak
                )?.let { return@RequestContext it }

                val dto = request.body(BodyExtractors.toMono(JournalpostMottaksHaandteringDto::class.java)).awaitFirst()

                val (saksnummerDto, feil) = postMottakService.klassifiserOgJournalfør(dto)
                if (feil != null) {
                    return@RequestContext ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .bodyValueAndAwait(OasFeil(feil))
                }
                requireNotNull(saksnummerDto) { "Saksnummer er null" }
                return@RequestContext ServerResponse.ok().bodyValueAndAwait(saksnummerDto)
            }
        }
    }
}

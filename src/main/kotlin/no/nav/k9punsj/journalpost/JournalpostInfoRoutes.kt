package no.nav.k9punsj.journalpost

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9.sak.kontrakt.dokument.JournalpostIdDto
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.journalpost.dto.JournalpostIderDto
import no.nav.k9punsj.journalpost.dto.SøkUferdigJournalposter
import no.nav.k9punsj.tilgangskontroll.AuthenticationHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.json
import kotlin.coroutines.coroutineContext

@Configuration
internal class JournalpostInfoRoutes(
    private val authenticationHandler: AuthenticationHandler,
    private val journalpostService: JournalpostService
) {

    internal object Urls {
        internal const val HentÅpneJournalposterPost = "/journalpost/uferdig"
    }

    @Bean
    fun JournalpostInfoRoutes() = SaksbehandlerRoutes(authenticationHandler) {
        POST("/api${Urls.HentÅpneJournalposterPost}") { request ->
            RequestContext(coroutineContext, request) {
                val dto = request.søkUferdigJournalposter()
                val journalpostIder = journalpostService.finnJournalposterPåPersonBareFraFordel(dto.aktorIdentDto)
                    .map { journalpost -> JournalpostIdDto(journalpost.journalpostId) }

                val journalpostPåBarnet = dto.aktorIdentBarnDto?.let {
                    journalpostService.finnJournalposterPåPersonBareFraFordel(it)
                        .map { journalpost -> JournalpostIdDto(journalpost.journalpostId) }
                }.orEmpty()

                return@RequestContext ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(JournalpostIderDto(journalpostIder, journalpostPåBarnet))
            }
        }
    }

    private suspend fun ServerRequest.søkUferdigJournalposter() =
        body(BodyExtractors.toMono(SøkUferdigJournalposter::class.java)).awaitFirst()

}

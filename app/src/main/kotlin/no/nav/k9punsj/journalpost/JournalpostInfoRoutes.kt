package no.nav.k9punsj.journalpost

import no.nav.k9.sak.kontrakt.dokument.JournalpostIdDto
import no.nav.k9punsj.AuthenticationHandler
import no.nav.k9punsj.K9SakRoutes
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.rest.web.dto.AktørIdDto
import no.nav.k9punsj.rest.web.søkUferdigJournalposter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.json
import kotlin.coroutines.coroutineContext

@Configuration
internal class JournalpostInfoRoutes(
    private val authenticationHandler: AuthenticationHandler,
    private val journalpostService: JournalpostService,
) {

    private companion object {
        private const val AktørIdKey = "aktor_id"
    }

    internal object Urls {
        internal const val HentÅpneJournalposter = "/journalpost/uferdig/{$AktørIdKey}"
        internal const val HentÅpneJournalposterPost = "/journalpost/uferdig"
    }

    @Bean
    fun JournalpostInfoRoutes() = K9SakRoutes(authenticationHandler) {
        GET("/api${Urls.HentÅpneJournalposter}") { request ->
            RequestContext(coroutineContext, request) {
                val aktørId = request.aktørId()
                val journalpostIder = journalpostService.finnJournalposterPåPersonBareFraFordel(aktørId)
                    .map { journalpost -> JournalpostIdDto(journalpost.journalpostId) }

             return@RequestContext ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(JournalpostIderDto(journalpostIder))
            }
        }

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

    private fun ServerRequest.aktørId(): AktørIdDto = pathVariable(AktørIdKey)

    data class JournalpostIderDto(
        val journalpostIder: List<JournalpostIdDto>,
        val journalpostIderBarn: List<JournalpostIdDto> = emptyList()
    )
}

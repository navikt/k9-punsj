package no.nav.k9punsj.rest.server

import no.nav.k9punsj.AuthenticationHandler
import no.nav.k9punsj.K9SakRoutes
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.journalpost.JournalpostService
import no.nav.k9punsj.rest.web.dto.AktørIdDto
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
    private val journalpostService: JournalpostService
) {

    private companion object {
        private const val AktørIdKey = "aktor_id"
    }

    internal object Urls {
        internal const val HentÅpneJournalposter = "/journalpost/uferdig/{${AktørIdKey}}"
    }

    @Bean
    fun JournalpostInfoRoutes() = K9SakRoutes(authenticationHandler) {
        GET("/api${Urls.HentÅpneJournalposter}") { request ->
            RequestContext(coroutineContext, request) {
                val aktørId = request.aktørId()
                val journalpostIder = journalpostService.finnJournalposterPåPerson(aktørId)
                    .map { journalpost -> journalpost.journalpostId }

                return@RequestContext ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(journalpostIder)
            }
        }
    }
    private fun ServerRequest.aktørId(): AktørIdDto = pathVariable(AktørIdKey)
}

package no.nav.k9.journalpost

import no.nav.k9.Routes
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.json

@Configuration
internal class JournalpostRoutes(
        @Value("classpath:dummy_soknad.pdf") dummySoknad: Resource
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(JournalpostRoutes::class.java)
        private const val JournalpostIdKey = "journalpost_id"
        private const val DokumentIdKey = "dokument_id"
    }

    private val dummyPdfContent = dummySoknad.inputStream.readAllBytes()

    internal object Urls {
        internal const val HenteJournalpostInfo = "/journalpost/{$JournalpostIdKey}"
        internal const val HenteDokument = "/journalpost/{$JournalpostIdKey}/dokument/{$DokumentIdKey}"
    }

    @Bean
    fun JournalpostRoutes() = Routes {

        GET("/api${Urls.HenteJournalpostInfo}") { request ->
            ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait("""
                        {
                            "dokumenter": [{
                                "dokument_id": "123"
                            }]
                        }
                    """.trimIndent())
        }

        GET("/api${Urls.HenteDokument}") { request ->
            ServerResponse
                    .ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .bodyValueAndAwait(dummyPdfContent)
        }
    }
}
package no.nav.k9punsj.journalpost

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9punsj.AuthenticationHandler
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.Routes
import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import no.nav.k9punsj.rest.eksternt.pdl.PdlService
import no.nav.k9punsj.rest.web.JournalpostId
import no.nav.k9punsj.rest.web.dto.IdentDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.*
import kotlin.coroutines.coroutineContext


@Configuration
internal class JournalpostRoutes(
    private val authenticationHandler: AuthenticationHandler,
    private val journalpostService: JournalpostService,
    private val pdlService: PdlService,
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(JournalpostRoutes::class.java)
        private const val JournalpostIdKey = "journalpost_id"
        private const val DokumentIdKey = "dokument_id"
    }

    internal object Urls {
        internal const val JournalpostInfo = "/journalpost/{$JournalpostIdKey}"
        internal const val OmfordelJournalpost = "$JournalpostInfo/omfordel"
        internal const val Dokument = "/journalpost/{$JournalpostIdKey}/dokument/{$DokumentIdKey}"
        internal const val HentJournalposter = "/journalpost/hent"
    }

    @Bean
    fun JournalpostRoutes() = Routes(authenticationHandler) {

        GET("/api${Urls.JournalpostInfo}") { request ->
            RequestContext(coroutineContext, request) {
                try {
                    val journalpostInfo = journalpostService.hentJournalpostInfo(
                        journalpostId = request.journalpostId()
                    )
                    if (journalpostInfo == null) {
                        ServerResponse
                            .notFound()
                            .buildAndAwait()
                    } else {
                        if (journalpostInfo.norskIdent == null && journalpostInfo.aktørId != null) {
                            val pdlResponse = pdlService.identifikatorMedAktørId(journalpostInfo.aktørId)
                            val personIdent = pdlResponse?.identPdl?.data?.hentIdenter?.identer?.first()?.ident
                            val journalpostInfoDto = JournalpostInfoDto(
                                journalpostId = journalpostInfo.journalpostId,
                                norskIdent = personIdent,
                                dokumenter = journalpostInfo.dokumenter)
                            ServerResponse
                                .ok()
                                .json()
                                .bodyValueAndAwait(journalpostInfoDto)
                        } else {
                            ServerResponse
                                .ok()
                                .json()
                                .bodyValueAndAwait(JournalpostInfoDto(journalpostId = journalpostInfo.journalpostId,
                                    norskIdent = journalpostInfo.norskIdent,
                                    dokumenter = journalpostInfo.dokumenter))
                        }
                    }

                } catch (cause: IkkeStøttetJournalpost) {
                    ServerResponse
                        .badRequest()
                        .buildAndAwait()
                } catch (case: IkkeTilgang) {
                    ServerResponse
                        .status(HttpStatus.FORBIDDEN)
                        .buildAndAwait()
                }
            }
        }

        POST("/api${Urls.HentJournalposter}") { request ->
            RequestContext(coroutineContext, request) {
                val norskIdent = request.ident()
                val pdlResponse = pdlService.identifikator(norskIdent.norskIdent)
                val aktørId = pdlResponse?.identPdl?.data?.hentIdenter?.identer?.first()?.ident
                    ?: throw IllegalStateException("Fant ikke aktørId i PDL")

                val finnJournalposterPåPerson = journalpostService.finnJournalposterPåPerson(aktørId)

                ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(finnJournalposterPåPerson)
            }
        }

        POST("/api${Urls.OmfordelJournalpost}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val omfordelingRequest = request.omfordelingRequest()
                try {
                    val journalpostInfo = journalpostService.hentJournalpostInfo(
                        journalpostId = request.journalpostId()
                    )
                    if (journalpostInfo == null) {
                        ServerResponse
                            .notFound()
                            .buildAndAwait()
                    } else {
                        journalpostService.omfordelJournalpost(
                            journalpostId = request.journalpostId(),
                            ytelse = FagsakYtelseType.fromKode(omfordelingRequest.fagsakYtelseTypeKode)
                        )
                        ServerResponse
                            .noContent()
                            .buildAndAwait()
                    }

                } catch (cause: IkkeStøttetJournalpost) {
                    ServerResponse
                        .badRequest()
                        .buildAndAwait()
                } catch (case: IkkeTilgang) {
                    ServerResponse
                        .status(HttpStatus.FORBIDDEN)
                        .buildAndAwait()
                }
            }
        }

        GET("/api${Urls.Dokument}") { request ->
            RequestContext(coroutineContext, request) {
                try {
                    val dokument = journalpostService.hentDokument(
                        journalpostId = request.journalpostId(),
                        dokumentId = request.dokumentId()
                    )

                    if (dokument == null) {
                        ServerResponse
                            .notFound()
                            .buildAndAwait()
                    } else {
                        ServerResponse
                            .ok()
                            .contentType(dokument.contentType)
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=${request.dokumentId()}.${dokument.contentType.subtype}")
                            .header("Content-Security-Policy", "frame-src;")
                            .bodyValueAndAwait(dokument.dataBuffer)
                    }
                } catch (cause: IkkeTilgang) {
                    ServerResponse
                        .status(HttpStatus.FORBIDDEN)
                        .buildAndAwait()
                }

            }
        }
    }

    private suspend fun ServerRequest.journalpostId(): JournalpostId = pathVariable(JournalpostIdKey)
    private suspend fun ServerRequest.dokumentId(): DokumentId = pathVariable(DokumentIdKey)
    private suspend fun ServerRequest.omfordelingRequest() =
        body(BodyExtractors.toMono(OmfordelingRequest::class.java)).awaitFirst()

    private suspend fun ServerRequest.ident() = body(BodyExtractors.toMono(IdentDto::class.java)).awaitFirst()

    data class OmfordelingRequest(
        val fagsakYtelseTypeKode: String,
    )
}

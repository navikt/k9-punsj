package no.nav.k9punsj.brev

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9punsj.tilgangskontroll.AuthenticationHandler
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.tilgangskontroll.azuregraph.IAzureGraphService
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.tilgangskontroll.InnloggetUtils
import no.nav.k9punsj.openapi.OasFeil
import no.nav.k9punsj.utils.ServerRequestUtils.hentNorskIdentHeader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.*
import kotlin.coroutines.coroutineContext

@Configuration
internal class BrevRoutes(
    private val authenticationHandler: AuthenticationHandler,
    private val brevService: BrevServiceImpl,
    private val azureGraphService: IAzureGraphService,
    private val innlogget: InnloggetUtils,
    private val personService: PersonService
) {

    private companion object {
        private const val JournalpostIdKey = "journalpost_id"
        private val logger: Logger = LoggerFactory.getLogger(BrevRoutes::class.java)
    }

    internal object Urls {
        internal const val BestillBrev = "/brev/bestill" //post
        internal const val HentAlleBrev = "/brev/hentAlle/{$JournalpostIdKey}" //get
        internal const val HentAktørId = "/brev/aktorId" //get
    }

    @Bean
    fun BrevRoutes() = SaksbehandlerRoutes(authenticationHandler) {
        GET("/api${Urls.HentAlleBrev}") { request ->
            RequestContext(coroutineContext, request) {
                val journalpostId = request.pathVariable(JournalpostIdKey)
                val brev = brevService.hentBrevSendtUtPåJournalpost(journalpostId)

                if (brev.isEmpty()) {
                    return@RequestContext ServerResponse
                        .notFound()
                        .buildAndAwait()
                }

                val res = brev.map { BrevVisningDto.lagBrevVisningDto(it.brevData, it) }

                return@RequestContext ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(res)
            }
        }

        POST("/api${Urls.BestillBrev}") { request ->
            RequestContext(coroutineContext, request) {
                val bestilling = kotlin.runCatching { request.brevBestilling() }
                    .getOrElse {
                        return@RequestContext ServerResponse
                            .badRequest()
                            .json()
                            .bodyValueAndAwait(OasFeil(it.message!!))
                    }
                val saksbehandler = azureGraphService.hentIdentTilInnloggetBruker()
                val brevEntitet = kotlin.runCatching {
                    brevService.bestillBrev(
                        bestilling.journalpostId,
                        bestilling,
                        BrevType.FRITEKSTBREV,
                        saksbehandler)
                }
                    .getOrElse {
                        return@RequestContext ServerResponse
                            .badRequest()
                            .json()
                            .bodyValueAndAwait(OasFeil(it.message!!))
                    }
                return@RequestContext ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(brevEntitet)
            }
        }

        GET("/api${Urls.HentAktørId}") { request ->
            RequestContext(coroutineContext, request) {
                val norskIdent = request.hentNorskIdentHeader()
                innlogget.harInnloggetBrukerTilgangTilOgSendeInn(norskIdent,
                    Urls.HentAktørId
                )?.let { return@RequestContext it }

                val person = kotlin.runCatching { personService.finnPersonVedNorskIdentFørstDbSåPdl(norskIdent) }
                    .getOrElse {
                        return@RequestContext ServerResponse
                            .badRequest()
                            .json()
                            .bodyValueAndAwait(OasFeil(it.message!!))
                    }
                return@RequestContext ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(person.aktørId)
            }
        }
    }

    private suspend fun ServerRequest.brevBestilling() =
        body(BodyExtractors.toMono(DokumentbestillingDto::class.java)).awaitFirst()
}

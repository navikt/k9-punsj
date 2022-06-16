package no.nav.k9punsj.brev

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9punsj.tilgangskontroll.AuthenticationHandler
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.brev.dto.DokumentbestillingDto
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.felles.UventetFeil
import no.nav.k9punsj.tilgangskontroll.azuregraph.IAzureGraphService
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
        private val logger: Logger = LoggerFactory.getLogger(BrevRoutes::class.java)
    }

    internal object Urls {
        internal const val BestillBrev = "/brev/bestill" //post
        internal const val HentAktørId = "/brev/aktorId" //get
    }

    @Bean
    fun BrevRoutes() = SaksbehandlerRoutes(authenticationHandler) {
        POST("/api${Urls.BestillBrev}") { request ->
            RequestContext(coroutineContext, request) {
                val norskIdent = request.hentNorskIdentHeader()
                innlogget.harInnloggetBrukerTilgangTil(
                    norskIdentDto = listOf(norskIdent),
                    url = request.path()
                )?.let { return@RequestContext it }

                val dokumentbestillingDto = try {
                    request.body(BodyExtractors.toMono(DokumentbestillingDto::class.java)).awaitFirst()
                } catch(e: Exception) {
                    return@RequestContext ServerResponse
                        .badRequest()
                        .json()
                        .bodyValueAndAwait(OasFeil(e.message!!))
                }

                val saksbehandler = azureGraphService.hentIdentTilInnloggetBruker()

                try {
                    brevService.bestillBrev(
                        dokumentbestillingDto = dokumentbestillingDto,
                        saksbehandler = saksbehandler)
                } catch(e: Exception) {
                    return@RequestContext ServerResponse
                        .badRequest()
                        .json()
                        .bodyValueAndAwait(e.localizedMessage)
                }

                return@RequestContext ServerResponse
                    .noContent()
                    .buildAndAwait()
            }
        }

        // TODO: Skall fjernes når frontend har byttet til aktorId i PersonRoutes
        GET("/api${Urls.HentAktørId}") { request ->
            RequestContext(coroutineContext, request) {
                val norskIdent = request.hentNorskIdentHeader()
                innlogget.harInnloggetBrukerTilgangTilOgSendeInn(
                    norskIdent,
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
}

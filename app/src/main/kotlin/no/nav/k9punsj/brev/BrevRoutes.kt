package no.nav.k9punsj.brev

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9punsj.tilgangskontroll.AuthenticationHandler
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.brev.dto.BrevType
import no.nav.k9punsj.brev.dto.DokumentbestillingDto
import no.nav.k9punsj.tilgangskontroll.azuregraph.IAzureGraphService
import no.nav.k9punsj.tilgangskontroll.InnloggetUtils
import no.nav.k9punsj.openapi.OasFeil
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
                val dokumentbestillingDto = kotlin.runCatching {
                    request.body(BodyExtractors.toMono(DokumentbestillingDto::class.java)).awaitFirst()
                }.getOrElse {
                    return@RequestContext ServerResponse
                        .badRequest()
                        .json()
                        .bodyValueAndAwait(OasFeil(it.message!!))
                }

                innlogget.harInnloggetBrukerTilgangTil(
                    norskIdentDto = listOf(dokumentbestillingDto.soekerId),
                    url = request.path()
                )?.let { return@RequestContext it }

                val saksbehandler = azureGraphService.hentIdentTilInnloggetBruker()

                try {
                    brevService.bestillBrev(
                        dokumentbestillingDto = dokumentbestillingDto,
                        brevType = BrevType.FRITEKSTBREV, // TODO: Dokument/BrevMal?
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
    }
}

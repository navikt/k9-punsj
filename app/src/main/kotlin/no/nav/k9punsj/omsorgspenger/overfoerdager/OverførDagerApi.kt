package no.nav.k9punsj.omsorgspenger.overfoerdager

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9punsj.IkkeKlarForProduksjon
import no.nav.k9punsj.PublicRoutes
import no.nav.k9punsj.RequestContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.buildAndAwait
import kotlin.coroutines.coroutineContext

@Configuration
@IkkeKlarForProduksjon(begrunnelse = "Mangler håndtering av token på disse endepunktene")
class OverførDagerApi(
    private val overførDagerSøknadService: OverførDagerSøknadService) {
    companion object {
        const val søknadTypeUri = "omsorgspenger-overfoer-dager-soknad"
        private val logger: Logger = LoggerFactory.getLogger(OverførDagerApi::class.java)
    }

    @Bean
    fun overførDagerRoutes() = PublicRoutes {

        POST("/api/$søknadTypeUri") { request ->
            RequestContext(coroutineContext, request) {
                val dto = request.body(BodyExtractors.toMono(OverførDagerDTO::class.java)).awaitFirst()
                val søknad = OverførDagerConverter.map(dto)

                overførDagerSøknadService.sendSøknad(søknad, dto.dedupKey.toString())
                logger.info("Sendte inn søknad om overføring av dager med dedup key:", dto.dedupKey)

                ServerResponse
                    .status(HttpStatus.ACCEPTED)
                    .buildAndAwait()
            }
        }
    }
}

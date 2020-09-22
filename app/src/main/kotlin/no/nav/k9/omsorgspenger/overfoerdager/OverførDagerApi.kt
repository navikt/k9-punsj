package no.nav.k9.omsorgspenger.overfoerdager

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9.RequestContext
import no.nav.k9.Routes
import no.nav.k9.SøknadType
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
class OverførDagerApi(
        private val overførDagerSøknadService: OverførDagerSøknadService,
) {
    companion object {
        const val søknadType : SøknadType = "omsorgspenger-overfoer-dager-soknad"
        private val logger: Logger = LoggerFactory.getLogger(OverførDagerApi::class.java)
    }

    @Bean
    fun overførDagerRoutes() = Routes {

        POST("/api/$søknadType") { request ->
            RequestContext(coroutineContext, request) {
                val dto = request.body(BodyExtractors.toMono(OverførDagerDTO::class.java)).awaitFirst()
                val søknad = OverførDagerConverter.map(dto)

                try {
                    overførDagerSøknadService.sendSøknad(søknad, dto.dedupKey.toString())
                    logger.info("Sendte inn søknad med dedup key:", dto.dedupKey)
                    ServerResponse
                            .status(HttpStatus.ACCEPTED)
                            .buildAndAwait()
                } catch (e: Exception) {
                    logger.error("Det skjedde en feil under innsending", e)
                    ServerResponse
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .buildAndAwait()
                }
            }
        }
    }
}

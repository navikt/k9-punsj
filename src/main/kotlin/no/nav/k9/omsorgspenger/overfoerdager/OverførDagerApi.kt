package no.nav.k9.omsorgspenger.overfoerdager

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9.RequestContext
import no.nav.k9.Routes
import no.nav.k9.SøknadType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.buildAndAwait
import kotlin.coroutines.coroutineContext

@Configuration
internal class OverførDagerApi(
        private val overførDagerSøknadService: OverførDagerSøknadService,
) {
    private companion object {
        private const val søknadType : SøknadType = "omsorgspenger-overfoer-dager-soknad"
    }

    @Bean
    fun overførDagerRoutes() = Routes {

        POST("/api$søknadType") { request ->
            RequestContext(coroutineContext, request) {
                val dto = request.body(BodyExtractors.toMono(OverførDagerDTO::class.java)).awaitFirst()
                println(dto.toString())
                ServerResponse
                        .accepted()
                        .buildAndAwait()
            }
        }
    }
}

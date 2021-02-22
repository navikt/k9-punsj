package no.nav.k9punsj.fagsak

import no.nav.k9punsj.Routes
import no.nav.k9punsj.fagsak.FagsakRoutes.Urls.HenteFagsakinfo
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.json

@Configuration
internal class FagsakRoutes {

    private companion object {
        private const val NorskIdentKey = "norsk_ident"
    }

    internal object Urls {
        internal const val HenteFagsakinfo = "/fagsak/{$NorskIdentKey}"
    }

    @Bean
    fun FagsakRoutes() = Routes {

        GET("/api$HenteFagsakinfo", queryParam("ytelse") { it == "pleiepenger-sykt-barn" }) { request ->
            ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait("""
                        {
                            "fagsaker": [
                                {
                                    "fagsak_id": "1234",
                                    "url": "https://intern.nav.no/k9-sak/fagsak/1234",
                                    "fra_og_med": "2019-01-01",
                                    "til_og_med": "2019-05-05",
                                    "barn": {
                                        "navn": "Barn Barnesen",
                                        "fødselsdato" : "2017-02-02"
                                    }
                                },
                                {
                                    "fagsak_id": "4321",
                                    "url": "https://intern.nav.no/k9-sak/fagsak/4321",
                                    "fra_og_med": "2019-02-13",
                                    "til_og_med": null,
                                    "barn": {
                                        "navn": "Barn Andre Barnesen",
                                        "fødselsdato" : "2015-02-02"
                                    }
                                }
                            ]
                        }
                    """.trimIndent())
        }
    }
}

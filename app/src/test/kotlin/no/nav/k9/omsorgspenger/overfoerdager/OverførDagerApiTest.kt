package no.nav.k9.omsorgspenger.overfoerdager

import kotlinx.coroutines.runBlocking
import no.nav.k9.TestSetup
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitExchange

@ExtendWith(SpringExtension::class)
internal class OverførDagerApiTest {

    private val client = TestSetup.client

    @Test
    fun `Gyldig skjema om overføring gir 202` () {
        @Language("json")
        val req = """
            {
              "journalpostIder": [
                "466988237"
              ],
              "søknad": {
                "identitetsnummer": "01010145265",
                "arbeidssituasjon": {
                  "erArbeidstaker": true,
                  "erFrilanser": false,
                  "erSelvstendigNæringsdrivende": false,
                  "metaHarFeil": null
                },
                "borINorge": "ja",
                "omsorgenDelesMed": {
                  "identitetsnummer": "23098025855",
                  "antallOverførteDager": 1,
                  "mottaker": "Ektefelle",
                  "samboerSiden": null
                },
                "aleneOmOmsorgen": "ja",
                "barn": [
                  {
                    "identitetsnummer": "01011021154",
                    "fødselsdato": "2020-09-04"
                  }
                ],
                "mottaksdato": "2020-09-05"
              },
              "dedupKey": "01EJTT64E3PG3DX4HKA5Z7JR75"
            }
        """.trimIndent()

        val response = client.post()
                .uri { it.pathSegment("api", OverførDagerApi.søknadType).build() }
                .body(BodyInserters.fromValue(req))
                .header("content-type", "application/json")
                .awaitExchangeBlocking()

        assertThat(response.statusCode()).isEqualTo(HttpStatus.ACCEPTED)
    }

    @Test
    fun `Ugyldig skjema om overføring gir 400` () {
        @Language("json")
        val req = """
            {
              "journalpostIder": [
                "466988237"
              ],
              "søknad": {},
              "dedupKey": "01EJTT64E3PG3DX4HKA5Z7JR75"
            }
        """.trimIndent()

        val response = client.post()
                .uri { it.pathSegment("api", OverførDagerApi.søknadType).build() }
                .body(BodyInserters.fromValue(req))
                .header("content-type", "application/json")
                .awaitExchangeBlocking()

        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    private fun WebClient.RequestHeadersSpec<*>.awaitExchangeBlocking(): ClientResponse = runBlocking { awaitExchange() }
}


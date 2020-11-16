package no.nav.k9.omsorgspenger.overfoerdager

import kotlinx.coroutines.runBlocking
import no.nav.k9.TestContext
import no.nav.k9.TestSetup
import no.nav.k9.exception.ExceptionResponse
import no.nav.k9.journalpost.Journalpost
import no.nav.k9.journalpost.JournalpostRepository
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitExchange
import java.util.*

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = [JournalpostRepository::class, TestContext::class])
internal class OverførDagerApiTest {
    @Autowired
    lateinit var journalpostRepository: JournalpostRepository

    private val client = TestSetup.client

    @Test
    fun `Gyldig skjema om overføring gir 202`() {
        @Language("json")
        val req =
                """
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
        runBlocking {
            val journalpost = Journalpost(UUID.randomUUID(), "466988237", aktørId = null)
            journalpostRepository.lagre(journalpost) {
                journalpost
            }
        }
        val response = client.post()
                .uri { it.pathSegment("api", OverførDagerApi.søknadType).build() }
                .body(BodyInserters.fromValue(req))
                .header("content-type", "application/json")
                .awaitExchangeBlocking()

        assertThat(response.statusCode()).isEqualTo(HttpStatus.ACCEPTED)
    }

    @Test
    fun `Ugyldig skjema om overføring gir 400`() {
        @Language("json")
        val req =
                """
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

        val responseBody = response.body(BodyExtractors.toMono(ExceptionResponse::class.java)).block()

        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(responseBody?.exceptionId).isNotBlank()
        assertThat(responseBody?.message).isNotBlank()
        assertThat(responseBody?.uri.toString()).isNotBlank()
    }

    @Test
    fun `Uhåndtert exception gir 500 response`() {
        val fellesIdNummer = "23098025855"

        @Language("json")
        val req =
                """
            {
              "journalpostIder": [
                "466988237"
              ],
              "søknad": {
                "identitetsnummer": "$fellesIdNummer",
                "arbeidssituasjon": {
                  "erArbeidstaker": true,
                  "erFrilanser": false,
                  "erSelvstendigNæringsdrivende": false,
                  "metaHarFeil": null
                },
                "borINorge": "ja",
                "omsorgenDelesMed": {
                  "identitetsnummer": "$fellesIdNummer",
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

        val responseBody = response.body(BodyExtractors.toMono(ExceptionResponse::class.java)).block()

        assertThat(response.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(responseBody?.exceptionId).isNotBlank()
        assertThat(responseBody?.message).isNotBlank()
        assertThat(responseBody?.uri.toString()).isNotBlank()
    }

    private fun WebClient.RequestHeadersSpec<*>.awaitExchangeBlocking(): ClientResponse = runBlocking { awaitExchange() }
}

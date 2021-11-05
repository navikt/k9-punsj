package no.nav.k9punsj.omsorgspenger.overfoerdager

import no.nav.k9punsj.TestBeans
import no.nav.k9punsj.TestSetup
import no.nav.k9punsj.exception.ExceptionResponse
import no.nav.k9punsj.journalpost.Journalpost
import no.nav.k9punsj.journalpost.JournalpostRepository
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
import org.springframework.web.reactive.function.client.awaitExchange
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = [
    JournalpostRepository::class,
    TestBeans::class
])
internal class OverførDagerApiTest {
    @Autowired
    lateinit var journalpostRepository: JournalpostRepository

    private val client = TestSetup.client

    @Test
    suspend fun `Gyldig skjema om overføring gir 202`() {
        @Language("json")
        val req =
            """
            {
              "journalpostIder": [
                "466988237"
              ],
              "søknad": {
                "norskIdent": "01010145265",
                "arbeidssituasjon": {
                  "erArbeidstaker": true,
                  "erFrilanser": false,
                  "erSelvstendigNæringsdrivende": false,
                  "metaHarFeil": null
                },
                "borINorge": "ja",
                "omsorgenDelesMed": {
                  "norskIdent": "23098025855",
                  "antallOverførteDager": 1,
                  "mottaker": "Ektefelle",
                  "samboerSiden": null
                },
                "aleneOmOmsorgen": "ja",
                "barn": [
                  {
                    "norskIdent": "01011021154",
                    "fødselsdato": "2020-09-04"
                  }
                ],
                "mottaksdato": "2020-09-05"
              },
              "dedupKey": "01EJTT64E3PG3DX4HKA5Z7JR75"
            }
            """.trimIndent()

        val journalpost = Journalpost(UUID.randomUUID(), "466988237", aktørId = null)
        journalpostRepository.lagre(journalpost) {
            journalpost
        }

        client.post()
            .uri { it.pathSegment("api", OverførDagerApi.søknadTypeUri).build() }
            .body(BodyInserters.fromValue(req))
            .header("content-type", "application/json")
            .awaitExchange { clientResponse ->
                assertThat(clientResponse.statusCode()).isEqualTo(HttpStatus.ACCEPTED)
            }
    }

    @Test
    suspend fun `Uhåndtert exception gir 500 response`() {
        val fellesIdNummer = "23098025855"

        @Language("json")
        val req =
            """
            {
              "journalpostIder": [
                "466988237"
              ],
              "søknad": {
                "norskIdent": "$fellesIdNummer",
                "arbeidssituasjon": {
                  "erArbeidstaker": true,
                  "erFrilanser": false,
                  "erSelvstendigNæringsdrivende": false,
                  "metaHarFeil": null
                },
                "borINorge": "ja",
                "omsorgenDelesMed": {
                  "norskIdent": "$fellesIdNummer",
                  "antallOverførteDager": 1,
                  "mottaker": "Ektefelle",
                  "samboerSiden": null
                },
                "aleneOmOmsorgen": "ja",
                "barn": [
                  {
                    "norskIdent": "01011021154",
                    "fødselsdato": "2020-09-04"
                  }
                ],
                "mottaksdato": "2020-09-05"
              },
              "dedupKey": "01EJTT64E3PG3DX4HKA5Z7JR75"
            }
            """.trimIndent()

        client.post()
            .uri { it.pathSegment("api", OverførDagerApi.søknadTypeUri).build() }
            .body(BodyInserters.fromValue(req))
            .header("content-type", "application/json").awaitExchange { response ->
                kotlin.runCatching {
                    response.body(BodyExtractors.toMono(ExceptionResponse::class.java)).block()
                }.onSuccess {
                    assertThat(response.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                    assertThat(it?.exceptionId).isNotBlank
                    assertThat(it?.message).isNotBlank
                    assertThat(it?.uri.toString()).isNotBlank
                }.onFailure { throw IllegalStateException("", it) }
            }
    }
}

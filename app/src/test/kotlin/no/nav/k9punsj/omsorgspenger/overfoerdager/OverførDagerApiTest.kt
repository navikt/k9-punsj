package no.nav.k9punsj.omsorgspenger.overfoerdager

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.ExceptionResponse
import no.nav.k9punsj.TestBeans
import no.nav.k9punsj.TestSetup
import no.nav.k9punsj.journalpost.PunsjJournalpost
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.util.WebClientUtils.awaitStatusWithBody
import no.nav.k9punsj.util.WebClientUtils.awaitStatuscode
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.reactive.function.BodyInserters
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
    fun `Gyldig skjema om overføring gir 202`() : Unit = runBlocking {
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

        val punsjJournalpost = PunsjJournalpost(UUID.randomUUID(), "466988237", aktørId = null)
        journalpostRepository.lagre(punsjJournalpost) {
            punsjJournalpost
        }

        val httpStatus = client.post()
            .uri { it.pathSegment("api", OverførDagerApi.søknadTypeUri).build() }
            .body(BodyInserters.fromValue(req))
            .header("content-type", "application/json")
            .awaitStatuscode()

        assertThat(httpStatus).isEqualTo(HttpStatus.ACCEPTED)
    }

    @Test
    fun `Uhåndtert exception gir 500 response`() : Unit = runBlocking {
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

        val (httpStatus, exceptionResponse) = client.post()
            .uri { it.pathSegment("api", OverførDagerApi.søknadTypeUri).build() }
            .body(BodyInserters.fromValue(req))
            .header("content-type", "application/json").awaitStatusWithBody<ExceptionResponse>()

        assertThat(httpStatus).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(exceptionResponse.exceptionId).isNotBlank
        assertThat(exceptionResponse.message).isNotBlank
        assertThat(exceptionResponse.uri.toString()).isNotBlank
    }
}

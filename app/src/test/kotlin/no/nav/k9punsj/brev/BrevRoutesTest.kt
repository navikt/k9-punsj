package no.nav.k9punsj.brev

import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9.formidling.kontrakt.kodeverk.FagsakYtelseType
import no.nav.k9punsj.TestSetup
import no.nav.k9punsj.brev.dto.DokumentbestillingDto
import no.nav.k9punsj.brev.dto.MottakerDto
import no.nav.k9punsj.fordel.PunsjInnsendingType
import no.nav.k9punsj.journalpost.PunsjJournalpost
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.util.DatabaseUtil
import no.nav.k9punsj.util.IdGenerator
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import org.springframework.web.reactive.function.client.awaitExchange
import java.util.*


@ExtendWith(SpringExtension::class, MockKExtension::class)
internal class BrevRoutesTest {

    private val client = TestSetup.client
    private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"


    @Test
    fun `Bestill brev og send til k9-formidling på kafka`(): Unit = runBlocking {

        val dokumentbestillingDto = DokumentbestillingDto(
            journalpostId = lagJournalpost(),
            aktørId = "01110050053",
            overstyrtMottaker = MottakerDto("ORGNR", "1231245"),
            ytelseType = FagsakYtelseType.OMSORGSPENGER,
            dokumentMal = "INNTID",
            saksnummer = "saksnummer",
        )

        val dokumentBestillingDtoJson = objectMapper().writeValueAsString(dokumentbestillingDto)

        val (httpStatus, feil) = client.post()
            .uri { it.path("api/brev/bestill").build() }
            .header("Authorization", saksbehandlerAuthorizationHeader)
            .header("X-Nav-Norskident", "01110050053")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(dokumentBestillingDtoJson))
            .awaitExchange { it.statusCode() to it.awaitBodyOrNull<String>()}

        assertThat(feil).isNull()
        assertThat(httpStatus).isEqualTo(HttpStatus.NO_CONTENT)
    }

    @Test
    fun `bestill generell_sak brev fra inntekt uten søknad dialog`(): Unit = runBlocking {
        @Language("JSON")
        val jsonPayload = """
            {
              "aktørId": "2400825411610",
              "eksternReferanse": "525112112",
              "ytelseType": {
                "kode": "OMP",
                "kodeverk": "FAGSAK_YTELSE"
              },
              "saksnummer": "GENERELL_SAK",
              "avsenderApplikasjon": "K9PUNSJ",
              "overstyrtMottaker": {
                "type": "ORGNR",
                "id": "972674818"
              },
              "dokumentMal": "GENERELT_FRITEKSTBREV",
              "dokumentdata": {
                "fritekstbrev": {
                  "overskrift": "testtittel",
                  "brødtekst": "test -fritekst \"#\nflera rader\n\nett mellanrom innan\n\nhei"
                }
              }
            }
        """.trimIndent()

        val (httpStatus, feil) = client.post()
            .uri { it.path("api/brev/bestill").build() }
            .header("Authorization", saksbehandlerAuthorizationHeader)
            .header("X-Nav-Norskident", "01110050053")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(jsonPayload))
            .awaitExchange { it.statusCode() to it.awaitBodyOrNull<String>()}

        assertThat(feil).isNull()
        assertThat(httpStatus).isEqualTo(HttpStatus.NO_CONTENT)
    }

    private suspend fun lagJournalpost(): String {
        val journalpostId = IdGenerator.nesteId()
        val aktørId = "100000000"

        val jp = PunsjJournalpost(
            uuid = UUID.randomUUID(),
            journalpostId = journalpostId,
            aktørId = aktørId,
            type = PunsjInnsendingType.INNTEKTSMELDING_UTGÅTT.kode
        )

        DatabaseUtil.getJournalpostRepo().lagre(jp) {
            jp
        }
        return journalpostId
    }

}

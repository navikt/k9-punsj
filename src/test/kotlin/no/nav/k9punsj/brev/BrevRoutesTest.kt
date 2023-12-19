package no.nav.k9punsj.brev

import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9.formidling.kontrakt.kodeverk.FagsakYtelseType
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.brev.dto.DokumentbestillingDto
import no.nav.k9punsj.brev.dto.MottakerDto
import no.nav.k9punsj.fordel.PunsjInnsendingType
import no.nav.k9punsj.journalpost.dto.PunsjJournalpost
import no.nav.k9punsj.util.IdGenerator
import no.nav.k9punsj.utils.objectMapper
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import org.springframework.web.reactive.function.client.awaitExchange
import java.util.*

@ActiveProfiles("test")
internal class BrevRoutesTest: AbstractContainerBaseTest() {

    private val client = WebClient.create("http://localhost:$port/")
    private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"

    @Test
    fun `Bestill brev og send til k9-formidling på kafka`(): Unit = runBlocking {
        val dokumentbestillingDto = DokumentbestillingDto(
            journalpostId = lagJournalpost(),
            soekerId = "01110050053",
            mottaker = MottakerDto("ORGNR", "1231245"),
            fagsakYtelseType = FagsakYtelseType.OMSORGSPENGER,
            dokumentMal = "INNTID",
            saksnummer = "saksnummer"
        )

        val dokumentBestillingDtoJson = objectMapper().writeValueAsString(dokumentbestillingDto)

        val (httpStatus, body) = client.post()
            .uri { it.path("api/brev/bestill").build() }
            .header("Authorization", saksbehandlerAuthorizationHeader)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(dokumentBestillingDtoJson))
            .awaitExchange { it.statusCode() to it.awaitBodyOrNull<String>() }

        assertThat(body).isNotNull
        assertThat(httpStatus).isEqualTo(HttpStatus.OK)
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

        journalpostRepository.lagre(jp) {
            jp
        }
        return journalpostId
    }
}
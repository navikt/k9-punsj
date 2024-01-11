package no.nav.k9punsj.brev

import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9.formidling.kontrakt.kodeverk.FagsakYtelseType
import no.nav.k9punsj.TestSetup
import no.nav.k9punsj.brev.dto.DokumentbestillingDto
import no.nav.k9punsj.brev.dto.MottakerDto
import no.nav.k9punsj.fordel.PunsjInnsendingType
import no.nav.k9punsj.journalpost.dto.PunsjJournalpost
import no.nav.k9punsj.utils.objectMapper
import no.nav.k9punsj.util.DatabaseUtil
import no.nav.k9punsj.util.IdGenerator
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.assertj.core.api.Assertions.assertThat
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

        DatabaseUtil.getJournalpostRepo().lagre(jp) {
            jp
        }
        return journalpostId
    }
}

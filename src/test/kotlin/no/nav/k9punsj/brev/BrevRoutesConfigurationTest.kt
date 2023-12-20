package no.nav.k9punsj.brev

import kotlinx.coroutines.runBlocking
import no.nav.k9.formidling.kontrakt.kodeverk.FagsakYtelseType
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.brev.dto.DokumentbestillingDto
import no.nav.k9punsj.brev.dto.MottakerDto
import no.nav.k9punsj.fordel.PunsjInnsendingType
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.journalpost.dto.PunsjJournalpost
import no.nav.k9punsj.util.IdGenerator
import no.nav.k9punsj.utils.objectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import java.util.*


internal class BrevRoutesConfigurationTest : AbstractContainerBaseTest() {

    @Autowired
    lateinit var journalpostRepository: JournalpostRepository

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

        webTestClient.post()
            .uri { it.pathSegment("api", "brev", "bestill").build() }
            .header("Authorization", saksbehandlerAuthorizationHeader)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(dokumentBestillingDtoJson))
            .exchange()
            .expectStatus().isOk
            .expectBody().consumeWith { it !== null }
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

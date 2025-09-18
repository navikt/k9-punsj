package no.nav.k9punsj.notat

import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.akjonspunkter.AksjonspunktKode
import no.nav.k9punsj.akjonspunkter.AksjonspunktRepository
import no.nav.k9punsj.akjonspunkter.AksjonspunktStatus
import no.nav.k9punsj.journalpost.JournalpostService
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.reactive.function.BodyInserters

internal class NotatRoutesTest : AbstractContainerBaseTest() {
    private companion object {
        private val api = "api"
    }

    @Autowired
    private lateinit var journalpostService: JournalpostService

    @Autowired
    private lateinit var aksjonspunktRepository: AksjonspunktRepository

    @Test
    fun `Journalføring av notat`(): Unit = runBlocking {
        val søkerIdent = "66666666666" // no.nav.k9punsj.rest.eksternt.pdl.TestPdlService.harBarn
        val nyNotat = NyNotat(
            søkerIdentitetsnummer = søkerIdent,
            fagsakId = "ABC123",
            tittel = "Journalføring av notat",
            notat = "lorem ipmsum osv..."
        )

        val journalpostResponse: ByteArray = webTestClient.post()
            .uri { it.path("/$api/notat/opprett").build() }
            .header("Authorization", "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}")
            .body(BodyInserters.fromValue(nyNotat))
            .exchange()
            .expectStatus().isCreated
            .expectBody().jsonPath("$.journalpostId").isEqualTo("201")
            .returnResult().responseBody!!

        val journalpostId = JSONObject(String(journalpostResponse)).getString("journalpostId")
        val punsjJournalpost = journalpostService.hentHvisJournalpostMedId(journalpostId)
        assertThat(punsjJournalpost).isNotNull
        assertThat(punsjJournalpost!!.ytelse).isEqualTo(FagsakYtelseType.PLEIEPENGER_SYKT_BARN.kode)
        assertThat(punsjJournalpost.skalTilK9).isTrue

        val aksjonspunkt = aksjonspunktRepository.hentAksjonspunkt(journalpostId, AksjonspunktKode.PUNSJ.kode)
        assertThat(aksjonspunkt).isNotNull
        assertThat(aksjonspunkt!!.aksjonspunktStatus).isEqualTo(AksjonspunktStatus.OPPRETTET)
    }
}

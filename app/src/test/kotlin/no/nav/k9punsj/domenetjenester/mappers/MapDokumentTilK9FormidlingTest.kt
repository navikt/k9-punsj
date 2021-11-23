package no.nav.k9punsj.domenetjenester.mappers

import kotlinx.coroutines.runBlocking
import no.nav.k9.formidling.kontrakt.kodeverk.FagsakYtelseType
import no.nav.k9punsj.brev.DokumentbestillingDto
import no.nav.k9punsj.rest.eksternt.pdl.IdentPdl
import no.nav.k9punsj.rest.eksternt.pdl.PdlResponse
import no.nav.k9punsj.rest.eksternt.pdl.PdlService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
internal class MapDokumentTilK9FormidlingTest {

    @MockBean
    private lateinit var pdlService: PdlService

    @Test
    fun `skal mappe bestilling uten feil`(): Unit = runBlocking {
        mockSvarFraPdl()

        // arrange
        val saksnummer = "GSF-123"
        val brevId = "dok123"
        val dokumentbestillingDto = DokumentbestillingDto("ref123",
            brevId,
            saksnummer,
            "123",
            DokumentbestillingDto.Mottaker("ORGNR", "1231245"),
            FagsakYtelseType.OMSORGSPENGER,
            "INNTID",
            null)

        // act
        val (bestilling, feil) = MapDokumentTilK9Formidling(brevId, dokumentbestillingDto, pdlService).bestillingOgFeil()

        // assert
        assertThat(feil).isEmpty()
        assertThat(bestilling.saksnummer).isEqualTo(saksnummer)
        assertThat(bestilling.aktørId).isEqualTo("321")
    }

    @Test
    fun `skal fange opp feil i bestillingen`(): Unit = runBlocking {
        // arrange
        mockSvarFraPdl()
        val saksnummer = "GSF-123"
        val brevId = "dok123"
        val dokumentbestillingDto = DokumentbestillingDto("ref123",
            brevId,
            saksnummer,
            "123",
            DokumentbestillingDto.Mottaker("ORG2NR", "1231245"),
            FagsakYtelseType.OMSORGSPENGER,
            "I2TID",
            null)


        // act
        val (_, feil) = MapDokumentTilK9Formidling(brevId, dokumentbestillingDto, pdlService).bestillingOgFeil()

        // assert
        assertThat(feil).isNotEmpty
        val feilKode = feil.map { it.feilkode }
        assertThat(feilKode).contains("DokumentMalType")
        assertThat(feilKode).contains("Mottaker")
    }

    private suspend fun mockSvarFraPdl() {
        val identer = IdentPdl.Data.HentIdenter.Identer(gruppe = "AKTORID", false, "321")
        val identPdl = IdentPdl(IdentPdl.Data(IdentPdl.Data.HentIdenter(identer = listOf(identer))), null)

        Mockito.doAnswer { PdlResponse(false, identPdl) }.`when`(pdlService).identifikator(Mockito.anyString())
    }
}

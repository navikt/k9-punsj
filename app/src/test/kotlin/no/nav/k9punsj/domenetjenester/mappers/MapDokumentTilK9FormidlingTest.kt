package no.nav.k9punsj.domenetjenester.mappers

import no.nav.k9.formidling.kontrakt.kodeverk.FagsakYtelseType
import no.nav.k9punsj.brev.DokumentbestillingDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class MapDokumentTilK9FormidlingTest {


    @Test
    fun `skal mappe bestilling uten feil`() {
       // arrange
        val saksnummer = "GSF-123"
        val dokumentbestillingDto = DokumentbestillingDto("ref123",
            "dok123",
            saksnummer,
            "123",
            DokumentbestillingDto.Mottaker("ORGNR", "1231245"),
            FagsakYtelseType.OMSORGSPENGER,
            "INNTID",
            null)


        // act
        val (bestilling, feil) = MapDokumentTilK9Formidling(dokumentbestillingDto).bestillingOgFeil()

        // assert
        assertThat(feil).isEmpty()
        assertThat(bestilling.saksnummer).isEqualTo(saksnummer)
    }

    @Test
    fun `skal fange opp feil i bestillingen`() {
        // arrange
        val saksnummer = "GSF-123"
        val dokumentbestillingDto = DokumentbestillingDto("ref123",
            "dok123",
            saksnummer,
            "123",
            DokumentbestillingDto.Mottaker("ORG2NR", "1231245"),
            FagsakYtelseType.OMSORGSPENGER,
            "I2TID",
            null)


        // act
        val (_, feil) = MapDokumentTilK9Formidling(dokumentbestillingDto).bestillingOgFeil()

        // assert
        assertThat(feil).isNotEmpty
        val feilKode = feil.map { it.feilkode }
        assertThat(feilKode).contains("DokumentMalType")
        assertThat(feilKode).contains("Mottaker")
    }

}

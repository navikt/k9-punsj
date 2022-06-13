package no.nav.k9punsj.domenetjenester.mappers

import kotlinx.coroutines.runBlocking
import no.nav.k9.formidling.kontrakt.kodeverk.FagsakYtelseType
import no.nav.k9punsj.brev.dto.DokumentbestillingDto
import no.nav.k9punsj.brev.MapDokumentTilK9Formidling
import no.nav.k9punsj.brev.dto.MottakerDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
internal class MapDokumentTilK9FormidlingTest {

    @Test
    fun `skal mappe bestilling uten feil`(): Unit = runBlocking {
        // arrange
        val saksnummer = "GSF-123"
        val brevId = "dok123"
        val dokumentbestillingDto = DokumentbestillingDto("ref123",
            brevId,
            saksnummer,
            "123",
            MottakerDto("ORGNR", "1231245"),
            FagsakYtelseType.OMSORGSPENGER,
            "INNTID",
            null)

        // act
        val (bestilling, feil) = MapDokumentTilK9Formidling(dokumentbestillingDto, "321").bestillingOgFeil()

        // assert
        assertThat(feil).isEmpty()
        assertThat(bestilling.saksnummer).isEqualTo(saksnummer)
        assertThat(bestilling.aktørId).isEqualTo("321")
    }

    @Test
    fun `skal fange opp feil i bestillingen`(): Unit = runBlocking {
        // arrange
        val saksnummer = "GSF-123"
        val brevId = "dok123"
        val dokumentbestillingDto = DokumentbestillingDto("ref123",
            brevId,
            saksnummer,
            "123",
            MottakerDto("ORG2NR", "1231245"),
            FagsakYtelseType.OMSORGSPENGER,
            "I2TID",
            null)


        // act
        val (_, feil) = MapDokumentTilK9Formidling(dokumentbestillingDto, "321").bestillingOgFeil()

        // assert
        assertThat(feil).isNotEmpty
        val feilKode = feil.map { it.feilkode }
        assertThat(feilKode).contains("DokumentMalType")
        assertThat(feilKode).contains("Mottaker")
    }
}

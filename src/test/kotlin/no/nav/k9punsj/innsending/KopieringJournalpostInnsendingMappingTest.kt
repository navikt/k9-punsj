package no.nav.k9punsj.innsending

import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9punsj.journalpost.dto.KopierJournalpostInfo
import org.intellij.lang.annotations.Language
import org.json.JSONObject
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

internal class KopieringJournalpostInnsendingMappingTest {
    private val innsendingClient = LoggingInnsendingClient()

    @Test
    fun `mappe kopiering av journalpost for pleiepenger syk barn`() {
        mapKopierJournalpostOgAssert(
            fagsakYtelseType = FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
            søknadstype = "PleiepengerSyktBarn"
        )
    }

    @Test
    fun `mappe kopiering av journalpost for omsorgspengr kronisk sykt barn`() {
        mapKopierJournalpostOgAssert(
            fagsakYtelseType = FagsakYtelseType.OMSORGSPENGER_KS,
            søknadstype = "OmsorgspengerKroniskSyktBarn"
        )
    }

    private fun mapKopierJournalpostOgAssert(fagsakYtelseType: FagsakYtelseType, søknadstype: String) {
        val (_, value) = innsendingClient.mapKopierJournalpost(
            KopierJournalpostInfo(
                journalpostId = "11111111111",
                fra = "22222222222",
                til = "33333333333",
                pleietrengende = "44444444444",
                ytelse = fagsakYtelseType
            )
        )

        val behov = JSONObject(value).getJSONObject("@behov").getJSONObject("KopierPunsjbarJournalpost").toString()

        @Language("JSON")
        val expected = """
            {
              "versjon": "1.0.0",
              "journalpostId": "11111111111",
              "fra": "22222222222",
              "til": "33333333333",
              "pleietrengende": "44444444444",
              "annenPart": null,
              "søknadstype": "$søknadstype"
            }
        """.trimIndent()

        JSONAssert.assertEquals(expected, behov, true)
    }
}

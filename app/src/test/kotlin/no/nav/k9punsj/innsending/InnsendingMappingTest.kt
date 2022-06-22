package no.nav.k9punsj.innsending

import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9.søknad.ytelse.Ytelse
import no.nav.k9punsj.journalpost.KopierJournalpostInfo
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.omsorgspengerkronisksyktbarn.MapOmsKSBTilK9Format
import no.nav.k9punsj.omsorgspengerkronisksyktbarn.OmsorgspengerKroniskSyktBarnSøknadDto
import no.nav.k9punsj.pleiepengersyktbarn.MapPsbTilK9Format
import no.nav.k9punsj.pleiepengersyktbarn.PleiepengerSyktBarnSøknadDto
import no.nav.k9punsj.util.LesFraFilUtil
import org.intellij.lang.annotations.Language
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.lang.IllegalArgumentException
import java.util.UUID

internal class InnsendingMappingTest {
    private val innsendingClient = LoggingInnsendingClient()

    @Test
    fun `mappe pleiepenger sykt barn søknad`() {
        mapTilK9FormatOgAssert<PleiepengerSyktBarnSøknadDto>(
            søknad = LesFraFilUtil.søknadFraFrontend(),
            ytelse = Ytelse.Type.PLEIEPENGER_SYKT_BARN
        )
    }

    @Test
    fun `mappe kopiering av journalpost for pleiepenger syk barn`() {
        mapKopierJournalpostOgAssert(
            fagsakYtelseType = FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
            søknadstype = "PleiepengerSyktBarn"
        )
    }

    @Test
    fun `mappe omsorgspenger kronisk sykt barn søknad`() {
        mapTilK9FormatOgAssert<OmsorgspengerKroniskSyktBarnSøknadDto>(
            søknad = LesFraFilUtil.søknadFraFrontendOmsKSB(),
            ytelse = Ytelse.Type.OMSORGSPENGER_UTVIDETRETT_KRONISK_SYKT_BARN
        )
    }

    @Test
    fun `mappe kopiering av journalpost for omsorgspengr kronisk sykt barn`() {
        mapKopierJournalpostOgAssert(
            fagsakYtelseType = FagsakYtelseType.OMSORGSPENGER_KS,
            søknadstype = "OmsorgspengerKroniskSyktBarn"
        )
    }

    @Test
    fun `mappe pleiepenger sykt barn endringssøknad med ignorert opptjening`() {
        mapTilK9FormatOgAssert<PleiepengerSyktBarnSøknadDto>(
            søknad = LesFraFilUtil.ignorerOpptjening(),
            ytelse = Ytelse.Type.PLEIEPENGER_SYKT_BARN
        )
    }

    private inline fun <reified T> mapTilK9FormatOgAssert(søknad: MutableMap<String, Any?>, ytelse: Ytelse.Type) {
        val dto: T = objectMapper().convertValue(søknad)
        val k9Format = when (dto) {
            is OmsorgspengerKroniskSyktBarnSøknadDto -> {
                MapOmsKSBTilK9Format(
                    søknadId = dto.soeknadId,
                    journalpostIder = dto.journalposter?.toSet() ?: emptySet(),
                    dto = dto
                ).søknadOgFeil().first
            }

            is PleiepengerSyktBarnSøknadDto -> {
                MapPsbTilK9Format(
                    søknadId = dto.soeknadId,
                    journalpostIder = dto.journalposter?.toSet() ?: emptySet(),
                    perioderSomFinnesIK9 = emptyList(),
                    dto = dto
                ).søknadOgFeil().first
            }

            else -> throw IllegalArgumentException("Ikke støttet type.")
        }

        val (_, value) = innsendingClient.mapSøknad(
            søknadId = k9Format.søknadId.id,
            søknad = k9Format,
            tilleggsOpplysninger = mapOf(
                "foo" to "bar",
                "bar" to 2
            ),
            correlationId = "${UUID.randomUUID()}"
        )

        val behov = JSONObject(value).getJSONObject("@behov").getJSONObject("PunsjetSøknad")
        assertTrue(behov.has("søknad") && behov.get("søknad") is JSONObject)
        assertEquals("bar", behov.getString("foo"))
        assertEquals(2, behov.getInt("bar"))
        assertEquals("1.0.0", behov.getString("versjon"))
        val k9FormatSøknad = behov.getJSONObject("søknad")
        assertEquals(ytelse.kode(), k9FormatSøknad.getJSONObject("ytelse").getString("type"))
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

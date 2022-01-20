package no.nav.k9punsj.innsending

import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9punsj.domenetjenester.mappers.MapOmsKSBTilK9Format
import no.nav.k9punsj.domenetjenester.mappers.MapPsbTilK9Format
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.dto.OmsorgspengerKroniskSyktBarnSøknadDto
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadDto
import no.nav.k9punsj.util.LesFraFilUtil
import org.intellij.lang.annotations.Language
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.util.UUID

internal class InnsendingMappingTest {
    private val innsendingClient = LoggingInnsendingClient()

    @Test
    fun `mappe pleiepenger sykt barn søknad`() {
        val søknad = LesFraFilUtil.søknadFraFrontend()
        val dto = objectMapper().convertValue<PleiepengerSøknadDto>(søknad)

        val k9Format = MapPsbTilK9Format(
            søknadId = dto.soeknadId,
            journalpostIder = dto.journalposter?.toSet()?: emptySet(),
            perioderSomFinnesIK9 = emptyList(),
            dto = dto
        ).søknadOgFeil().first

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
        assertEquals( "bar", behov.getString("foo"))
        assertEquals( 2, behov.getInt("bar"))
        assertEquals("1.0.0", behov.getString("versjon"))
        val k9FormatSøknad = behov.getJSONObject("søknad")
        assertEquals("PLEIEPENGER_SYKT_BARN", k9FormatSøknad.getJSONObject("ytelse").getString("type"))
    }

    @Test
    fun `mappe kopiering av journalpost for pleiepenger syk barn`() {
        val (_, value) = innsendingClient.mapKopierJournalpost(KopierJournalpostInfo(
            correlationId = "5bc73106-a0b1-46a4-a297-54541265934e",
            journalpostId = "11111111111",
            fra = "22222222222",
            til = "33333333333",
            pleietrengende = "44444444444",
            ytelse = FagsakYtelseType.PLEIEPENGER_SYKT_BARN
        ))

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
              "søknadstype": "PleiepengerSyktBarn"
            }
        """.trimIndent()

        JSONAssert.assertEquals(expected, behov, true)
    }

    @Test
    fun `mappe omsorgspenger kronisk sykt barn søknad`() {
        val søknad = LesFraFilUtil.søknadFraFrontendOmsKSB()
        val dto = objectMapper().convertValue<OmsorgspengerKroniskSyktBarnSøknadDto>(søknad)

        val k9Format = MapOmsKSBTilK9Format(
            søknadId = dto.soeknadId,
            journalpostIder = dto.journalposter?.toSet()?: emptySet(),
            dto = dto
        ).søknadOgFeil().first

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
        assertEquals( "bar", behov.getString("foo"))
        assertEquals( 2, behov.getInt("bar"))
        assertEquals("1.0.0", behov.getString("versjon"))
        val k9FormatSøknad = behov.getJSONObject("søknad")
        assertEquals("OMP_UTV_KS", k9FormatSøknad.getJSONObject("ytelse").getString("type"))
    }

    @Test
    fun `mappe kopiering av journalpost for omsorgspengr kronisk sykt barn`() {
        val (_, value) = innsendingClient.mapKopierJournalpost(KopierJournalpostInfo(
            correlationId = "5bc73106-a0b1-46a4-a297-54541265934e",
            journalpostId = "11111111111",
            fra = "22222222222",
            til = "33333333333",
            pleietrengende = "44444444444",
            ytelse = FagsakYtelseType.OMSORGSPENGER_KS
        ))

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
              "søknadstype": "OmsorgspengerKroniskSyktBarn"
            }
        """.trimIndent()

        JSONAssert.assertEquals(expected, behov, true)
    }
}

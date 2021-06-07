package no.nav.k9punsj.innsending

import com.fasterxml.jackson.module.kotlin.convertValue
import de.huxhorn.sulky.ulid.ULID
import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9punsj.domenetjenester.mappers.MapFraVisningTilEksternFormat
import no.nav.k9punsj.domenetjenester.mappers.MapTilK9Format
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadVisningDto
import no.nav.k9punsj.util.IdGenerator
import no.nav.k9punsj.util.LesFraFilUtil
import org.intellij.lang.annotations.Language
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.util.UUID

internal class InnsendingMappingTest {
    private val innsendingClient = LoggingInnsendingClient()

    @Test
    fun `mappe pleiepenger sykt barn søknad`() {
        val søknad = LesFraFilUtil.søknadFraFrontend()
        val dto = objectMapper().convertValue<PleiepengerSøknadVisningDto>(søknad)

        val k9Format = MapTilK9Format.mapTilEksternFormat(
            søknad = MapFraVisningTilEksternFormat.mapTilSendingsformat(dto),
            soeknadId = "${UUID.randomUUID()}",
            hentPerioderSomFinnesIK9 = emptyList(),
            journalpostIder = setOf(IdGenerator.nesteId(), IdGenerator.nesteId())
        ).first

        val (_, value) = innsendingClient.mapSøknad(
            søknadId = k9Format.søknadId.id,
            søknad = k9Format,
            tilleggsOpplysninger = emptyMap(),
            correlationId = "${UUID.randomUUID()}"
        )
        println(value)
    }

    @Test
    fun `mappe kopiering av journalpost for pleiepenger syk barn`() {
        val (id, value) = innsendingClient.mapKopierJournalpost(KopierJournalpostInfo(
            id = ULID.parseULID("01F7KCGX2WKTJ2C0PXNGHFNBNT"),
            correlationId = "5bc73106-a0b1-46a4-a297-54541265934e",
            journalpostId = "11111111111",
            fra = "22222222222",
            til = "33333333333",
            pleietrengende = "44444444444",
            ytelse = FagsakYtelseType.PLEIEPENGER_SYKT_BARN
        ))

        assertEquals(id, "01F7KCGX2WKTJ2C0PXNGHFNBNT")
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
}

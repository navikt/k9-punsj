package no.nav.k9punsj.fordel

import no.nav.k9punsj.fordel.FordelConsumer.Companion.somFordelPunsjEventDto
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class KafkaFormatTest {

    @Test
    fun `ugyldig json`() {
        assertThrows<IllegalStateException> {
            "foo".somFordelPunsjEventDto("test-topic")
        }
    }

    @Test
    fun `kan ikke deserialiseres`() {
        assertThrows<IllegalStateException> {
            """{"foo":"bar", "aktørId":"1234"}""".somFordelPunsjEventDto("test-topic")
        }
    }

    @Test
    fun `normal punsjbar journalpost`() {
        @Language("JSON")
        val melding = """
        {
            "aktørId": "7891011",
            "ytelse": "PSB",
            "type": "INNLOGGET_CHAT",
            "journalpostId": "12131415"
        }
        """.trimIndent()

        val forventet = FordelPunsjEventDto(
            aktørId = "7891011",
            journalpostId = "12131415",
            ytelse = "PSB",
            type = "INNLOGGET_CHAT"
        )

        assertEquals(forventet, melding.somFordelPunsjEventDto("test-topic"))
    }
    @Test
    fun `kopiert punsjbar journalpost`() {
        @Language("JSON")
        val melding = """
        {
            "journalpostId": "22222222222",
            "aktørId": "33333333333",
            "ytelse": "PSB",
            "type": "KOPI",
            "opprinneligJournalpost": {
                "journalpostId": "11111111111"
            }
        }
        """.trimIndent()

        val forventet = FordelPunsjEventDto(
            aktørId = "33333333333",
            journalpostId = "22222222222",
            ytelse = "PSB",
            type = "KOPI",
            opprinneligJournalpost = FordelPunsjEventDto.OpprinneligJournalpost(
                journalpostId = "11111111111"
            )
        )

        assertEquals(forventet, melding.somFordelPunsjEventDto("test-topic"))
    }
}
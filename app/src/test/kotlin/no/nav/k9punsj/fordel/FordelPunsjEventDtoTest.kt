package no.nav.k9punsj.fordel

import no.nav.k9punsj.fordel.FordelPunsjEventDto.Companion.somFordelPunsjEventDto
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class FordelPunsjEventDtoTest {

    @Test
    fun `skal deserilisere ny dto`() {
        val topic = "testTopic"

        val dto = """{
            "aktørId": "21707997229",
            "journalpostId": "23452352",
            "type": "INNTEKSTMELDING_UTGÅTT",
            "ytelse": "OMP"
            }""".trimIndent()

        val fordelPunsjEventDto = dto.somFordelPunsjEventDto(topic)

        Assertions.assertThat(fordelPunsjEventDto.type).isEqualTo(PunsjInnsendingType.INNTEKSTMELDING_UTGÅTT.kode)
    }
}

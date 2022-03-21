package no.nav.k9punsj.journalpost

import no.nav.k9punsj.dokarkiv.SafDtos
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class SafDtosTest {


    @Test
    internal fun skal_sjekke_erIkkeStøttetDigitalJournalpost() {
        val journalpost = SafDtos.Journalpost(
            "test",
            "test",
            "test",
            null,
            null,
            null,
            emptyList(),
            emptyList(),
            listOf(SafDtos.Tilleggsopplysning("k9.kilde", "DIGITAL"), SafDtos.Tilleggsopplysning("k9.type", "SØKNAD"))
        )

        Assertions.assertThat(journalpost.erIkkeStøttetDigitalJournalpost).isFalse
    }

    @Test
    internal fun skal_sjekke_erIkkeStøttetDigitalJournalpost_true() {
        val journalpost = SafDtos.Journalpost(
            "test",
            "test",
            "test",
            null,
            null,
            null,
            emptyList(),
            emptyList(),
            listOf(SafDtos.Tilleggsopplysning("k9.kilde", "DIGITAL"), SafDtos.Tilleggsopplysning("k9.type", "SØKNAD_TEST"))
        )

        Assertions.assertThat(journalpost.erIkkeStøttetDigitalJournalpost).isTrue
    }

    @Test
    internal fun skal_sjekke_erIkkeStøttetDigitalJournalpost_ikke_digital() {
        val journalpost = SafDtos.Journalpost(
            "test",
            "test",
            "test",
            null,
            null,
            null,
            emptyList(),
            emptyList(),
            listOf(SafDtos.Tilleggsopplysning("k9.kilde", "SKANNING"), SafDtos.Tilleggsopplysning("k9.type", "SØKNAD"))
        )

        Assertions.assertThat(journalpost.erIkkeStøttetDigitalJournalpost).isFalse
    }
}

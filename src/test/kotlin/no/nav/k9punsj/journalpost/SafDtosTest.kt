package no.nav.k9punsj.journalpost

import no.nav.k9punsj.integrasjoner.dokarkiv.SafDtos
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class SafDtosTest {

    @Test
    internal fun skal_sjekke_erIkkeStøttetDigitalJournalpost() {
        val journalpost = SafDtos.Journalpost(
            journalpostId = "123456789",
            tema = "test",
            tittel = "omsorgspengerutbetaling",
            journalposttype = SafDtos.JournalpostType.UTGAAENDE.kode,
            journalstatus = "test",
            datoOpprettet = LocalDateTime.now(),
            bruker = null,
            sak = null,
            avsender = null,
            avsenderMottaker = null,
            dokumenter = emptyList(),
            relevanteDatoer = emptyList(),
            tilleggsopplysninger = listOf(SafDtos.Tilleggsopplysning("k9.kilde", "DIGITAL"), SafDtos.Tilleggsopplysning("k9.type", "SØKNAD"))
        )

        Assertions.assertThat(journalpost.erIkkeStøttetDigitalJournalpost).isFalse
    }

    @Test
    internal fun skal_sjekke_erIkkeStøttetTema_true() {
        val journalpost = SafDtos.Journalpost(
            journalpostId = "123456789",
            tema = "AAP",
            tittel = "Arbeidsavklaringspenger",
            journalposttype = SafDtos.JournalpostType.INNGAAENDE.kode,
            journalstatus = SafDtos.Journalstatus.MOTTATT.name,
            datoOpprettet = LocalDateTime.now(),
            bruker = null,
            sak = null,
            avsender = null,
            avsenderMottaker = null,
            dokumenter = emptyList(),
            relevanteDatoer = emptyList(),
            tilleggsopplysninger = listOf()
        )

        Assertions.assertThat(journalpost.ikkeErTemaOMS).isTrue()
    }

    @Test
    internal fun skal_sjekke_erIkkeStøttetDigitalJournalpost_true() {
        val journalpost = SafDtos.Journalpost(
            journalpostId = "123456789",
            tema = "test",
            tittel = "omsorgspengerutbetaling",
            journalposttype = SafDtos.JournalpostType.INNGAAENDE.kode,
            journalstatus = "test",
            datoOpprettet = LocalDateTime.now(),
            bruker = null,
            sak = null,
            avsender = null,
            avsenderMottaker = null,
            dokumenter = emptyList(),
            relevanteDatoer = emptyList(),
            tilleggsopplysninger = listOf(SafDtos.Tilleggsopplysning("k9.kilde", "DIGITAL"), SafDtos.Tilleggsopplysning("k9.type", "SØKNAD_TEST"))
        )

        Assertions.assertThat(journalpost.erIkkeStøttetDigitalJournalpost).isTrue
    }

    @Test
    internal fun skal_sjekke_erIkkeStøttetDigitalJournalpost_ikke_digital() {
        val journalpost = SafDtos.Journalpost(
            journalpostId = "123456789",
            tema = "test",
            tittel = "omsorgspengerutbetaling",
            journalposttype = SafDtos.JournalpostType.NOTAT.kode,
            journalstatus = "test",
            datoOpprettet = LocalDateTime.now(),
            bruker = null,
            sak = null,
            avsender = null,
            avsenderMottaker = null,
            dokumenter = emptyList(),
            relevanteDatoer = emptyList(),
            tilleggsopplysninger = listOf(SafDtos.Tilleggsopplysning("k9.kilde", "SKANNING"), SafDtos.Tilleggsopplysning("k9.type", "SØKNAD"))
        )

        Assertions.assertThat(journalpost.erIkkeStøttetDigitalJournalpost).isFalse
    }
}

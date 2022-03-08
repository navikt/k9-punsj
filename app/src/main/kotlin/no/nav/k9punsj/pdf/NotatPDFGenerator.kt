package no.nav.k9punsj.pdf

import no.nav.k9punsj.journalpost.NyJournalpost
import org.springframework.stereotype.Service
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime

@Service
class NotatPDFGenerator : PDFGenerator<NyJournalpost>() {

    override val templateNavn: String
        get() = "notat"


    override fun NyJournalpost.tilMap(): Map<String, Any?> = mapOf(
        "tittel" to tittel,
        "fagsakId" to fagsakId,
        "notat" to notat,
        "søkerIdentitetsnummer" to søkerIdentitetsnummer,
        "inneholderSensitivePersonopplysninger" to inneholderSensitivePersonopplysninger,
        "tidspunkt" to DATE_TIME_FORMATTER.format(ZonedDateTime.now(UTC))
    )

    override val bilder: Map<String, String>
        get() = mapOf()
}

package no.nav.k9punsj.notat

import no.nav.k9punsj.pdf.PDFGenerator
import org.springframework.stereotype.Service
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime

@Service
class NotatPDFGenerator : PDFGenerator<NotatOpplysninger>() {

    override val templateNavn: String
        get() = "notat"


    override fun NotatOpplysninger.tilMap(): Map<String, Any?> = mapOf(
        "tittel" to tittel,
        "fagsakId" to fagsakId,
        "notat" to notat,
        "søkerIdentitetsnummer" to søkerIdentitetsnummer,
        "søkerNavn" to søkerNavn,
        "saksbehandlerNavn" to saksbehandlerNavn,
        "saksbehandlerEnhet" to saksbehandlerEnhet,
        "inneholderSensitivePersonopplysninger" to inneholderSensitivePersonopplysninger,
        "tidspunkt" to DATE_TIME_FORMATTER.format(ZonedDateTime.now(UTC))
    )

    override val bilder: Map<String, String>
        get() = mapOf()
}

data class NotatOpplysninger(
    val søkerIdentitetsnummer: String,
    val søkerNavn: String,
    val fagsakId: String,
    val tittel: String,
    val notat: String,
    val saksbehandlerNavn: String,
    val saksbehandlerEnhet: String,
    val inneholderSensitivePersonopplysninger: Boolean
)

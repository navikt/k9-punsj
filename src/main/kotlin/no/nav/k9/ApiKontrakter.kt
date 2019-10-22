package no.nav.k9

import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.ConstraintViolation

typealias JournalpostId = String

typealias Innhold = MutableMap<String, Any?>

data class Innsending(val innhold: MutableList<Innhold>)

internal fun Innsending.oppdater(nyInnsending: Innsending) : Innsending {
    innhold.forEachIndexed { index, eksisterendeInnhold ->
        eksisterendeInnhold.putAll(nyInnsending.innhold[index])
    }
    return this
}

data class MellomlagringsResultat(
        val innhold: List<Innhold>,
        private val violations: Set<ConstraintViolation<*>>
) {
    val feil = violations.map { Feil(
            attributt = it.propertyPath.toString(),
            ugyldigVerdi = it.invalidValue,
            melding = it.message
    )}
}

data class Feil(
        val attributt: String,
        @JsonProperty(value = "ugyldig_verdi")
        val ugyldigVerdi: Any?,
        val melding: String
)



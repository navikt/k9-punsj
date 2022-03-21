package no.nav.k9punsj.integrasjoner.pdl

import java.time.LocalDate

data class Personopplysninger (
    internal val identitetsnummer: String,
    internal val fødselsdato: LocalDate,
    internal val fornavn: String,
    internal val mellomnavn: String?,
    internal val etternavn: String,
    private val gradering: Gradering
) {
    internal val erUgradert = Gradering.UGRADERT == gradering

    enum class Gradering {
        UGRADERT,
        FORTROLIG,
        STRENGT_FORTROLIG,
        STRENGT_FORTROLIG_UTLAND;

        internal companion object {
            internal fun String?.fraPdlDto() = when {
                isNullOrBlank() -> UGRADERT
                else -> valueOf(this)
            }
        }
    }
}
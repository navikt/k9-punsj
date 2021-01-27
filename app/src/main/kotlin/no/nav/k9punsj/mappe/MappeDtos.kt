package no.nav.k9punsj.mappe

import no.nav.k9punsj.JournalpostId
import no.nav.k9punsj.Mangel
import no.nav.k9punsj.NorskIdent
import no.nav.k9punsj.SøknadJson

data class MapperSvarDTO(
        val mapper : List<MappeSvarDTO>
)

data class MappeSvarDTO(
        val mappeId: MappeId,
        val personer: MutableMap<NorskIdent, PersonDTO<SøknadJson>>
) {
    internal fun erKomplett() = personer.all { it.value.mangler.isEmpty() }
}

data class PersonDTO<T>(
        val innsendinger: MutableSet<JournalpostId>,
        val soeknad: T,
        val mangler: Set<Mangel>
)

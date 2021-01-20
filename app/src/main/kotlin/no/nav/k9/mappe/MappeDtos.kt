package no.nav.k9.mappe

import no.nav.k9.*

data class MapperSvarDTO(
        val mapper : List<MappeSvarDTO>
)

data class MappeSvarDTO(
        val mappeId: MappeId,
        val søknadType: SøknadType,
        val personer: MutableMap<NorskIdent, PersonDTO<SøknadJson>>
) {
    internal fun erKomplett() = personer.all { it.value.mangler.isEmpty() }
}

data class PersonDTO<T>(
        val innsendinger: MutableSet<JournalpostId>,
        val soeknad: T,
        val mangler: Set<Mangel>
)

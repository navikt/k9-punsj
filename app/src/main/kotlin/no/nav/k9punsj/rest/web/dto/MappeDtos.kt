package no.nav.k9punsj.rest.web.dto

import no.nav.k9punsj.db.datamodell.MappeId

data class MapperSvarDTO(
        val mapper : List<MappeSvarDTO>
)

data class MappeSvarDTO(
        val mappeId: MappeIdDto,
        val søker: NorskIdentDto,
        val bunker: List<BunkeDto>
)

data class PersonDTO<T>(
        val innsendinger: MutableSet<JournalpostIdDto>,
        val soeknad: T?,
)

data class MappeFeil(
        val mappeId: MappeId,
        val feil: List<SøknadFeilDto>
) {
        data class SøknadFeilDto(
                val felt: String?,
                val feilkode: String?,
                val feilmelding: String?,
        )
}

package no.nav.k9punsj.felles.dto

data class SøknadFeil(
    val søknadIdDto: String,
    val feil: List<SøknadFeilDto>
) {
        data class SøknadFeilDto(
                val felt: String?,
                val feilkode: String?,
                val feilmelding: String?,
        )
}


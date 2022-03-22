package no.nav.k9punsj.domenetjenester.dto

data class SøknadFeil(
    val søknadIdDto: SøknadIdDto,
    val feil: List<SøknadFeilDto>
) {
        data class SøknadFeilDto(
                val felt: String?,
                val feilkode: String?,
                val feilmelding: String?,
        )
}


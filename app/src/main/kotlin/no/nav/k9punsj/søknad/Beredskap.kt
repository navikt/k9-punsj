package no.nav.k9punsj.søknad


data class Beredskap(

    val perioder: Map<Periode, BeredskapPeriodeInfo>


)

data class BeredskapPeriodeInfo(
    val tilleggsinformasjon: String
)


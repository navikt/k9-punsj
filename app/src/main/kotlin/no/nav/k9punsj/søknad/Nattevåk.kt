package no.nav.k9punsj.søknad


data class Nattevåk(

    val perioder: Map<Periode, NattevåkPeriodeInfo>

)

data class NattevåkPeriodeInfo(
    val tilleggsinformasjon: String
)

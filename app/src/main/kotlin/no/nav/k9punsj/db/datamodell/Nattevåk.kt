package no.nav.k9punsj.db.datamodell


data class Nattevåk(

    val perioder: Map<Periode, NattevåkPeriodeInfo>

)

data class NattevåkPeriodeInfo(
    val tilleggsinformasjon: String
)

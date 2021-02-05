package no.nav.k9punsj.db.datamodell


data class Beredskap(

    val perioder: Map<Periode, BeredskapPeriodeInfo>


)

data class BeredskapPeriodeInfo(
    val tilleggsinformasjon: String
)


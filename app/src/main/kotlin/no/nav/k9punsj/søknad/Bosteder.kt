package no.nav.k9punsj.søknad

import no.nav.k9.søknad.felles.type.Landkode


data class Bosteder(
        val perioder: Map<Periode, BostedPeriodeInfo>

)

data class BostedPeriodeInfo(
        val landkode: Landkode
)

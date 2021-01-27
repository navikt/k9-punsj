package no.nav.k9punsj.søknad

import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold.UtenlandsoppholdÅrsak
import no.nav.k9.søknad.felles.type.Landkode


data class Utenlandsopphold(
        val perioder: Map<Periode, UtenlandsoppholdPeriodeInfo>

)

data class UtenlandsoppholdPeriodeInfo(
        val landkode: Landkode,
        val utenlandsoppholdÅrsak: UtenlandsoppholdÅrsak
)

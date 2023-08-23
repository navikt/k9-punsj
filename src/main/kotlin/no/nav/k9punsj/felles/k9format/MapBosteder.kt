package no.nav.k9punsj.felles.k9format

import no.nav.k9.søknad.felles.personopplysninger.Bosteder
import no.nav.k9.søknad.felles.type.Landkode
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9punsj.felles.dto.BostederDto
import no.nav.k9punsj.utils.PeriodeUtils.erSatt
import no.nav.k9punsj.utils.PeriodeUtils.somK9Periode
import no.nav.k9punsj.utils.StringUtils.erSatt

fun List<BostederDto>.mapTilBosteder(): Bosteder? {
    val k9Bosteder = mutableMapOf<Periode, Bosteder.BostedPeriodeInfo>()
    filter { it.periode.erSatt() }.forEach { bosted ->
        val k9Periode = bosted.periode!!.somK9Periode()!!
        val k9Info = Bosteder.BostedPeriodeInfo()
        if (bosted.land.erSatt()) {
            k9Info.medLand(Landkode.of(bosted.land))
        }
        k9Bosteder[k9Periode] = k9Info
    }
    if (k9Bosteder.isNotEmpty()) {
        return Bosteder().medPerioder(k9Bosteder)
    }
    return null
}

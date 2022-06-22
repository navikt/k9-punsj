package no.nav.k9punsj.felles.k9format

import no.nav.fpsak.tidsserie.LocalDateInterval
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.k9.søknad.felles.Feil
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold
import no.nav.k9.søknad.felles.type.Landkode
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9punsj.felles.dto.UtenlandsoppholdDto
import no.nav.k9punsj.felles.dto.UtenlandsoppholdDtoV2
import no.nav.k9punsj.felles.k9format.MappingUtils.mapEllerLeggTilFeil
import no.nav.k9punsj.utils.PeriodeUtils.erSatt
import no.nav.k9punsj.utils.PeriodeUtils.jsonPath
import no.nav.k9punsj.utils.PeriodeUtils.somK9Periode
import no.nav.k9punsj.utils.StringUtils.erSatt

fun List<UtenlandsoppholdDto>.leggTilUtenlandsopphold(feil: MutableList<Feil>): Utenlandsopphold? {
    val k9Utenlandsopphold = mutableMapOf<Periode, Utenlandsopphold.UtenlandsoppholdPeriodeInfo>()
    filter { it.periode.erSatt() }.forEach { utenlandsopphold ->
        val k9Periode = utenlandsopphold.periode!!.somK9Periode()!!
        val k9Info = Utenlandsopphold.UtenlandsoppholdPeriodeInfo()
        if (utenlandsopphold.land.erSatt()) {
            k9Info.medLand(Landkode.of(utenlandsopphold.land))
        }
        if (utenlandsopphold.årsak.erSatt()) {
            mapEllerLeggTilFeil(feil, "ytelse.utenlandsopphold.${k9Periode.jsonPath()}.årsak") {
                Utenlandsopphold.UtenlandsoppholdÅrsak.of(utenlandsopphold.årsak!!)
            }?.also { k9Info.medÅrsak(it) }
        }

        k9Utenlandsopphold[k9Periode] = k9Info
    }
    if (k9Utenlandsopphold.isNotEmpty()) {
        return Utenlandsopphold().medPerioder(k9Utenlandsopphold)
    }
    return null
}

fun List<UtenlandsoppholdDtoV2>.leggTilUtenlandsoppholdV2(feil: MutableList<Feil>): Utenlandsopphold? {
    val INGEN_ÅRSAK = ""
    val k9Utenlandsopphold = mutableMapOf<Periode, Utenlandsopphold.UtenlandsoppholdPeriodeInfo>()
    filter { it.periode.erSatt() }.forEach { utenlandsopphold ->
        val oppholdet: LocalDateSegment<String> = LocalDateSegment(utenlandsopphold.periode!!.fom, utenlandsopphold.periode.tom, INGEN_ÅRSAK)
        val innleggelsesperioder = LocalDateTimeline(
            utenlandsopphold.innleggelsesperioder.map {
                LocalDateSegment(it.periode!!.fom, it.periode.tom, it.årsak ?: INGEN_ÅRSAK)
            }
        )

        val utenlandsoppholdet = LocalDateTimeline(listOf(oppholdet))
        val listeMedAllePerioder = utenlandsoppholdet.combine(
            innleggelsesperioder,
            { interval, overlappenePeriode, innleggelsesperiode ->
                slåSammen(
                    interval,
                    overlappenePeriode,
                    innleggelsesperiode
                )
            },
            LocalDateTimeline.JoinStyle.CROSS_JOIN
        )

        val intervaller = listeMedAllePerioder.localDateIntervals

        intervaller.forEach { interval ->
            val segment = listeMedAllePerioder.getSegment(interval)
            val k9Periode = segment.localDateInterval.somK9Periode()
            val k9Info = Utenlandsopphold.UtenlandsoppholdPeriodeInfo()
            if (utenlandsopphold.land.erSatt()) {
                k9Info.medLand(Landkode.of(utenlandsopphold.land))
            }
            if (segment.value != INGEN_ÅRSAK) {
                mapEllerLeggTilFeil(feil, "ytelse.utenlandsopphold.${k9Periode.jsonPath()}.årsak") {
                    Utenlandsopphold.UtenlandsoppholdÅrsak.of(segment.value)
                }?.also { k9Info.medÅrsak(it) }
            }
            k9Utenlandsopphold[k9Periode] = k9Info
        }
    }
    if (k9Utenlandsopphold.isNotEmpty()) {
        return Utenlandsopphold().medPerioder(k9Utenlandsopphold)
    }
    return null
}

private fun slåSammen(
    intervall: LocalDateInterval,
    utenlandsoppholdet: LocalDateSegment<String>?,
    innleggelsesperiode: LocalDateSegment<String>?
): LocalDateSegment<String> {
    if (utenlandsoppholdet != null && innleggelsesperiode != null) {
        return LocalDateSegment(intervall, innleggelsesperiode.value)
    } else if (innleggelsesperiode != null) {
        return LocalDateSegment(intervall, innleggelsesperiode.value)
    }
    return LocalDateSegment(intervall, "")
}

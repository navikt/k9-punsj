package no.nav.k9punsj.søknad.pleiepengersyktbarn

import no.nav.k9punsj.person.Person

import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9punsj.pleiepengersyktbarn.PleiepengerSyktBarnYtelseMapper
import no.nav.k9punsj.søknad.*


data class Pleiepengersyktbarn(
    val barn: Barn,
    val periode: Periode,
    val arbeidAktivitet: ArbeidAktivitet,
    val flereOmsorgspersoner: Boolean,
    val relasjonTilBarnet: String,
    val samtykketOmsorgForBarnet: Boolean,
    val beskrivelseAvOmsorgsrollen: String,
    val bosteder: Bosteder,
    val utenlandsopphold: Utenlandsopphold,
    val beredskap: Beredskap,
    val nattevåk: Nattevåk,
    val tilsynsordning: Tilsynsordning,
    val lovbestemtFerie: LovbestemtFerie,
    val arbeid: Arbeid,
    val uttak: Uttak


): Ytelse {
    override fun getType(): Ytelse.Type {
        return Ytelse.Type.PLEIEPENGER_SYKT_BARN
    }

    override fun getSøknadsperiode(): Periode {
        return periode
    }

    override fun getBerørtePersoner(): List<Person> {
        return listOf(barn.person)
    }

    override fun mapTilEksternYtelse(): no.nav.k9.søknad.ytelse.Ytelse {
        return PleiepengerSyktBarnYtelseMapper.map(this)
    }
}










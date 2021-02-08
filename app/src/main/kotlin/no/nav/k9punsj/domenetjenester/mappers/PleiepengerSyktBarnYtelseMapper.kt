package no.nav.k9punsj.domenetjenester.mappers

import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9.søknad.felles.LovbestemtFerie
import no.nav.k9.søknad.felles.aktivitet.ArbeidAktivitet
import no.nav.k9.søknad.felles.personopplysninger.Barn
import no.nav.k9.søknad.felles.personopplysninger.Bosteder
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.ytelse.psb.v1.*
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstid
import no.nav.k9.søknad.ytelse.psb.v1.tilsyn.Tilsynsordning
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadDto

internal class PleiepengerSyktBarnYtelseMapper {
    companion object {
        private val objectMapper = no.nav.k9punsj.objectMapper()
        fun map(psb: PleiepengerSøknadDto.PleiepengerYtelseDto): PleiepengerSyktBarn {

            val barn = Barn(NorskIdentitetsnummer.of(psb.barn?.norskIdentitetsnummer), psb.barn?.fødselsdato)
            val søknadsperiode: Periode = objectMapper.convertValue(psb.søknadsperiode!!)
            val arbeidAktivitet: ArbeidAktivitet = objectMapper.convertValue(psb.arbeidAktivitet!!)
//            val databruktTilUtledning: DataBruktTilUtledning = objectMapper.convertValue(psb.dataBruktTilUtledning!!)
            val bosteder: Bosteder = objectMapper.convertValue(psb.bosteder!!)
            val utenlandsopphold = objectMapper.convertValue<Utenlandsopphold>(psb.utenlandsopphold!!)
            val beredskap = objectMapper.convertValue<Beredskap>(psb.beredskap!!)
            val nattevåk = objectMapper.convertValue<Nattevåk>(psb.nattevåk!!)
            val tilsynsordning = objectMapper.convertValue<Tilsynsordning>(psb.tilsynsordning!!)
            val lovbestemtFerie = objectMapper.convertValue<LovbestemtFerie>(psb.lovbestemtFerie!!)
            val arbeidstid = objectMapper.convertValue<Arbeidstid>(psb.arbeidstid!!)
            val uttak = objectMapper.convertValue<Uttak>(psb.uttak!!)
//            val omsorg = objectMapper.convertValue<Omsorg>(psb.omsorg!!)

            val søknadInfo = SøknadInfo()
            return PleiepengerSyktBarn(søknadsperiode, søknadInfo, barn, arbeidAktivitet, beredskap, nattevåk, tilsynsordning, arbeidstid, uttak, lovbestemtFerie, bosteder, utenlandsopphold)
        }

    }
}

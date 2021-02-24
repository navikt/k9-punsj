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
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadVisningDto
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadVisningDto.PleiepengerYtelseDto.DataBruktTilUtledningDto

internal class PleiepengerSyktBarnYtelseMapper {
    companion object {
        private val objectMapper = no.nav.k9punsj.objectMapper()
        fun map(psb: PleiepengerSøknadVisningDto.PleiepengerYtelseDto): PleiepengerSyktBarn {
            val barn = Barn(NorskIdentitetsnummer.of(psb.barn?.norskIdentitetsnummer), psb.barn?.fødselsdato)
            val søknadsperiode: Periode? = objectMapper.convertValue(psb.søknadsperiode!!)
            val arbeidAktivitet: ArbeidAktivitet? = objectMapper.convertValue(psb.arbeidAktivitet!!)
            val databruktTilUtledning: DataBruktTilUtledning? = dataBruktTilUtledning(psb.dataBruktTilUtledning)
            val bosteder: Bosteder? = objectMapper.convertValue(psb.bosteder!!)
            val utenlandsopphold: Utenlandsopphold? = objectMapper.convertValue(psb.utenlandsopphold!!)
            val beredskap: Beredskap? = objectMapper.convertValue(psb.beredskap!!)
            val nattevåk: Nattevåk? = objectMapper.convertValue(psb.nattevåk!!)
            val tilsynsordning: Tilsynsordning? = objectMapper.convertValue(psb.tilsynsordning!!)
            val lovbestemtFerie: LovbestemtFerie? = objectMapper.convertValue(psb.lovbestemtFerie!!)
            val arbeidstid: Arbeidstid? = objectMapper.convertValue(psb.arbeidstid!!)
            val uttak: Uttak? = objectMapper.convertValue(psb.uttak!!)
            val omsorg: Omsorg? = objectMapper.convertValue(psb.omsorg!!)

            return PleiepengerSyktBarn(søknadsperiode,
                databruktTilUtledning,
                barn,
                arbeidAktivitet,
                beredskap,
                nattevåk,
                tilsynsordning,
                arbeidstid,
                uttak,
                omsorg,
                lovbestemtFerie,
                bosteder,
                utenlandsopphold)
        }

        private fun dataBruktTilUtledning(dataBruktTilUtledningDto: DataBruktTilUtledningDto?): DataBruktTilUtledning? {
            var databruktTilUtledning: DataBruktTilUtledning? = null
            if (dataBruktTilUtledningDto != null) {
                databruktTilUtledning = objectMapper.convertValue(dataBruktTilUtledningDto)
            }
            return databruktTilUtledning
        }
    }
}

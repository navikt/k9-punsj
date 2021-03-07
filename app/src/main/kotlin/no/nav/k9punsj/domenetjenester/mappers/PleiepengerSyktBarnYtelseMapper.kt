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
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadMottakDto

internal class PleiepengerSyktBarnYtelseMapper {
    companion object {
        private val objectMapper = no.nav.k9punsj.objectMapper()
        fun map(psb: PleiepengerSøknadMottakDto.PleiepengerYtelseDto): PleiepengerSyktBarn {
            val barn = Barn(NorskIdentitetsnummer.of(psb.barn?.norskIdentitetsnummer), psb.barn?.fødselsdato)
            val søknadsperiode: Periode? =
                if (psb.søknadsperiode != null) objectMapper.convertValue(psb.søknadsperiode) else null
            val arbeidAktivitet: ArbeidAktivitet? =
                if (psb.arbeidAktivitet != null) objectMapper.convertValue(psb.arbeidAktivitet) else null
            val databruktTilUtledning: DataBruktTilUtledning? =
                if (psb.dataBruktTilUtledning != null) objectMapper.convertValue(psb.dataBruktTilUtledning) else null
            val bosteder: Bosteder? = if (psb.bosteder != null) objectMapper.convertValue(psb.bosteder) else null
            val utenlandsopphold: Utenlandsopphold? =
                if (psb.utenlandsopphold != null) objectMapper.convertValue(psb.utenlandsopphold) else null
            val beredskap: Beredskap? = if (psb.beredskap != null) objectMapper.convertValue(psb.beredskap) else null
            val nattevåk: Nattevåk? = if (psb.nattevåk != null) objectMapper.convertValue(psb.nattevåk) else null
            val tilsynsordning: Tilsynsordning? =
                if (psb.tilsynsordning != null) objectMapper.convertValue(psb.tilsynsordning) else null
            val lovbestemtFerie: LovbestemtFerie? =
                if (psb.lovbestemtFerie != null) objectMapper.convertValue(psb.lovbestemtFerie) else null
            val arbeidstid: Arbeidstid? =
                if (psb.arbeidstid != null) objectMapper.convertValue(psb.arbeidstid) else null
            val uttak: Uttak? = if (psb.uttak != null) objectMapper.convertValue(psb.uttak) else null
            val omsorg: Omsorg? = if (psb.omsorg != null) objectMapper.convertValue(psb.omsorg) else null

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
    }
}

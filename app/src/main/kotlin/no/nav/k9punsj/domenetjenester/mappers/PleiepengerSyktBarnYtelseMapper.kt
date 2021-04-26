package no.nav.k9punsj.domenetjenester.mappers

import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9.søknad.felles.opptjening.OpptjeningAktivitet
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
            val barn: Barn? = Barn(NorskIdentitetsnummer.of(psb.barn?.norskIdentitetsnummer), psb.barn?.fødselsdato)
            val søknadsperiode: Periode? =
                if (psb.søknadsperiode != null) objectMapper.convertValue(psb.søknadsperiode) else null
            val opptjeningAktivitet: OpptjeningAktivitet? =
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

            //TODO(OJR) fikse med ekte data
            val infoFraPunsj: InfoFraPunsj? = InfoFraPunsj().medSøknadenInneholderInfomasjonSomIkkeKanPunsjes(true)

            val pleiepengerSyktBarn = PleiepengerSyktBarn()
            barn?.let { pleiepengerSyktBarn.medBarn(it) }
            søknadsperiode?.let { pleiepengerSyktBarn.medSøknadsperiode(it) }

            //TODO(OJR) koble på endringsperioder - hent fra k9-sak
            pleiepengerSyktBarn.medEndringsperiode(listOf())

            opptjeningAktivitet?.let { pleiepengerSyktBarn.medOpptjeningAktivitet(it) }
            databruktTilUtledning?.let { pleiepengerSyktBarn.medSøknadInfo(it) }
            infoFraPunsj?.let { pleiepengerSyktBarn.medInfoFraPunsj(it) }
            bosteder?.let { pleiepengerSyktBarn.medBosteder(it) }
            utenlandsopphold?.let { pleiepengerSyktBarn.medUtenlandsopphold(it) }

            beredskap?.let { pleiepengerSyktBarn.medBeredskap(it) }
            nattevåk?.let { pleiepengerSyktBarn.medNattevåk(it) }
            tilsynsordning?.let { pleiepengerSyktBarn.medTilsynsordning(it) }
            lovbestemtFerie?.let { pleiepengerSyktBarn.medLovbestemtFerie(it) }
            arbeidstid?.let { pleiepengerSyktBarn.medArbeidstid(it) }
            uttak?.let { pleiepengerSyktBarn.medUttak(it) }
            omsorg?.let { pleiepengerSyktBarn.medOmsorg(it) }

            return pleiepengerSyktBarn
        }
    }
}

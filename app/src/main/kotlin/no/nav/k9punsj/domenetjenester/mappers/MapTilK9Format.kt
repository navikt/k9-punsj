package no.nav.k9punsj.domenetjenester.mappers

import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.Validator
import no.nav.k9.søknad.felles.Feil
import no.nav.k9.søknad.felles.Versjon
import no.nav.k9.søknad.felles.opptjening.OpptjeningAktivitet
import no.nav.k9.søknad.felles.personopplysninger.Barn
import no.nav.k9.søknad.felles.personopplysninger.Bosteder
import no.nav.k9.søknad.felles.personopplysninger.Søker
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold
import no.nav.k9.søknad.felles.type.Journalpost
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.felles.type.SøknadId
import no.nav.k9.søknad.ytelse.psb.v1.*
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstid
import no.nav.k9.søknad.ytelse.psb.v1.tilsyn.Tilsynsordning
import no.nav.k9punsj.rest.web.dto.PeriodeDto
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadMottakDto
import no.nav.k9punsj.rest.web.dto.SøknadIdDto
import java.time.ZonedDateTime
import java.util.UUID


/**  Mapper til k9-format
 *   og bruker deres validering
 */
internal class MapTilK9Format {

    companion object {
        private val validator = Validator()
        private const val SKILLE = "/"
        private const val ÅPEN = ".."
        private val objectMapper = no.nav.k9punsj.objectMapper()

        internal fun mapTilEksternFormat(
            søknad: PleiepengerSøknadMottakDto,
            soeknadId: SøknadIdDto,
            hentPerioderSomFinnesIK9: List<PeriodeDto>,
            journalpostIder: Set<String>,
        ): Pair<Søknad, List<Feil>> {
            val ytelse = søknad.ytelse
            val pleiepengerSyktBarn = map(
                psb = ytelse!!,
                endringsperioder = hentPerioderSomFinnesIK9.map {
                    fromPeriodeDtoToString(it)
                }
            )

            val søknadK9Format = opprettSøknad(
                søknadId = UUID.fromString(soeknadId),
                mottattDato = søknad.mottattDato,
                søker = Søker(NorskIdentitetsnummer.of(søknad.søker?.norskIdentitetsnummer)),
                ytelse = pleiepengerSyktBarn
            ).medJournalposter(journalpostIder.map { Journalpost().medJournalpostId(it) })
            val feil = validator.valider(søknadK9Format)

            return Pair(søknadK9Format, feil)
        }

        private fun map(
            psb: PleiepengerSøknadMottakDto.PleiepengerYtelseDto,
            endringsperioder: List<String>,
        ): PleiepengerSyktBarn {
            val barn: Barn? = if (psb.barn != null) Barn(NorskIdentitetsnummer.of(psb.barn.norskIdentitetsnummer),
                psb.barn.fødselsdato) else null
            val søknadsperiode: Periode? =
                if (psb.søknadsperiode != null) objectMapper.convertValue(psb.søknadsperiode) else null
            val opptjeningAktivitet: OpptjeningAktivitet? =
                if (psb.opptjeningAktivitet != null) objectMapper.convertValue(psb.opptjeningAktivitet) else null
            val databruktTilUtledning: DataBruktTilUtledning? =
                if (psb.soknadsinfo != null) objectMapper.convertValue(psb.soknadsinfo) else null
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

            val infoFraPunsj = InfoFraPunsj()
                .medSøknadenInneholderInfomasjonSomIkkeKanPunsjes(psb.harInfoSomIkkeKanPunsjes)
                .medInneholderMedisinskeOpplysninger(psb.harMedisinskeOpplysninger)

            val pleiepengerSyktBarn = PleiepengerSyktBarn()
            pleiepengerSyktBarn.medInfoFraPunsj(infoFraPunsj)
            barn?.let { pleiepengerSyktBarn.medBarn(it) }
            søknadsperiode?.let { pleiepengerSyktBarn.medSøknadsperiode(it) }

            if (endringsperioder.isNotEmpty()) {
                val endrignsperioder: List<Periode> = endringsperioder.map { objectMapper.convertValue(it) }
                pleiepengerSyktBarn.medEndringsperiode(endrignsperioder)
            }
            opptjeningAktivitet?.let { pleiepengerSyktBarn.medOpptjeningAktivitet(it) }
            databruktTilUtledning?.let { pleiepengerSyktBarn.medSøknadInfo(it) }

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

        private fun opprettSøknad(
            søknadId: UUID,
            versjon: Versjon = Versjon.of("1.0.0"),
            mottattDato: ZonedDateTime?,
            søker: Søker,
            ytelse: no.nav.k9.søknad.ytelse.Ytelse,
        ): Søknad {
            return Søknad(SøknadId.of(søknadId.toString()), versjon, mottattDato, søker, ytelse)
        }

        private fun fromPeriodeDtoToString(dato: PeriodeDto): String {
            val fom = if (dato.fom != null) dato.fom.toString() else ÅPEN
            val tom = if (dato.tom != null) dato.tom.toString() else ÅPEN
            return fom + SKILLE + tom
        }
    }
}

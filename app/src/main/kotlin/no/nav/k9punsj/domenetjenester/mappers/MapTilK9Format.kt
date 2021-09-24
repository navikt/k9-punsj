package no.nav.k9punsj.domenetjenester.mappers

import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Feil
import no.nav.k9.søknad.felles.Versjon
import no.nav.k9.søknad.felles.opptjening.OpptjeningAktivitet
import no.nav.k9.søknad.felles.personopplysninger.Barn
import no.nav.k9.søknad.felles.personopplysninger.Bosteder
import no.nav.k9.søknad.felles.personopplysninger.Søker
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold
import no.nav.k9.søknad.felles.type.*
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
        private val validator = PleiepengerSyktBarnSøknadValidator()
        private const val SKILLE = "/"
        private const val ÅPEN = ".."
        private val objectMapper = no.nav.k9punsj.objectMapper()

        internal fun mapTilEksternFormat(
            søknad: PleiepengerSøknadMottakDto,
            soeknadId: SøknadIdDto,
            perioderSomFinnesIK9: List<PeriodeDto>,
            journalpostIder: Set<String>,
        ): Pair<Søknad, List<Feil>> {
            val ytelse = søknad.ytelse
            val pleiepengerSyktBarn = map(
                psb = ytelse!!,
                endringsperioder = perioderSomFinnesIK9.somK9Perioder()
            )

            val søknadK9Format = opprettSøknad(
                søknadId = UUID.fromString(soeknadId),
                mottattDato = søknad.mottattDato,
                søker = Søker(NorskIdentitetsnummer.of(søknad.søker?.norskIdentitetsnummer)),
                ytelse = pleiepengerSyktBarn
            ).medJournalposter(journalpostIder.map { Journalpost()
                .medJournalpostId(it)
                .medInfomasjonSomIkkeKanPunsjes(ytelse.harInfoSomIkkeKanPunsjes)
                .medInneholderMedisinskeOpplysninger(ytelse.harMedisinskeOpplysninger)
            })

            val feil = validator.valider(søknadK9Format)
            // TODO: Dette er riktig når vi endrer til k9-format som utleder endringsperioder
            //val feil = validator.valider(søknadK9Format, perioderSomFinnesIK9.somK9Perioder())

            return Pair(søknadK9Format, feil)
        }

        private fun map(
            psb: PleiepengerSøknadMottakDto.PleiepengerYtelseDto,
            endringsperioder: List<Periode>
        ): PleiepengerSyktBarn {
            val barn: Barn? = if (psb.barn != null) Barn(NorskIdentitetsnummer.of(psb.barn.norskIdentitetsnummer),
                psb.barn.fødselsdato) else null
            val søknadsperiode: Periode? =
                if (psb.søknadsperiode != null) objectMapper.convertValue(psb.søknadsperiode) else null
            val opptjeningAktivitet: OpptjeningAktivitet? =
                if (psb.opptjeningAktivitet != null) objectMapper.convertValue(psb.opptjeningAktivitet) else null
            val databruktTilUtledning: DataBruktTilUtledning? =
                if (psb.soknadsinfo != null) objectMapper.convertValue(psb.soknadsinfo) else null

            /** Bosteder **/
            val bosteder = psb.bosteder?.let { punsjBosteder ->
                val k9Bosteder = mutableMapOf<Periode, Bosteder.BostedPeriodeInfo>()
                punsjBosteder.perioder?.filterKeys { it.isNotBlank() }?.forEach { (punsjPeriode, punsjInfo) ->
                    k9Bosteder[Periode(punsjPeriode)] = Bosteder.BostedPeriodeInfo()
                        .let { if (punsjInfo.land.isNullOrBlank()) it else it.medLand(Landkode.of(punsjInfo.land))}
                }
                Bosteder().medPerioder(k9Bosteder)
            }

            /** Utenlandsopphold **/
            val utenlandsopphold = psb.utenlandsopphold?.let { punsjUtelandsopphold ->
                val k9Utenlandsopphold = mutableMapOf<Periode, Utenlandsopphold.UtenlandsoppholdPeriodeInfo>()
                punsjUtelandsopphold.perioder?.filterKeys { it.isNotBlank() }?.forEach { (punsjPeriode, punsjInfo) ->
                    k9Utenlandsopphold[Periode(punsjPeriode)] = Utenlandsopphold.UtenlandsoppholdPeriodeInfo()
                        .let { if (punsjInfo.land.isNullOrBlank()) it else it.medLand(Landkode.of(punsjInfo.land)) }
                        .let { if (punsjInfo.årsak.isNullOrBlank()) it else it.medÅrsak(Utenlandsopphold.UtenlandsoppholdÅrsak.valueOf(punsjInfo.årsak)) }
                }
                Utenlandsopphold().medPerioder(k9Utenlandsopphold)
            }

            val beredskap: Beredskap? = if (psb.beredskap != null) objectMapper.convertValue(psb.beredskap) else null
            val nattevåk: Nattevåk? = if (psb.nattevåk != null) objectMapper.convertValue(psb.nattevåk) else null
            val tilsynsordning: Tilsynsordning? =
                if (psb.tilsynsordning != null) objectMapper.convertValue(psb.tilsynsordning) else null
            val lovbestemtFerie: LovbestemtFerie? = if (psb.lovbestemtFerie != null || psb.lovbestemtFerieSomSkalSlettes != null) mapLovbestemFerie(psb) else null
            val arbeidstid: Arbeidstid? =
                if (psb.arbeidstid != null) objectMapper.convertValue(psb.arbeidstid) else null
            val uttak: Uttak? = if (psb.uttak != null) objectMapper.convertValue(psb.uttak) else null

            val omsorg: Omsorg? = if (psb.omsorg != null) objectMapper.convertValue(psb.omsorg) else null

            val pleiepengerSyktBarn = PleiepengerSyktBarn()
            barn?.let { pleiepengerSyktBarn.medBarn(it) }
            søknadsperiode?.let { pleiepengerSyktBarn.medSøknadsperiode(it) }

            // TODO: Fjernes når vi endrer til k9-format som utleder endringsperioder
            pleiepengerSyktBarn.medEndringsperiode(endringsperioder)

            pleiepengerSyktBarn.addAllTrekkKravPerioder(psb.trekkKravPerioder.map { Periode(it) })

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

        private fun mapLovbestemFerie(psb: PleiepengerSøknadMottakDto.PleiepengerYtelseDto): LovbestemtFerie? {
            val lovbestemtFerie = LovbestemtFerie()
            if (psb.lovbestemtFerie != null) {
                psb.lovbestemtFerie.perioder?.forEach {
                    lovbestemtFerie.leggeTilPeriode(Periode(it.key), LovbestemtFerie.LovbestemtFeriePeriodeInfo().medSkalHaFerie(true))
                }
            }
            if (psb.lovbestemtFerieSomSkalSlettes != null) {
                psb.lovbestemtFerieSomSkalSlettes.perioder?.forEach {
                    lovbestemtFerie.leggeTilPeriode(Periode(it.key), LovbestemtFerie.LovbestemtFeriePeriodeInfo().medSkalHaFerie(false))
                }
            }
            return lovbestemtFerie
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

        internal fun List<PeriodeDto>.somK9Perioder() = map {
            objectMapper.convertValue<Periode>(fromPeriodeDtoToString(it))
        }

        private fun fromPeriodeDtoToString(dato: PeriodeDto): String {
            val fom = if (dato.fom != null) dato.fom.toString() else ÅPEN
            val tom = if (dato.tom != null) dato.tom.toString() else ÅPEN
            return fom + SKILLE + tom
        }
    }
}

package no.nav.k9punsj.domenetjenester.mappers


import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.Validator
import no.nav.k9.søknad.felles.Feil
import no.nav.k9.søknad.felles.Versjon
import no.nav.k9.søknad.felles.personopplysninger.Søker
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.SøknadId
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.dto.PeriodeDto
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadMottakDto
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadVisningDto
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadVisningDto.PleiepengerYtelseDto
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadVisningDto.PleiepengerYtelseDto.*
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID


internal class SøknadMapper {
    companion object {
        const val SKILLE = "/"
        const val ÅPEN = ".."
        private val validator = Validator()

        fun mapTilEksternFormat(søknad: PleiepengerSøknadVisningDto): Pair<Søknad, List<Feil>>{
            val ytelse = søknad.ytelse
            val pleiepengerSyktBarn = PleiepengerSyktBarnYtelseMapper.map(ytelse!!)

            val søknadK9Format = opprettSøknad(
                søker = Søker(NorskIdentitetsnummer.of(søknad.søker?.norskIdentitetsnummer)),
                ytelse = pleiepengerSyktBarn
            )
            val feil = validator.valider(søknadK9Format)

            return Pair(søknadK9Format, feil)
        }

        fun mapTilVisningFormat(søknad: PleiepengerSøknadMottakDto) : PleiepengerSøknadVisningDto {
            val ytelse = if (søknad.ytelse!= null) mapYtelse(søknad.ytelse) else null
            return PleiepengerSøknadVisningDto(søknad.søker, søknad.mottattDato, ytelse)
        }

        private fun mapYtelse(ytelse: PleiepengerSøknadMottakDto.PleiepengerYtelseDto) : PleiepengerYtelseDto {
            val søknadsperiode: PeriodeDto? = if(ytelse.søknadsperiode != null) fromDatoStringToPeriodeDto(ytelse.søknadsperiode) else null
            val arbeidAktivitet: ArbeidAktivitetDto? = if(ytelse.arbeidAktivitet != null ) mapArbeidAktivitetDto(ytelse.arbeidAktivitet) else null
            val bosteder: List<BostederDto>? = if(ytelse.bosteder != null) ytelse.bosteder.perioder?.map { entry -> BostederDto(fromDatoStringToPeriodeDto(entry.key), entry.value.land)}?.toList() else null
            val utenlandsoppholdDto: List<UtenlandsoppholdDto>? = if(ytelse.utenlandsopphold != null) ytelse.utenlandsopphold.perioder?.map { entry -> UtenlandsoppholdDto(fromDatoStringToPeriodeDto(entry.key), entry.value.land, entry.value.årsak)}?.toList() else null
            val beredskapDto: List<BeredskapDto>? = if(ytelse.beredskap != null) ytelse.beredskap.perioder?.map { entry -> BeredskapDto(fromDatoStringToPeriodeDto(entry.key), entry.value.tilleggsinformasjon)}?.toList() else null
            val nattevåkDto: List<NattevåkDto>? = if(ytelse.nattevåk != null) ytelse.nattevåk.perioder?.map { entry -> NattevåkDto(fromDatoStringToPeriodeDto(entry.key), entry.value.tilleggsinformasjon)}?.toList() else null
            val tilsynsordningDto: List<TilsynsordningDto>? = if(ytelse.tilsynsordning != null) ytelse.tilsynsordning.perioder?.map { entry -> TilsynsordningDto(fromDatoStringToPeriodeDto(entry.key), entry.value.etablertTilsynTimerPerDag)}?.toList() else null
            val lovbestemtFerieDto: LovbestemtFerieDto? = if(ytelse.lovbestemtFerie != null) LovbestemtFerieDto(ytelse.lovbestemtFerie.perioder?.map { entry -> (fromDatoStringToPeriodeDto(entry)) }?.toList()) else null
            val uttakDto: List<UttakDto>? = if(ytelse.uttak != null) ytelse.uttak.perioder.map { entry -> UttakDto(fromDatoStringToPeriodeDto(entry.key), entry.value.timerPleieAvBarnetPerDag)}.toList() else null
            ytelse.arbeidstid

            return PleiepengerYtelseDto(
                ytelse.barn,
                søknadsperiode,
                arbeidAktivitet,
                ytelse.dataBruktTilUtledning,
                bosteder,
                utenlandsoppholdDto,
                beredskapDto,
                nattevåkDto,
                tilsynsordningDto,
                lovbestemtFerieDto,
                null,
                uttakDto,
                ytelse.omsorg
            )
        }

        private fun mapArbeidAktivitetDto(arbeidAktivitet: PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto?): ArbeidAktivitetDto {
            val frilanser: ArbeidAktivitetDto.FrilanserDto? = objectMapper().convertValue(arbeidAktivitet?.frilanser!!)
            val selvstendigNæringsdrivende = arbeidAktivitet.selvstendigNæringsdrivende?.map { s -> mapSelvstendigNæringsdrivendeDto(s) }?.toList()
            val arbeidstaker = arbeidAktivitet.arbeidstaker?.map { a -> mapArbeidstaker(a) }?.toList()
            return ArbeidAktivitetDto(selvstendigNæringsdrivende, frilanser, arbeidstaker)
        }

        private fun mapSelvstendigNæringsdrivendeDto(s: PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.SelvstendigNæringsdrivendeDto): ArbeidAktivitetDto.SelvstendigNæringsdrivendeDto {
             return ArbeidAktivitetDto.SelvstendigNæringsdrivendeDto(s.perioder?.map { entry ->
                 ArbeidAktivitetDto.SelvstendigNæringsdrivendeDto.SelvstendigNæringsdrivendePeriodeInfoDto(
                     periode = fromDatoStringToPeriodeDto(entry.key),
                     virksomhetstyper = entry.value.virksomhetstyper,
                     regnskapsførerNavn = entry.value.regnskapsførerNavn,
                     regnskapsførerTlf =  entry.value.regnskapsførerTlf,
                     erVarigEndring =  entry.value.erVarigEndring,
                     endringDato =  entry.value.endringDato,
                     endringBegrunnelse =  entry.value.endringBegrunnelse,
                     bruttoInntekt =  entry.value.bruttoInntekt,
                     erNyoppstartet =  entry.value.erNyoppstartet,
                     registrertIUtlandet =  entry.value.registrertIUtlandet,
                     landkode =  entry.value.landkode
                 )
                  }?.toList()
                , s.organisasjonsnummer
                , s.virksomhetNavn)
        }

        private fun mapArbeidstaker(a : PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto): ArbeidAktivitetDto.ArbeidstakerDto {
            val arbeidstidInfoDto = ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto(
                a.arbeidstidInfo.jobberNormaltTimerPerDag,
                a.arbeidstidInfo.perioder?.map { entry -> ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto.ArbeidstidPeriodeInfoDto(
                    fromDatoStringToPeriodeDto(entry.key), entry.value.faktiskArbeidTimerPerDag) }?.toList()
            )

            return ArbeidAktivitetDto.ArbeidstakerDto(a.norskIdentitetsnummer, a.organisasjonsnummer, arbeidstidInfoDto)
        }


        private fun opprettSøknad(søknadId: UUID = UUID.randomUUID(), versjon: Versjon = Versjon.of("1.0.0"), mottattDato: ZonedDateTime = ZonedDateTime.now(), søker: Søker, ytelse: no.nav.k9.søknad.ytelse.Ytelse) : Søknad {
            return Søknad(SøknadId.of(søknadId.toString()), versjon, mottattDato, søker, ytelse)
        }

        private fun fromDatoStringToPeriodeDto(dato: String): PeriodeDto {
            verifiserKanVæreGyldigPeriode(dato)
            val split: Array<String> = dato.split(SKILLE.toRegex()).toTypedArray()
            val fom = parseLocalDate(split[0])
            val tom = parseLocalDate(split[1])
            return PeriodeDto(fom!!, tom!!)
        }

        private fun verifiserKanVæreGyldigPeriode(dato: String) {
            require(dato.split(SKILLE.toRegex()).toTypedArray().size == 2) { "Periode på ugylig format '$dato'." }
        }

        private fun parseLocalDate(dato: String): LocalDate? {
            return if (ÅPEN == dato) null else LocalDate.parse(dato)
        }

    }
}



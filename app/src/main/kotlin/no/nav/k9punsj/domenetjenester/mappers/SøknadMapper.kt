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
import kotlin.streams.toList


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
            val bostederDto: BostederDto? = if(ytelse.bosteder != null) BostederDto(ytelse.bosteder.perioder?.mapKeys { entry -> fromDatoStringToPeriodeDto(entry.key) }) else null
            val utenlandsoppholdDto: UtenlandsoppholdDto? = if (ytelse.utenlandsopphold!= null) UtenlandsoppholdDto(ytelse.utenlandsopphold.perioder?.mapKeys { entry -> fromDatoStringToPeriodeDto( entry.key) }) else null
            val beredskapDto: BeredskapDto? = if(ytelse.beredskap != null) BeredskapDto(ytelse.beredskap.perioder?.mapKeys { entry -> fromDatoStringToPeriodeDto(entry.key) }) else null
            val nattevåkDto: NattevåkDto? =if (ytelse.nattevåk != null) NattevåkDto(ytelse.nattevåk.perioder?.mapKeys { entry -> fromDatoStringToPeriodeDto(entry.key) }) else null
            val tilsynsordningDto: TilsynsordningDto? =if (ytelse.tilsynsordning != null) TilsynsordningDto(ytelse.tilsynsordning.perioder?.mapKeys { entry -> fromDatoStringToPeriodeDto(entry.key) }) else null
            val lovbestemtFerieDto: LovbestemtFerieDto? =if (ytelse.lovbestemtFerie != null) LovbestemtFerieDto(ytelse.lovbestemtFerie.perioder?.stream()?.map { p -> fromDatoStringToPeriodeDto(p) }?.toList()) else null
            val uttakDto: UttakDto? =if (ytelse.uttak != null) UttakDto(ytelse.uttak.perioder.mapKeys { entry -> fromDatoStringToPeriodeDto(entry.key) }) else null

            return PleiepengerYtelseDto(
                ytelse.barn,
                søknadsperiode,
                arbeidAktivitet,
                ytelse.dataBruktTilUtledning,
                bostederDto,
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
            return ArbeidAktivitetDto.SelvstendigNæringsdrivendeDto(s.perioder?.mapKeys { entry -> fromDatoStringToPeriodeDto(entry.key) }
                , s.organisasjonsnummer
                , s.virksomhetNavn)
        }

        private fun mapArbeidstaker(a : PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto): ArbeidAktivitetDto.ArbeidstakerDto {
            val arbeidstidInfoDto = ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto(
                a.arbeidstidInfo.jobberNormaltTimerPerDag,
                a.arbeidstidInfo.perioder?.mapKeys { entry -> fromDatoStringToPeriodeDto(entry.key) }
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



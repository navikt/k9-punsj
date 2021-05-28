package no.nav.k9punsj.domenetjenester.mappers

import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.dto.PeriodeDto
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadMottakDto
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadVisningDto
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException


/** Mapper fra visning format (GUI format) til backend format slik at dette letter kan gjøres om til k9-format senere
 *
 */
internal class MapFraVisningTilEksternFormat {
    companion object {
        private const val SKILLE = "/"
        private const val ÅPEN = ".."

        fun mapTilSendingsformat(søknad: PleiepengerSøknadVisningDto): PleiepengerSøknadMottakDto {
            val ytelseDto = mapYtelse(søknad)
            return PleiepengerSøknadMottakDto(
                søker = PleiepengerSøknadMottakDto.SøkerDto(søknad.soekerId),
                mottattDato = if (søknad.mottattDato != null && søknad.klokkeslett != null) ZonedDateTime.of(søknad.mottattDato,
                    søknad.klokkeslett,
                    ZoneId.of("UTC"))
                else null,
                ytelse = ytelseDto
            )
        }

        private fun mapYtelse(søknad: PleiepengerSøknadVisningDto): PleiepengerSøknadMottakDto.PleiepengerYtelseDto {
            return PleiepengerSøknadMottakDto.PleiepengerYtelseDto(
                barn = PleiepengerSøknadMottakDto.PleiepengerYtelseDto.BarnDto(søknad.barn?.norskIdent,
                    søknad.barn?.foedselsdato),
                søknadsperiode = if (søknad.soeknadsperiode?.fom != null) fromPeriodeDtoToString(søknad.soeknadsperiode) else null,
                opptjeningAktivitet = if (erNullEllerTom(søknad.opptjeningAktivitet)) mapTilMottakArbeidAktivitetDto(
                    søknad.opptjeningAktivitet!!) else null,
                soknadsinfo = if (søknad.soknadsinfo != null) PleiepengerSøknadMottakDto.PleiepengerYtelseDto.DataBruktTilUtledningDto(
                    samtidigHjemme = søknad.soknadsinfo.samtidigHjemme,
                    harMedsøker = søknad.soknadsinfo.harMedsoeker) else null,
                bosteder = if (!søknad.bosteder.isNullOrEmpty() && søknad.bosteder[0].periode != null) PleiepengerSøknadMottakDto.PleiepengerYtelseDto.BostederDto(
                    søknad.bosteder.associate {
                        Pair(fromPeriodeDtoToString(it.periode!!),
                            PleiepengerSøknadMottakDto.PleiepengerYtelseDto.BostederDto.BostedPeriodeInfoDto(it.land))
                    }) else null,
                utenlandsopphold = if (!søknad.utenlandsopphold.isNullOrEmpty() && søknad.utenlandsopphold[0].periode != null) PleiepengerSøknadMottakDto.PleiepengerYtelseDto.UtenlandsoppholdDto(
                    søknad.utenlandsopphold.associate {
                        Pair(fromPeriodeDtoToString(it.periode!!),
                            PleiepengerSøknadMottakDto.PleiepengerYtelseDto.UtenlandsoppholdDto.UtenlandsoppholdPeriodeInfoDto(
                                it.land,
                                null))
                    }) else null,
                beredskap = if (!søknad.beredskap.isNullOrEmpty() && søknad.beredskap[0].periode != null) PleiepengerSøknadMottakDto.PleiepengerYtelseDto.BeredskapDto(
                    søknad.beredskap.associate {
                        Pair(fromPeriodeDtoToString(it.periode!!),
                            PleiepengerSøknadMottakDto.PleiepengerYtelseDto.BeredskapDto.BeredskapPeriodeInfoDto(it.tilleggsinformasjon))
                    }) else null,
                nattevåk = if (!søknad.nattevaak.isNullOrEmpty() && søknad.nattevaak[0].periode != null) PleiepengerSøknadMottakDto.PleiepengerYtelseDto.NattevåkDto(
                    søknad.nattevaak.associate {
                        Pair(fromPeriodeDtoToString(it.periode!!),
                            PleiepengerSøknadMottakDto.PleiepengerYtelseDto.NattevåkDto.NattevåkPeriodeInfoDto(it.tilleggsinformasjon))
                    }) else null,
                tilsynsordning = if (!søknad.tilsynsordning?.perioder.isNullOrEmpty() && søknad.tilsynsordning?.perioder?.get(
                        0)?.periode != null
                ) PleiepengerSøknadMottakDto.PleiepengerYtelseDto.TilsynsordningDto(
                    søknad.tilsynsordning.perioder.associate {
                        Pair(fromPeriodeDtoToString(it.periode!!),
                            PleiepengerSøknadMottakDto.PleiepengerYtelseDto.TilsynsordningDto.TilsynPeriodeInfoDto(
                                zeroTimerHvisTomString(it.timer.toString())
                                    .plus(zeroMinutterHvisTomString(it.minutter.toString()))))
                    }) else null,
                lovbestemtFerie = if (!søknad.lovbestemtFerie.isNullOrEmpty() && søknad.lovbestemtFerie.first().fom != null && søknad.lovbestemtFerie.first().tom != null) PleiepengerSøknadMottakDto.PleiepengerYtelseDto.LovbestemtFerieDto(
                    søknad.lovbestemtFerie.associate {
                        Pair(fromPeriodeDtoToString(it), null)
                    }) else null,

                arbeidstid = if (erNullEllerTom(søknad.arbeidstid)) mapTilMottatArbeidstid(søknad.arbeidstid) else null,
                uttak = lagUttak(søknad),
                omsorg = if (søknad.omsorg?.relasjonTilBarnet.isNullOrEmpty()) null else mapOmsorg(søknad.omsorg!!),
                harInfoSomIkkeKanPunsjes = søknad.harInfoSomIkkeKanPunsjes,
                harMedisinskeOpplysninger = søknad.harMedisinskeOpplysninger,
            )
        }

        private fun mapOmsorg(omsorg: PleiepengerSøknadVisningDto.OmsorgDto): PleiepengerSøknadVisningDto.OmsorgDto {
            return PleiepengerSøknadVisningDto.OmsorgDto(
                omsorg.relasjonTilBarnet!!.toUpperCase(),
                omsorg.samtykketOmsorgForBarnet,
                omsorg.beskrivelseAvOmsorgsrollen)
        }

        private fun lagUttak(søknad: PleiepengerSøknadVisningDto): PleiepengerSøknadMottakDto.PleiepengerYtelseDto.UttakDto? {
            var uttakDto: PleiepengerSøknadMottakDto.PleiepengerYtelseDto.UttakDto? = null
            if (!søknad.uttak.isNullOrEmpty()) {
                uttakDto = PleiepengerSøknadMottakDto.PleiepengerYtelseDto.UttakDto(søknad.uttak.associate {
                    Pair(fromPeriodeDtoToString(it.periode!!),
                        PleiepengerSøknadMottakDto.PleiepengerYtelseDto.UttakDto.UttakPeriodeInfoDto(
                            zeroTimerHvisTomString(it.timerPleieAvBarnetPerDag)))
                })
            }
            if (uttakDto != null) {
                return uttakDto
            }
            if (søknad.soeknadsperiode?.fom == null) {
                return null
            }
            return PleiepengerSøknadMottakDto.PleiepengerYtelseDto.UttakDto(
                mapOf(Pair(fromPeriodeDtoToString(søknad.soeknadsperiode),
                    PleiepengerSøknadMottakDto.PleiepengerYtelseDto.UttakDto.UttakPeriodeInfoDto(zeroTimerHvisTomString(
                        "PT7H30M")))))
        }

        private fun erNullEllerTom(opptjeningAktivitet: PleiepengerSøknadVisningDto.ArbeidAktivitetDto?): Boolean {
            if (opptjeningAktivitet == null) {
                return false
            }

            if (opptjeningAktivitet.arbeidstaker.isNullOrEmpty()
                && (opptjeningAktivitet.frilanser?.jobberFortsattSomFrilans == null)
                && opptjeningAktivitet.selvstendigNaeringsdrivende == null
            ) {
                return false
            }
            return true
        }

        private fun erNullEllerTom(arbeidstid: PleiepengerSøknadVisningDto.ArbeidstidDto?): Boolean {
            if (arbeidstid == null) {
                return false
            }

            if (arbeidstid.arbeidstakerList.isNullOrEmpty()
                && (arbeidstid.frilanserArbeidstidInfo?.periode == null)
                && arbeidstid.selvstendigNæringsdrivendeArbeidstidInfo?.arbeidstidInfo == null
            ) {
                return false
            }
            return true
        }

        private fun zeroTimerHvisTomString(input: String?): Duration {
            if (input.isNullOrEmpty()) {
                return Duration.ofHours(0L)
            }
            try {
                return Duration.parse(input)
            } catch (e: DateTimeParseException) {
            }
            try {
                return Duration.ofHours(input.toLong())
            } catch (e: NumberFormatException) {
            }
            if (input.contains('.')) {
                val split = input.split('.')
                return Duration.ofHours(split[0].toLong()).plusMinutes(finnMinutter(split[1]))
            }
            if (input.contains(',')) {
                val split = input.split(',')
                return Duration.ofHours(split[0].toLong()).plusMinutes(finnMinutter(split[1]))
            }
            throw IllegalStateException("Klarer ikke parse $input")
        }

        private fun finnMinutter(split: String): Long {
            val div = split.toDouble().div(10)
            return div.times(60).toLong()
        }

        private fun zeroMinutterHvisTomString(input: String?): Duration {
            if (input.isNullOrEmpty()) {
                return Duration.ofHours(0L)
            }
            return Duration.ofMinutes(input.toLong())
        }

        private fun mapTilMottatArbeidstid(arbeidstidDto: PleiepengerSøknadVisningDto.ArbeidstidDto?): PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidstidDto? {
            if (arbeidstidDto == null) {
                return null
            }

            val arbeidstaker =
                if (!arbeidstidDto.arbeidstakerList.isNullOrEmpty()) arbeidstidDto.arbeidstakerList.map { arbeidstakerDto ->
                    PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto(
                        arbeidstakerDto.norskIdent,
                        arbeidstakerDto.organisasjonsnummer,
                        PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto(
                            arbeidstakerDto.arbeidstidInfo?.perioder?.associate { periodeInfoDto ->
                                Pair(fromPeriodeDtoToString(periodeInfoDto.periode!!),
                                    PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto.ArbeidstidPeriodeInfoDto(
                                        faktiskArbeidTimerPerDag = zeroTimerHvisTomString(periodeInfoDto.faktiskArbeidTimerPerDag),
                                        jobberNormaltTimerPerDag = zeroTimerHvisTomString(periodeInfoDto.jobberNormaltTimerPerDag))
                                )
                            }
                        )
                    )
                } else null

            val arbeidstidInfoFrilans = if (arbeidstidDto.frilanserArbeidstidInfo?.periode != null)
                PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto(
                    mapOf(Pair(fromPeriodeDtoToString(arbeidstidDto.frilanserArbeidstidInfo.periode),
                        PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto.ArbeidstidPeriodeInfoDto(
                            zeroTimerHvisTomString(arbeidstidDto.frilanserArbeidstidInfo.faktiskArbeidTimerPerDag),
                            zeroTimerHvisTomString(arbeidstidDto.frilanserArbeidstidInfo.faktiskArbeidTimerPerDag)
                        )))
                ) else null

            val arbeidstidInfoSn = if (arbeidstidDto.selvstendigNæringsdrivendeArbeidstidInfo?.arbeidstidInfo != null)
                PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto(
                    arbeidstidDto.selvstendigNæringsdrivendeArbeidstidInfo.arbeidstidInfo.perioder?.associate {
                        Pair(fromPeriodeDtoToString(it.periode!!),
                            PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto.ArbeidstidPeriodeInfoDto(
                                faktiskArbeidTimerPerDag = zeroTimerHvisTomString(it.faktiskArbeidTimerPerDag),
                                jobberNormaltTimerPerDag = zeroTimerHvisTomString(it.jobberNormaltTimerPerDag)))
                    }) else null

            return PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidstidDto(
                arbeidstakerList = arbeidstaker,
                frilanserArbeidstidInfo = arbeidstidInfoFrilans,
                selvstendigNæringsdrivendeArbeidstidInfo = arbeidstidInfoSn)
        }

        private fun mapTilMottakArbeidAktivitetDto(arbeidAktivitet: PleiepengerSøknadVisningDto.ArbeidAktivitetDto): PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto {
            val frilanser: PleiepengerSøknadVisningDto.ArbeidAktivitetDto.FrilanserDto? =
                if (arbeidAktivitet.frilanser != null) (objectMapper().convertValue(
                    arbeidAktivitet.frilanser)) else null
            val selvstendigNæringsdrivende =
                if (arbeidAktivitet.selvstendigNaeringsdrivende != null && arbeidAktivitet.selvstendigNaeringsdrivende.info?.periode?.fom != null)
                    mapTilMottakSelvstendigNæringsdrivendeDto(arbeidAktivitet.selvstendigNaeringsdrivende) else null
            val arbeidstaker = if (arbeidAktivitet.arbeidstaker != null) arbeidAktivitet.arbeidstaker.map { a ->
                mapTilMottakArbeidstaker(a)
            }
                .toList() else null

            return PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto(
                if (selvstendigNæringsdrivende != null) listOf(selvstendigNæringsdrivende) else null,
                frilanser,
                arbeidstaker)
        }

        private fun mapTilMottakSelvstendigNæringsdrivendeDto(s: PleiepengerSøknadVisningDto.ArbeidAktivitetDto.SelvstendigNæringsdrivendeDto): PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.SelvstendigNæringsdrivendeDto {
            val pair = Pair(fromPeriodeDtoToString(s.info?.periode!!),
                PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.SelvstendigNæringsdrivendePeriodeInfoDto(
                    virksomhetstyper = s.info.virksomhetstyper,
                    regnskapsførerNavn = s.info.regnskapsførerNavn,
                    regnskapsførerTlf = s.info.regnskapsførerTlf,
                    bruttoInntekt = if(s.info.erVarigEndring != null && s.info.erVarigEndring) s.info.endringInntekt else s.info.bruttoInntekt,
                    erNyoppstartet = s.info.periode.fom!!.isAfter(LocalDate.now().minusYears(4L)),
                    registrertIUtlandet = s.info.registrertIUtlandet,
                    landkode = s.info.landkode))

            return PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.SelvstendigNæringsdrivendeDto(
                perioder = mapOf(pair),
                organisasjonsnummer = s.organisasjonsnummer,
                virksomhetNavn = s.virksomhetNavn)
        }

        private fun mapTilMottakArbeidstaker(a: PleiepengerSøknadVisningDto.ArbeidAktivitetDto.ArbeidstakerDto): PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto {
            val perioder = a.arbeidstidInfo?.perioder?.associate {
                Pair(fromPeriodeDtoToString(it.periode!!),
                    PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto.ArbeidstidPeriodeInfoDto(
                        zeroTimerHvisTomString(it.faktiskArbeidTimerPerDag),
                        zeroTimerHvisTomString(it.jobberNormaltTimerPerDag)
                    ))
            }
            val arbeidstidInfoDto =
                PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto(
                    perioder
                )
            return PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto(a.norskIdent,
                a.organisasjonsnummer,
                arbeidstidInfoDto)
        }

        private fun fromPeriodeDtoToString(dato: PeriodeDto): String {
            val fom = if(dato.fom != null) dato.fom.toString() else ÅPEN
            val tom = if(dato.tom != null) dato.tom.toString() else ÅPEN
            return fom + SKILLE + tom
        }
    }
}



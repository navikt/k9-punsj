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
import no.nav.k9punsj.rest.web.dto.SøknadIdDto
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID


internal class SøknadMapper {
    companion object {
        const val SKILLE = "/"
        const val ÅPEN = ".."
        private val validator = Validator()

        fun mapTilEksternFormat(søknad: PleiepengerSøknadMottakDto, soeknadId: SøknadIdDto): Pair<Søknad, List<Feil>> {
            val ytelse = søknad.ytelse
            val pleiepengerSyktBarn = PleiepengerSyktBarnYtelseMapper.map(ytelse!!)

            val søknadK9Format = opprettSøknad(
                søknadId = UUID.fromString(soeknadId),
                mottattDato = søknad.mottattDato!!,
                søker = Søker(NorskIdentitetsnummer.of(søknad.søker?.norskIdentitetsnummer)),
                ytelse = pleiepengerSyktBarn
            )
            val feil = validator.valider(søknadK9Format)

            return Pair(søknadK9Format, feil)
        }

        fun mapTilSendingsformat(søknad: PleiepengerSøknadVisningDto): PleiepengerSøknadMottakDto {
            val ytelseDto = mapYtelseV2(søknad)
            return PleiepengerSøknadMottakDto(
                søker = PleiepengerSøknadMottakDto.SøkerDto(søknad.soekerId),
                mottattDato = if (søknad.mottattDato != null) ZonedDateTime.of(søknad.mottattDato,
                    LocalTime.now(),
                    ZoneId.of("UTC")) else null,
                ytelse = ytelseDto
            )
        }

        private fun mapYtelseV2(søknad: PleiepengerSøknadVisningDto): PleiepengerSøknadMottakDto.PleiepengerYtelseDto {
            return PleiepengerSøknadMottakDto.PleiepengerYtelseDto(
                barn = PleiepengerSøknadMottakDto.PleiepengerYtelseDto.BarnDto(søknad.barn?.norskIdent,
                    søknad.barn?.foedselsdato),
                søknadsperiode = if (søknad.soeknadsperiode != null) fromPeriodeDtoToString(søknad.soeknadsperiode) else null,
                opptjeningAktivitet = if (søknad.opptjeningAktivitet != null) mapTilMottakArbeidAktivitetDto(søknad.opptjeningAktivitet) else null,
                soknadsinfo = søknad.soknadsinfo,
                bosteder = PleiepengerSøknadMottakDto.PleiepengerYtelseDto.BostederDto(søknad.bosteder?.associate {
                    Pair(fromPeriodeDtoToString(it.periode!!),
                        PleiepengerSøknadMottakDto.PleiepengerYtelseDto.BostederDto.BostedPeriodeInfoDto(it.land))
                }),
                utenlandsopphold = null,
                beredskap = PleiepengerSøknadMottakDto.PleiepengerYtelseDto.BeredskapDto(søknad.beredskap?.associate {
                    Pair(fromPeriodeDtoToString(it.periode!!),
                        PleiepengerSøknadMottakDto.PleiepengerYtelseDto.BeredskapDto.BeredskapPeriodeInfoDto(it.tilleggsinformasjon))
                }),
                nattevåk = PleiepengerSøknadMottakDto.PleiepengerYtelseDto.NattevåkDto(søknad.nattevaak?.associate {
                    Pair(fromPeriodeDtoToString(it.periode!!),
                        PleiepengerSøknadMottakDto.PleiepengerYtelseDto.NattevåkDto.NattevåkPeriodeInfoDto(it.tilleggsinformasjon))
                }),
                tilsynsordning = PleiepengerSøknadMottakDto.PleiepengerYtelseDto.TilsynsordningDto(søknad.tilsynsordning?.perioder?.associate {
                    Pair(fromPeriodeDtoToString(it.periode!!),
                        PleiepengerSøknadMottakDto.PleiepengerYtelseDto.TilsynsordningDto.TilsynPeriodeInfoDto(
                            java.time.Duration.ofHours(Integer.toUnsignedLong(it.timer))
                                .plus(java.time.Duration.ofMinutes(Integer.toUnsignedLong(it.minutter))).toString()))
                }),
                lovbestemtFerie = PleiepengerSøknadMottakDto.PleiepengerYtelseDto.LovbestemtFerieDto(søknad.lovbestemtFerie?.associate {
                    Pair(fromPeriodeDtoToString(it.periode!!),
                        PleiepengerSøknadMottakDto.PleiepengerYtelseDto.LovbestemtFerieDto.TilsynLovbestemtFerieInfoDto(
                            it.land))
                }),
                arbeidstid = mapTilMottatArbeidstid(søknad.arbeidstid),
                uttak = PleiepengerSøknadMottakDto.PleiepengerYtelseDto.UttakDto(søknad.uttak?.associate {
                    Pair(fromPeriodeDtoToString(it.periode!!),
                        PleiepengerSøknadMottakDto.PleiepengerYtelseDto.UttakDto.UttakPeriodeInfoDto(it.timerPleieAvBarnetPerDag))
                }),
                omsorg = søknad.omsorg
            )
        }

        private fun mapTilMottatArbeidstid(arbeidstidDto: PleiepengerSøknadVisningDto.ArbeidstidDto?): PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidstidDto? {
            if (arbeidstidDto == null) {
                return null
            }

            val arbeidstaker =
                if (arbeidstidDto.arbeidstakerList != null) arbeidstidDto.arbeidstakerList.map { arbeidstakerDto ->
                    PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto(
                        arbeidstakerDto.norskIdent,
                        arbeidstakerDto.organisasjonsnummer,
                        PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto(
                            arbeidstakerDto.arbeidstidInfo?.perioder?.associate { periodeInfoDto ->
                                Pair(fromPeriodeDtoToString(periodeInfoDto.periode!!),
                                    PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto.ArbeidstidPeriodeInfoDto(
                                        faktiskArbeidTimerPerDag = periodeInfoDto.faktiskArbeidTimerPerDag,
                                        jobberNormaltTimerPerDag = arbeidstakerDto.arbeidstidInfo.jobberNormaltTimerPerDag))
                            }
                        )
                    )
                } else null

            val arbeidstidInfoFrilans = if (arbeidstidDto.frilanserArbeidstidInfo != null)
                PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto(
                    mapOf(Pair(fromPeriodeDtoToString(arbeidstidDto.frilanserArbeidstidInfo.periode!!),
                        PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto.ArbeidstidPeriodeInfoDto(
                            arbeidstidDto.frilanserArbeidstidInfo.faktiskArbeidTimerPerDag,
                            arbeidstidDto.frilanserArbeidstidInfo.faktiskArbeidTimerPerDag
                        )))
                ) else null

            val arbeidstidInfoSn = if (arbeidstidDto.selvstendigNæringsdrivendeArbeidstidInfo != null)
                PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto(
                    arbeidstidDto.selvstendigNæringsdrivendeArbeidstidInfo.arbeidstidInfo?.perioder?.associate {
                        Pair(fromPeriodeDtoToString(it.periode!!),
                            PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto.ArbeidstidPeriodeInfoDto(
                                faktiskArbeidTimerPerDag = it.faktiskArbeidTimerPerDag,
                                jobberNormaltTimerPerDag = arbeidstidDto.selvstendigNæringsdrivendeArbeidstidInfo.arbeidstidInfo.jobberNormaltTimerPerDag))
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
            val selvstendigNæringsdrivende = if (arbeidAktivitet.selvstendigNæringsdrivende != null)
                arbeidAktivitet.selvstendigNæringsdrivende.map { s -> mapTilMottakSelvstendigNæringsdrivendeDto(s) }
                    .toList() else null
            val arbeidstaker = if (arbeidAktivitet.arbeidstaker != null) arbeidAktivitet.arbeidstaker.map { a ->
                mapTilMottakArbeidstaker(a)
            }
                .toList() else null

            return PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto(
                selvstendigNæringsdrivende,
                frilanser,
                arbeidstaker)
        }

        private fun mapTilMottakSelvstendigNæringsdrivendeDto(s: PleiepengerSøknadVisningDto.ArbeidAktivitetDto.SelvstendigNæringsdrivendeDto): PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.SelvstendigNæringsdrivendeDto {
            val perioder = s.perioder?.associate {
                Pair(fromPeriodeDtoToString(it.periode!!),
                    PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.SelvstendigNæringsdrivendePeriodeInfoDto(
                        virksomhetstyper = it.virksomhetstyper,
                        regnskapsførerNavn = it.regnskapsførerNavn,
                        regnskapsførerTlf = it.regnskapsførerTlf,
                        bruttoInntekt = it.bruttoInntekt,
                        erNyoppstartet = it.erNyoppstartet,
                        registrertIUtlandet = it.registrertIUtlandet,
                        landkode = it.landkode))
            }
            return PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.SelvstendigNæringsdrivendeDto(
                perioder = perioder,
                organisasjonsnummer = s.organisasjonsnummer,
                virksomhetNavn = s.virksomhetNavn)
        }

        private fun mapTilMottakArbeidstaker(a: PleiepengerSøknadVisningDto.ArbeidAktivitetDto.ArbeidstakerDto): PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto {
            val perioder = a.arbeidstidInfo?.perioder?.associate {
                Pair(fromPeriodeDtoToString(it.periode!!),
                    PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto.ArbeidstidPeriodeInfoDto(
                        it.faktiskArbeidTimerPerDag, a.arbeidstidInfo.jobberNormaltTimerPerDag))
            }
            val arbeidstidInfoDto =
                PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto(
                    perioder
                )
            return PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto(a.norskIdent,
                a.organisasjonsnummer,
                arbeidstidInfoDto)
        }


        private fun opprettSøknad(
            søknadId: UUID,
            // TODO(OJR) hva skal versjonen være her? bruke samme som k9-format?
            versjon: Versjon = Versjon.of("1.0.0"),
            mottattDato: ZonedDateTime,
            søker: Søker,
            ytelse: no.nav.k9.søknad.ytelse.Ytelse,
        ): Søknad {
            return Søknad(SøknadId.of(søknadId.toString()), versjon, mottattDato, søker, ytelse)
        }

        private fun fromPeriodeDtoToString(dato: PeriodeDto): String {
            return dato.fom.toString() + SKILLE + dato.tom.toString()
        }
    }
}



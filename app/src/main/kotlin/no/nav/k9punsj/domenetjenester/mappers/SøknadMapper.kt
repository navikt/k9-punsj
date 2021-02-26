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

        fun mapTilEksternFormat(søknad: PleiepengerSøknadMottakDto): Pair<Søknad, List<Feil>> {
            val ytelse = søknad.ytelse
            val pleiepengerSyktBarn = PleiepengerSyktBarnYtelseMapper.map(ytelse!!)

            val søknadK9Format = opprettSøknad(
                søker = Søker(NorskIdentitetsnummer.of(søknad.søker?.norskIdentitetsnummer)),
                ytelse = pleiepengerSyktBarn
            )
            val feil = validator.valider(søknadK9Format)

            return Pair(søknadK9Format, feil)
        }

        fun mapTilVisningFormat(søknad: PleiepengerSøknadMottakDto): PleiepengerSøknadVisningDto {
            val ytelse = if (søknad.ytelse != null) mapYtelse(søknad.ytelse) else null
            return PleiepengerSøknadVisningDto(
                søker = søknad.søker,
                mottattDato = søknad.mottattDato,
                ytelse = ytelse
            )
        }

        fun mapTilMapFormat(søknad: PleiepengerSøknadVisningDto): PleiepengerSøknadMottakDto {
            val ytelse = if (søknad.ytelse != null) mapYtelse(søknad.ytelse) else null
            return PleiepengerSøknadMottakDto(
                søker = søknad.søker,
                mottattDato = søknad.mottattDato,
                ytelse = ytelse
            )
        }

        private fun mapYtelse(ytelse: PleiepengerYtelseDto): PleiepengerSøknadMottakDto.PleiepengerYtelseDto {
            val søknadsperiode: String? =
                if (ytelse.søknadsperiode != null) fromPeriodeDtoToString(ytelse.søknadsperiode) else null
            val arbeidAktivitet: PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto? =
                if (ytelse.arbeidAktivitet != null) mapTilMottakArbeidAktivitetDto(ytelse.arbeidAktivitet) else null

            val bosteder = if(ytelse.bosteder!= null)PleiepengerSøknadMottakDto.PleiepengerYtelseDto.BostederDto(
                ytelse.bosteder.associate {
                    Pair(fromPeriodeDtoToString(it.periode), PleiepengerSøknadMottakDto.PleiepengerYtelseDto.BostederDto.BostedPeriodeInfoDto(it.land)
                    )
                }) else null

            val utenlandsoppholdDto = if(ytelse.utenlandsopphold!= null)PleiepengerSøknadMottakDto.PleiepengerYtelseDto.UtenlandsoppholdDto(
                ytelse.utenlandsopphold.associate {
                    Pair(fromPeriodeDtoToString(it.periode), PleiepengerSøknadMottakDto.PleiepengerYtelseDto.UtenlandsoppholdDto.UtenlandsoppholdPeriodeInfoDto(it.land, it.årsak)
                    )
                }) else null

            val beredskapDto = if(ytelse.beredskap!= null)PleiepengerSøknadMottakDto.PleiepengerYtelseDto.BeredskapDto(
                ytelse.beredskap.associate {
                    Pair(fromPeriodeDtoToString(it.periode), PleiepengerSøknadMottakDto.PleiepengerYtelseDto.BeredskapDto.BeredskapPeriodeInfoDto(it.tilleggsinformasjon)
                    )
                }) else null

            val nattevåkDto = if(ytelse.nattevåk!= null)PleiepengerSøknadMottakDto.PleiepengerYtelseDto.NattevåkDto(
                ytelse.nattevåk.associate {
                    Pair(fromPeriodeDtoToString(it.periode), PleiepengerSøknadMottakDto.PleiepengerYtelseDto.NattevåkDto.NattevåkPeriodeInfoDto(it.tilleggsinformasjon)
                    )
                }) else null

            val tilsynsordningDto = if(ytelse.tilsynsordning!= null)PleiepengerSøknadMottakDto.PleiepengerYtelseDto.TilsynsordningDto(
                ytelse.tilsynsordning.associate {
                    Pair(fromPeriodeDtoToString(it.periode), PleiepengerSøknadMottakDto.PleiepengerYtelseDto.TilsynsordningDto.TilsynPeriodeInfoDto(it.etablertTilsynTimerPerDag)
                    )
                }) else null

            val uttakDto = if(ytelse.uttak!= null)PleiepengerSøknadMottakDto.PleiepengerYtelseDto.UttakDto(
                ytelse.uttak.associate {
                    Pair(fromPeriodeDtoToString(it.periode), PleiepengerSøknadMottakDto.PleiepengerYtelseDto.UttakDto.UttakPeriodeInfoDto(it.timerPleieAvBarnetPerDag)
                    )
                }) else null

            val lovbestemtFerieDto =
                if (ytelse.lovbestemtFerie != null) PleiepengerSøknadMottakDto.PleiepengerYtelseDto.LovbestemtFerieDto(
                    ytelse.lovbestemtFerie.perioder?.map { fromPeriodeDtoToString(it) }?.toList()) else null

            return PleiepengerSøknadMottakDto.PleiepengerYtelseDto(
                barn = ytelse.barn,
                søknadsperiode = søknadsperiode,
                arbeidAktivitet = arbeidAktivitet,
                dataBruktTilUtledning = ytelse.dataBruktTilUtledning,
                bosteder = bosteder,
                utenlandsopphold = utenlandsoppholdDto,
                beredskap = beredskapDto,
                nattevåk = nattevåkDto,
                tilsynsordning = tilsynsordningDto,
                lovbestemtFerie = lovbestemtFerieDto,
                arbeidstid = mapTilMottatArbeidstid(ytelse.arbeidstid),
                uttak = uttakDto,
                omsorg = ytelse.omsorg
            )
        }

        private fun mapYtelse(ytelse: PleiepengerSøknadMottakDto.PleiepengerYtelseDto): PleiepengerYtelseDto {
            val søknadsperiode: PeriodeDto? =
                if (ytelse.søknadsperiode != null) fromDatoStringToPeriodeDto(ytelse.søknadsperiode) else null
            val arbeidAktivitet: ArbeidAktivitetDto? =
                if (ytelse.arbeidAktivitet != null) mapArbeidAktivitetDto(ytelse.arbeidAktivitet) else null
            val bosteder: List<BostederDto>? = if (ytelse.bosteder != null) ytelse.bosteder.perioder?.map { entry ->
                BostederDto(fromDatoStringToPeriodeDto(entry.key),
                    entry.value.land)
            }?.toList() else null
            val utenlandsoppholdDto: List<UtenlandsoppholdDto>? =
                if (ytelse.utenlandsopphold != null) ytelse.utenlandsopphold.perioder?.map { entry ->
                    UtenlandsoppholdDto(fromDatoStringToPeriodeDto(entry.key),
                        entry.value.land,
                        entry.value.årsak)
                }?.toList() else null
            val beredskapDto: List<BeredskapDto>? =
                if (ytelse.beredskap != null) ytelse.beredskap.perioder?.map { entry ->
                    BeredskapDto(fromDatoStringToPeriodeDto(entry.key),
                        entry.value.tilleggsinformasjon)
                }?.toList() else null
            val nattevåkDto: List<NattevåkDto>? = if (ytelse.nattevåk != null) ytelse.nattevåk.perioder?.map { entry ->
                NattevåkDto(fromDatoStringToPeriodeDto(entry.key),
                    entry.value.tilleggsinformasjon)
            }?.toList() else null
            val tilsynsordningDto: List<TilsynsordningDto>? =
                if (ytelse.tilsynsordning != null) ytelse.tilsynsordning.perioder?.map { entry ->
                    TilsynsordningDto(fromDatoStringToPeriodeDto(entry.key),
                        entry.value.etablertTilsynTimerPerDag)
                }?.toList() else null
            val lovbestemtFerieDto: LovbestemtFerieDto? =
                if (ytelse.lovbestemtFerie != null) LovbestemtFerieDto(ytelse.lovbestemtFerie.perioder?.map { entry ->
                    (fromDatoStringToPeriodeDto(entry))
                }?.toList()) else null
            val uttakDto: List<UttakDto>? = if (ytelse.uttak != null) ytelse.uttak.perioder.map { entry ->
                UttakDto(fromDatoStringToPeriodeDto(entry.key),
                    entry.value.timerPleieAvBarnetPerDag)
            }.toList() else null
            ytelse.arbeidstid

            return PleiepengerYtelseDto(
                barn = ytelse.barn,
                søknadsperiode = søknadsperiode,
                arbeidAktivitet = arbeidAktivitet,
                dataBruktTilUtledning = ytelse.dataBruktTilUtledning,
                bosteder = bosteder,
                utenlandsopphold = utenlandsoppholdDto,
                beredskap = beredskapDto,
                nattevåk = nattevåkDto,
                tilsynsordning = tilsynsordningDto,
                lovbestemtFerie = lovbestemtFerieDto,
                arbeidstid = mapArbeidstid(ytelse.arbeidstid),
                uttak = uttakDto,
                omsorg = ytelse.omsorg
            )
        }

        private fun mapArbeidstid(arbeidstidDto: PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidstidDto?): ArbeidstidDto? {
            val arbeidstaker = arbeidstidDto?.arbeidstakerList?.map { a ->
                ArbeidAktivitetDto.ArbeidstakerDto(
                    a.norskIdentitetsnummer,
                    a.organisasjonsnummer,
                    ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto(a.arbeidstidInfo.jobberNormaltTimerPerDag,
                        a.arbeidstidInfo.perioder?.map { p ->
                            ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto.ArbeidstidPeriodeInfoDto(
                                fromDatoStringToPeriodeDto(p.key), p.value.faktiskArbeidTimerPerDag)
                        }))
            }

            val arbeidstidInfoFrilans =
                ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto(arbeidstidDto?.frilanserArbeidstidInfo?.jobberNormaltTimerPerDag,
                    arbeidstidDto?.frilanserArbeidstidInfo?.perioder?.map { p ->
                        ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto.ArbeidstidPeriodeInfoDto(
                            fromDatoStringToPeriodeDto(p.key), p.value.faktiskArbeidTimerPerDag)
                    })

            val arbeidstidInfoSn =
                ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto(arbeidstidDto?.selvstendigNæringsdrivendeArbeidstidInfo?.jobberNormaltTimerPerDag,
                    arbeidstidDto?.selvstendigNæringsdrivendeArbeidstidInfo?.perioder?.map { p ->
                        ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto.ArbeidstidPeriodeInfoDto(
                            fromDatoStringToPeriodeDto(p.key), p.value.faktiskArbeidTimerPerDag)
                    })

            return ArbeidstidDto(
                arbeidstakerList = arbeidstaker,
                frilanserArbeidstidInfo = arbeidstidInfoFrilans,
                selvstendigNæringsdrivendeArbeidstidInfo = arbeidstidInfoSn)

        }

        private fun mapTilMottatArbeidstid(arbeidstidDto: ArbeidstidDto?): PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidstidDto? {
            val arbeidstaker = arbeidstidDto?.arbeidstakerList?.map { a ->
                PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto(
                    a.norskIdentitetsnummer,
                    a.organisasjonsnummer,
                    PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto(
                        a.arbeidstidInfo.jobberNormaltTimerPerDag,
                        a.arbeidstidInfo.perioder?.associate {
                            Pair(fromPeriodeDtoToString(it.periode), PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto.ArbeidstidPeriodeInfoDto(it.faktiskArbeidTimerPerDag))
                        }
                    )
                )
            }

            val arbeidstidInfoFrilans =
                PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto(
                    arbeidstidDto?.frilanserArbeidstidInfo?.jobberNormaltTimerPerDag,
                    arbeidstidDto?.frilanserArbeidstidInfo?.perioder?.associate {
                        Pair(fromPeriodeDtoToString(it.periode), PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto.ArbeidstidPeriodeInfoDto(it.faktiskArbeidTimerPerDag))
                    })

            val arbeidstidInfoSn =
                PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto(
                    arbeidstidDto?.selvstendigNæringsdrivendeArbeidstidInfo?.jobberNormaltTimerPerDag,
                    arbeidstidDto?.selvstendigNæringsdrivendeArbeidstidInfo?.perioder?.associate {
                        Pair(fromPeriodeDtoToString(it.periode), PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto.ArbeidstidPeriodeInfoDto(it.faktiskArbeidTimerPerDag))
                    })

            return PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidstidDto(
                arbeidstakerList = arbeidstaker,
                frilanserArbeidstidInfo = arbeidstidInfoFrilans,
                selvstendigNæringsdrivendeArbeidstidInfo = arbeidstidInfoSn)
        }


        private fun mapArbeidAktivitetDto(arbeidAktivitet: PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto?): ArbeidAktivitetDto {
            val frilanser: ArbeidAktivitetDto.FrilanserDto? = objectMapper().convertValue(arbeidAktivitet?.frilanser!!)
            val selvstendigNæringsdrivende =
                arbeidAktivitet.selvstendigNæringsdrivende?.map { s -> mapTilVisningSelvstendigNæringsdrivendeDto(s) }?.toList()
            val arbeidstaker = arbeidAktivitet.arbeidstaker?.map { a -> mapArbeidstaker(a) }?.toList()
            return ArbeidAktivitetDto(selvstendigNæringsdrivende, frilanser, arbeidstaker)
        }

        private fun mapTilMottakArbeidAktivitetDto(arbeidAktivitet: ArbeidAktivitetDto?): PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto {
            val frilanser: ArbeidAktivitetDto.FrilanserDto? = objectMapper().convertValue(arbeidAktivitet?.frilanser!!)
            val selvstendigNæringsdrivende =
                arbeidAktivitet.selvstendigNæringsdrivende?.map { s -> mapTilMottakSelvstendigNæringsdrivendeDto(s) }?.toList()
            val arbeidstaker = arbeidAktivitet.arbeidstaker?.map { a -> mapTilMottakArbeidstaker(a) }?.toList()

            return PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto(selvstendigNæringsdrivende, frilanser,  arbeidstaker)
        }

        private fun mapTilVisningSelvstendigNæringsdrivendeDto(s: PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.SelvstendigNæringsdrivendeDto): ArbeidAktivitetDto.SelvstendigNæringsdrivendeDto {
            return ArbeidAktivitetDto.SelvstendigNæringsdrivendeDto(s.perioder?.map { entry ->
                ArbeidAktivitetDto.SelvstendigNæringsdrivendeDto.SelvstendigNæringsdrivendePeriodeInfoDto(
                    periode = fromDatoStringToPeriodeDto(entry.key),
                    virksomhetstyper = entry.value.virksomhetstyper,
                    regnskapsførerNavn = entry.value.regnskapsførerNavn,
                    regnskapsførerTlf = entry.value.regnskapsførerTlf,
                    erVarigEndring = entry.value.erVarigEndring,
                    endringDato = entry.value.endringDato,
                    endringBegrunnelse = entry.value.endringBegrunnelse,
                    bruttoInntekt = entry.value.bruttoInntekt,
                    erNyoppstartet = entry.value.erNyoppstartet,
                    registrertIUtlandet = entry.value.registrertIUtlandet,
                    landkode = entry.value.landkode
                )
            }?.toList(), s.organisasjonsnummer, s.virksomhetNavn)
        }

        private fun mapTilMottakSelvstendigNæringsdrivendeDto(s: ArbeidAktivitetDto.SelvstendigNæringsdrivendeDto): PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.SelvstendigNæringsdrivendeDto {
            val perioder = s.perioder?.associate {
                Pair(fromPeriodeDtoToString(it.periode),
                    PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.SelvstendigNæringsdrivendePeriodeInfoDto(
                        virksomhetstyper = it.virksomhetstyper,
                        regnskapsførerNavn = it.regnskapsførerNavn,
                        regnskapsførerTlf = it.regnskapsførerTlf,
                        erVarigEndring = it.erVarigEndring,
                        endringDato = it.endringDato,
                        endringBegrunnelse = it.endringBegrunnelse,
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

        private fun mapArbeidstaker(a: PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto): ArbeidAktivitetDto.ArbeidstakerDto {
            val arbeidstidInfoDto = ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto(
                a.arbeidstidInfo.jobberNormaltTimerPerDag,
                a.arbeidstidInfo.perioder?.map { entry ->
                    ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto.ArbeidstidPeriodeInfoDto(
                        fromDatoStringToPeriodeDto(entry.key), entry.value.faktiskArbeidTimerPerDag)
                }?.toList()
            )

            return ArbeidAktivitetDto.ArbeidstakerDto(a.norskIdentitetsnummer, a.organisasjonsnummer, arbeidstidInfoDto)
        }

        private fun mapTilMottakArbeidstaker(a: ArbeidAktivitetDto.ArbeidstakerDto): PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto{
            val perioder = a.arbeidstidInfo.perioder?.associate {
                Pair(fromPeriodeDtoToString(it.periode),
                    PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto.ArbeidstidPeriodeInfoDto(
                        it.faktiskArbeidTimerPerDag))
            }
            val arbeidstidInfoDto = PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto(
                a.arbeidstidInfo.jobberNormaltTimerPerDag,
                perioder
            )
            return PleiepengerSøknadMottakDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto(a.norskIdentitetsnummer, a.organisasjonsnummer, arbeidstidInfoDto)
        }


        private fun opprettSøknad(
            søknadId: UUID = UUID.randomUUID(),
            versjon: Versjon = Versjon.of("1.0.0"),
            mottattDato: ZonedDateTime = ZonedDateTime.now(),
            søker: Søker,
            ytelse: no.nav.k9.søknad.ytelse.Ytelse,
        ): Søknad {
            return Søknad(SøknadId.of(søknadId.toString()), versjon, mottattDato, søker, ytelse)
        }

        private fun fromDatoStringToPeriodeDto(dato: String): PeriodeDto {
            verifiserKanVæreGyldigPeriode(dato)
            val split: Array<String> = dato.split(SKILLE.toRegex()).toTypedArray()
            val fom = parseLocalDate(split[0])
            val tom = parseLocalDate(split[1])
            return PeriodeDto(fom!!, tom!!)
        }

        private fun fromPeriodeDtoToString(dato: PeriodeDto): String {
            return dato.fom.toString() + SKILLE + dato.tom.toString()
        }

        private fun verifiserKanVæreGyldigPeriode(dato: String) {
            require(dato.split(SKILLE.toRegex()).toTypedArray().size == 2) { "Periode på ugylig format '$dato'." }
        }

        private fun parseLocalDate(dato: String): LocalDate? {
            return if (ÅPEN == dato) null else LocalDate.parse(dato)
        }

    }
}



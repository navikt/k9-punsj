package no.nav.k9punsj.util

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn
import no.nav.k9punsj.domenetjenester.mappers.MapFraVisningTilEksternFormat
import no.nav.k9punsj.domenetjenester.mappers.MapTilK9Format
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.dto.PeriodeDto
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadVisningDto
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

internal object PleiepengerSøknadVisningDtoUtils {

    internal fun MutableMap<String, Any?>.somPleiepengerSøknadVisningDto(manipuler: (MutableMap<String, Any?>) -> Unit)
        = manipuler(this).let { objectMapper().convertValue<PleiepengerSøknadVisningDto>(this) }

    internal fun minimalSøknadSomValiderer(
        søker: String = "11111111111",
        barn: String = "22222222222",
        søknadsperiode: Pair<LocalDate, LocalDate>? = null,
        manipuler: (MutableMap<String, Any?>) -> Unit = {}
    ) : PleiepengerSøknadVisningDto {
        @Language("JSON")
        val json = """
            {
              "soeknadId": "${UUID.randomUUID()}",
              "mottattDato": "${LocalDate.now()}",
              "klokkeslett": "${LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))}",
              "soekerId": "$søker",
              "barn": {
                "norskIdent": "$barn"
              },
              "journalposter": [
                "12345678"
              ]
            }
            """
        val søknad: MutableMap<String, Any?> = objectMapper().readValue(json)
        søknadsperiode?.also {
            søknad["soeknadsperiode"] = mapOf("fom" to "${it.first}", "tom" to "${it.second}")
        }
        manipuler(søknad)
        return objectMapper().convertValue(søknad)
    }

    internal fun PleiepengerSøknadVisningDto.mapTilSendingsFormat() =
        MapFraVisningTilEksternFormat.mapTilSendingsformat(this)

    internal fun PleiepengerSøknadVisningDto.mapTilK9Format(
        perioderSomFinnesIK9: List<PeriodeDto> = emptyList()) =
        MapTilK9Format.mapTilEksternFormat(
            søknad = mapTilSendingsFormat(),
            soeknadId = soeknadId,
            perioderSomFinnesIK9 = perioderSomFinnesIK9,
            journalpostIder = journalposter?.toSet() ?: emptySet()
        ).let { it.first.getYtelse<PleiepengerSyktBarn>() to it.second }


    private fun arbeidstidInfoKomplettStruktur(
        optionalTekst: String?,
        periode: PeriodeDto?
    ) = PleiepengerSøknadVisningDto.ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto(
        perioder = listOf(
            PleiepengerSøknadVisningDto.ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto.ArbeidstidPeriodeInfoDto(
                periode = periode,
                faktiskArbeidTimerPerDag = optionalTekst,
                jobberNormaltTimerPerDag = optionalTekst
            ))
    )

    internal fun søknadMedKomplettStruktur(
        requiredTekst: String = "",
        optionalTekst: String? = "",
        requiredPeriode: PeriodeDto,
        optionalPeriode: PeriodeDto?
    ) = PleiepengerSøknadVisningDto(
        soeknadId = "${UUID.randomUUID()}",
        soekerId = "11111111111",
        journalposter = listOf(requiredTekst),
        mottattDato = null,
        klokkeslett = null,
        barn = PleiepengerSøknadVisningDto.BarnDto(
            norskIdent = optionalTekst,
            foedselsdato = null
        ),
        soeknadsperiode = optionalPeriode,
        opptjeningAktivitet = PleiepengerSøknadVisningDto.ArbeidAktivitetDto(
            selvstendigNaeringsdrivende = PleiepengerSøknadVisningDto.ArbeidAktivitetDto.SelvstendigNæringsdrivendeDto(
                organisasjonsnummer = optionalTekst,
                virksomhetNavn = optionalTekst,
                info = PleiepengerSøknadVisningDto.ArbeidAktivitetDto.SelvstendigNæringsdrivendeDto.SelvstendigNæringsdrivendePeriodeInfoDto(
                    periode = optionalPeriode,
                    virksomhetstyper = listOf(requiredTekst),
                    registrertIUtlandet = null,
                    landkode = optionalTekst,
                    regnskapsførerNavn = optionalTekst,
                    regnskapsførerTlf = optionalTekst,
                    bruttoInntekt = null,
                    erNyoppstartet = null,
                    erVarigEndring = null,
                    endringInntekt = null,
                    endringDato = null,
                    endringBegrunnelse = optionalTekst
                )
            ),
            arbeidstaker = listOf(
                PleiepengerSøknadVisningDto.ArbeidAktivitetDto.ArbeidstakerDto(
                    arbeidstidInfo = arbeidstidInfoKomplettStruktur(optionalTekst, optionalPeriode),
                    norskIdent = optionalTekst,
                    organisasjonsnummer = optionalTekst
                )),
            frilanser = PleiepengerSøknadVisningDto.ArbeidAktivitetDto.FrilanserDto(
                startdato = optionalTekst,
                sluttdato = optionalTekst,
                jobberFortsattSomFrilans = null
            )
        ),
        arbeidstid = PleiepengerSøknadVisningDto.ArbeidstidDto(
            arbeidstakerList = listOf(
                PleiepengerSøknadVisningDto.ArbeidAktivitetDto.ArbeidstakerDto(
                    norskIdent = optionalTekst,
                    organisasjonsnummer = optionalTekst,
                    arbeidstidInfo = arbeidstidInfoKomplettStruktur(optionalTekst, optionalPeriode)
                )),
            frilanserArbeidstidInfo = arbeidstidInfoKomplettStruktur(optionalTekst, optionalPeriode),
            selvstendigNæringsdrivendeArbeidstidInfo = arbeidstidInfoKomplettStruktur(optionalTekst, optionalPeriode)
        ),
        beredskap = listOf(
            PleiepengerSøknadVisningDto.BeredskapDto(
                periode = optionalPeriode,
                tilleggsinformasjon = optionalTekst
            )
        ),
        nattevaak = listOf(
            PleiepengerSøknadVisningDto.NattevåkDto(
                periode = optionalPeriode,
                tilleggsinformasjon =  optionalTekst
            )
        ),
        tilsynsordning = PleiepengerSøknadVisningDto.TilsynsordningDto(
            perioder = listOf(
                PleiepengerSøknadVisningDto.TilsynsordningDto.TilsynsordningPeriodeInfoDto(
                    periode = optionalPeriode,
                    timer = 0,
                    minutter = 0
                )
            )
        ),
        uttak = listOf(PleiepengerSøknadVisningDto.UttakDto(
            periode = optionalPeriode,
            timerPleieAvBarnetPerDag = optionalTekst
        )),
        omsorg = PleiepengerSøknadVisningDto.OmsorgDto(
            relasjonTilBarnet = optionalTekst,
            samtykketOmsorgForBarnet = null,
            beskrivelseAvOmsorgsrollen = optionalTekst
        ),
        bosteder = listOf(PleiepengerSøknadVisningDto.BostederDto(
            periode = optionalPeriode,
            land = optionalTekst
        )),
        lovbestemtFerie = listOf(requiredPeriode),
        lovbestemtFerieSomSkalSlettes = listOf(requiredPeriode),
        soknadsinfo = PleiepengerSøknadVisningDto.DataBruktTilUtledningDto(
            samtidigHjemme = null,
            harMedsoeker = null
        ),
        utenlandsopphold = listOf(
            PleiepengerSøknadVisningDto.UtenlandsoppholdDto(
                periode = optionalPeriode,
                land = optionalTekst,
                årsak = optionalTekst
            )
        ),
        harInfoSomIkkeKanPunsjes = true,
        harMedisinskeOpplysninger = true,
        trekkKravPerioder = setOf(requiredPeriode)
    )

    init {
        val k9Feil = minimalSøknadSomValiderer(
            søknadsperiode = LocalDate.now() to LocalDate.now().plusWeeks(1)
        ).mapTilK9Format(emptyList()).second
        check(k9Feil.isEmpty()) {
            "Minimal søknad mangler felter. Feil=$k9Feil"
        }
    }
}
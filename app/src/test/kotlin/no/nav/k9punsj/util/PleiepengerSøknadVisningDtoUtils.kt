package no.nav.k9punsj.util

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.k9.søknad.felles.type.BegrunnelseForInnsending
import no.nav.k9punsj.domenetjenester.mappers.MapPsbTilK9Format
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.dto.PeriodeDto
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadDto
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

internal object PleiepengerSøknadVisningDtoUtils {

    private fun minimalSøknadSomValiderer(
        søker: String = "11111111111",
        barn: String = "22222222222",
        søknadsperiode: Pair<LocalDate, LocalDate>? = null,
        manipuler: (MutableMap<String, Any?>) -> Unit = {}
    ) : PleiepengerSøknadDto {
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
            val (fom, tom) = it
            søknad["soeknadsperiode"] = listOf(mapOf("fom" to "$fom", "tom" to "$tom"))
        }
        manipuler(søknad)
        return objectMapper().convertValue(søknad)
    }

    private fun PleiepengerSøknadDto.mapTilK9Format(
        perioderSomFinnesIK9: List<PeriodeDto> = emptyList()) =
        MapPsbTilK9Format(
            søknadId = soeknadId,
            perioderSomFinnesIK9 = perioderSomFinnesIK9,
            journalpostIder = journalposter?.toSet() ?: emptySet(),
            dto = this
        ).søknadOgFeil()


    private fun arbeidstidInfoKomplettStruktur(
        optionalTekst: String?,
        periode: PeriodeDto?
    ) = PleiepengerSøknadDto.ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto(
        perioder = listOf(
            PleiepengerSøknadDto.ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto.ArbeidstidPeriodeInfoDto(
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
    ) = PleiepengerSøknadDto(
        soeknadId = "${UUID.randomUUID()}",
        soekerId = "11111111111",
        journalposter = listOf(requiredTekst),
        mottattDato = null,
        klokkeslett = null,
        barn = PleiepengerSøknadDto.BarnDto(
            norskIdent = optionalTekst,
            foedselsdato = null
        ),
        soeknadsperiode = optionalTilPeriode(optionalPeriode),
        opptjeningAktivitet = PleiepengerSøknadDto.ArbeidAktivitetDto(
            selvstendigNaeringsdrivende = PleiepengerSøknadDto.ArbeidAktivitetDto.SelvstendigNæringsdrivendeDto(
                organisasjonsnummer = optionalTekst,
                virksomhetNavn = optionalTekst,
                info = PleiepengerSøknadDto.ArbeidAktivitetDto.SelvstendigNæringsdrivendeDto.SelvstendigNæringsdrivendePeriodeInfoDto(
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
                PleiepengerSøknadDto.ArbeidAktivitetDto.ArbeidstakerDto(
                    arbeidstidInfo = arbeidstidInfoKomplettStruktur(optionalTekst, optionalPeriode),
                    norskIdent = optionalTekst,
                    organisasjonsnummer = optionalTekst
                )),
            frilanser = PleiepengerSøknadDto.ArbeidAktivitetDto.FrilanserDto(
                startdato = optionalTekst,
                sluttdato = optionalTekst,
                jobberFortsattSomFrilans = null
            )
        ),
        arbeidstid = PleiepengerSøknadDto.ArbeidstidDto(
            arbeidstakerList = listOf(
                PleiepengerSøknadDto.ArbeidAktivitetDto.ArbeidstakerDto(
                    norskIdent = optionalTekst,
                    organisasjonsnummer = optionalTekst,
                    arbeidstidInfo = arbeidstidInfoKomplettStruktur(optionalTekst, optionalPeriode)
                )),
            frilanserArbeidstidInfo = arbeidstidInfoKomplettStruktur(optionalTekst, optionalPeriode),
            selvstendigNæringsdrivendeArbeidstidInfo = arbeidstidInfoKomplettStruktur(optionalTekst, optionalPeriode)
        ),
        beredskap = listOf(
            PleiepengerSøknadDto.BeredskapDto(
                periode = optionalPeriode,
                tilleggsinformasjon = optionalTekst
            )
        ),
        nattevaak = listOf(
            PleiepengerSøknadDto.NattevåkDto(
                periode = optionalPeriode,
                tilleggsinformasjon =  optionalTekst
            )
        ),
        tilsynsordning = PleiepengerSøknadDto.TilsynsordningDto(
            perioder = listOf(
                PleiepengerSøknadDto.TilsynsordningInfoDto(
                    periode = optionalPeriode,
                    timer = 0,
                    minutter = 0
                )
            )
        ),
        uttak = listOf(PleiepengerSøknadDto.UttakDto(
            periode = optionalPeriode,
            timerPleieAvBarnetPerDag = optionalTekst
        )),
        omsorg = PleiepengerSøknadDto.OmsorgDto(
            relasjonTilBarnet = optionalTekst,
            samtykketOmsorgForBarnet = null,
            beskrivelseAvOmsorgsrollen = optionalTekst
        ),
        bosteder = listOf(PleiepengerSøknadDto.BostederDto(
            periode = optionalPeriode,
            land = optionalTekst
        )),
        lovbestemtFerie = listOf(requiredPeriode),
        lovbestemtFerieSomSkalSlettes = listOf(requiredPeriode),
        soknadsinfo = PleiepengerSøknadDto.DataBruktTilUtledningDto(
            samtidigHjemme = null,
            harMedsoeker = null
        ),
        utenlandsopphold = listOf(
            PleiepengerSøknadDto.UtenlandsoppholdDto(
                periode = optionalPeriode,
                land = optionalTekst,
                årsak = optionalTekst
            )
        ),
        harInfoSomIkkeKanPunsjes = true,
        harMedisinskeOpplysninger = true,
        trekkKravPerioder = setOf(requiredPeriode),
        begrunnelseForInnsending = BegrunnelseForInnsending().medBegrunnelseForInnsending("fordi dette er ett test")
    )

    init {
        val (_, feil) = minimalSøknadSomValiderer(
            søknadsperiode = LocalDate.now() to LocalDate.now().plusWeeks(1)
        ).mapTilK9Format(emptyList())

        check(feil.isEmpty()) {
            "Minimal søknad mangler felter. Feil=$feil"
        }
    }

    private fun optionalTilPeriode(periodeDto: PeriodeDto?):  List<PeriodeDto>? {
        if(periodeDto != null) {
            return listOf(periodeDto)
        }
        return null
    }
}

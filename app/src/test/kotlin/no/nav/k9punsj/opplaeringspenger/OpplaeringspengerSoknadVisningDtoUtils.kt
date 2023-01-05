package no.nav.k9punsj.opplaeringspenger

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.k9.søknad.felles.type.BegrunnelseForInnsending
import no.nav.k9punsj.felles.dto.ArbeidAktivitetDto
import no.nav.k9punsj.felles.dto.ArbeidstidDto
import no.nav.k9punsj.felles.dto.BostederDto
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.felles.dto.UtenlandsoppholdDto
import no.nav.k9punsj.objectMapper
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

internal object OpplaeringspengerSoknadVisningDtoUtils {

    private fun minimalSøknadSomValiderer(
        søker: String = "11111111111",
        barn: String = "22222222222",
        søknadsperiode: Pair<LocalDate, LocalDate>? = null,
        manipuler: (MutableMap<String, Any?>) -> Unit = {}
    ): OpplaeringspengerSøknadDto {
        val now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        @Language("JSON")
        val json = """
            {
              "soeknadId": "${UUID.randomUUID()}",
              "mottattDato": "${LocalDate.now()}",
              "klokkeslett": "$now",
              "soekerId": "$søker",
              "barn": {
                "norskIdent": "$barn"
              },
              "journalposter": [
                "12345678"
              ],
              "kurs":{
                  "kursHolder":{
                     "holder":"Nav"
                  },
                  "formaal":"test",
                  "kursperioder":[
                     {
                        "periode":{
                           "fom":"2022-12-14",
                           "tom":"2022-12-30"
                        },
                        "avreise":"2022-12-14",
                        "hjemkomst":"2022-12-30"
                     }
                  ]
               }
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

    private fun OpplaeringspengerSøknadDto.mapTilK9Format(
        perioderSomFinnesIK9: List<PeriodeDto> = emptyList()
    ) =
        MapOlpTilK9Format(
            søknadId = soeknadId,
            perioderSomFinnesIK9 = perioderSomFinnesIK9,
            journalpostIder = journalposter?.toSet() ?: emptySet(),
            dto = this
        ).søknadOgFeil()


    private fun arbeidstidInfoKomplettStruktur(
        optionalTekst: String?,
        periode: PeriodeDto?
    ) = ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto(
        perioder = listOf(
            ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto.ArbeidstidPeriodeInfoDto(
                periode = periode,
                faktiskArbeidTimerPerDag = optionalTekst,
                jobberNormaltTimerPerDag = optionalTekst
            )
        )
    )

    internal fun søknadMedKomplettStruktur(
        requiredTekst: String = "",
        optionalTekst: String? = "",
        requiredPeriode: PeriodeDto,
        optionalPeriode: PeriodeDto?
    ) = OpplaeringspengerSøknadDto(
        soeknadId = "${UUID.randomUUID()}",
        soekerId = "11111111111",
        journalposter = listOf(requiredTekst),
        mottattDato = LocalDate.now(),
        klokkeslett = LocalTime.now(),
        barn = OpplaeringspengerSøknadDto.BarnDto(
            norskIdent = optionalTekst,
            foedselsdato = null
        ),
        soeknadsperiode = optionalTilPeriode(optionalPeriode),
        opptjeningAktivitet = ArbeidAktivitetDto(
            selvstendigNaeringsdrivende = ArbeidAktivitetDto.SelvstendigNæringsdrivendeDto(
                organisasjonsnummer = optionalTekst,
                virksomhetNavn = optionalTekst,
                info = ArbeidAktivitetDto.SelvstendigNæringsdrivendeDto.SelvstendigNæringsdrivendePeriodeInfoDto(
                    periode = optionalPeriode,
                    virksomhetstyper = listOf(requiredTekst),
                    erFiskerPåBladB = null,
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
                ArbeidAktivitetDto.ArbeidstakerDto(
                    arbeidstidInfo = arbeidstidInfoKomplettStruktur(optionalTekst, optionalPeriode),
                    norskIdent = optionalTekst,
                    organisasjonsnummer = optionalTekst
                )
            ),
            frilanser = ArbeidAktivitetDto.FrilanserDto(
                startdato = optionalTekst,
                sluttdato = optionalTekst,
                jobberFortsattSomFrilans = null
            )
        ),
        arbeidstid = ArbeidstidDto(
            arbeidstakerList = listOf(
                ArbeidAktivitetDto.ArbeidstakerDto(
                    norskIdent = optionalTekst,
                    organisasjonsnummer = optionalTekst,
                    arbeidstidInfo = arbeidstidInfoKomplettStruktur(optionalTekst, optionalPeriode)
                )
            ),
            frilanserArbeidstidInfo = arbeidstidInfoKomplettStruktur(optionalTekst, optionalPeriode),
            selvstendigNæringsdrivendeArbeidstidInfo = arbeidstidInfoKomplettStruktur(optionalTekst, optionalPeriode)
        ),
        uttak = listOf(
            OpplaeringspengerSøknadDto.UttakDto(
                periode = optionalPeriode,
                timerPleieAvBarnetPerDag = optionalTekst
            )
        ),
        omsorg = OpplaeringspengerSøknadDto.OmsorgDto(
            relasjonTilBarnet = optionalTekst,
            samtykketOmsorgForBarnet = null,
            beskrivelseAvOmsorgsrollen = optionalTekst
        ),
        bosteder = listOf(
            BostederDto(
                periode = optionalPeriode,
                land = optionalTekst
            )
        ),
        lovbestemtFerie = listOf(requiredPeriode),
        soknadsinfo = OpplaeringspengerSøknadDto.DataBruktTilUtledningDto(
            samtidigHjemme = null,
            harMedsoeker = null
        ),
        harInfoSomIkkeKanPunsjes = true,
        harMedisinskeOpplysninger = true,
        trekkKravPerioder = setOf(requiredPeriode),
        begrunnelseForInnsending = BegrunnelseForInnsending().medBegrunnelseForInnsending("fordi dette er ett test"),
        kurs = OpplaeringspengerSøknadDto.Kurs(
            kursHolder = OpplaeringspengerSøknadDto.KursHolder(holder = "Nav", institusjonsUuid = null),
            kursperioder = listOf(OpplaeringspengerSøknadDto.KursPeriodeMedReisetid(
                periode = requiredPeriode, avreise = requiredPeriode.fom, hjemkomst = requiredPeriode.tom
            ))
        )
    )

    init {
        val (_, feil) = minimalSøknadSomValiderer(
            søknadsperiode = LocalDate.now() to LocalDate.now().plusWeeks(1)
        ).mapTilK9Format(emptyList())

        check(feil.isEmpty()) {
            "Minimal søknad mangler felter. Feil=$feil"
        }
    }

    private fun optionalTilPeriode(periodeDto: PeriodeDto?): List<PeriodeDto>? {
        if (periodeDto != null) {
            return listOf(periodeDto)
        }
        return null
    }
}

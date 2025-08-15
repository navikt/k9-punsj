package no.nav.k9punsj.opplaeringspenger

import no.nav.k9.søknad.felles.Feil
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.utils.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class MapOlpTilK9FormatTest {

    @Test
    fun `tom søknad med komplett struktur skal gi feil fra k9-format`() {
        val periode =
            PeriodeDto(LocalDate.now(), LocalDate.now().plusMonths(1))

        OpplaeringspengerSoknadVisningDtoUtils.søknadMedKomplettStruktur(requiredPeriode = periode, optionalPeriode = null)
            .feil()
            .assertInneholderFeil()

        OpplaeringspengerSoknadVisningDtoUtils.søknadMedKomplettStruktur(requiredPeriode = periode, optionalPeriode = periode)
            .feil()
            .assertInneholderFeil()
    }

    @Test
    fun `test json-payload mapper o validerer`() {
        val json = """
            {
              "arbeidstid": {
                "arbeidstakerList": [],
                "frilanserArbeidstidInfo": null,
                "selvstendigNæringsdrivendeArbeidstidInfo": null
              },
              "barn": { "norskIdent": "03091477490", "foedselsdato": "" },
              "begrunnelseForInnsending": { "tekst": "" },
              "bosteder": [],
              "harInfoSomIkkeKanPunsjes": false,
              "harMedisinskeOpplysninger": true,
              "journalposter": ["200"],
              "klokkeslett": "12:53",
              "kurs": {
                "kursHolder": {
                  "institusjonsUuid": "8671b60e-fd47-4097-b005-c376ea0fa240"
                },
                "kursperioder": [
                  {
                    "periode": { "fom": "2023-01-01", "tom": "2023-01-31" }
                  }
                ],
                "reise": {
                    "reisedager": ["2023-01-01", "2023-01-31"],
                    "reisedagerBeskrivelse": "borte bra, hjemme best"
                }
              },
              "lovbestemtFerie": [],
              "mottattDato": "2020-10-12",
              "omsorg": {
                "relasjonTilBarnet": "",
                "beskrivelseAvOmsorgsrollen": "",
                "samtykketOmsorgForBarnet": false
              },
              "opptjeningAktivitet": {
                "arbeidstaker": [],
                "selvstendigNaeringsdrivende": null,
                "frilanser": null
              },
              "soekerId": "29099000129",
              "soeknadId": "c9f9d5aa-535d-4da3-af8b-76e245ed1794",
              "soeknadsperiode": null,
              "soknadsinfo": { "samtidigHjemme": null, "harMedsøker": null },
              "trekkKravPerioder": [],
              "utenlandsopphold": [],
              "uttak": []
            }
        """.trimIndent()

        val dto = objectMapper().readValue(json, OpplaeringspengerSøknadDto::class.java)
        assert(dto.feil().isEmpty())
    }

    private fun KursPeriode(fom: LocalDate, tom: LocalDate) = OpplaeringspengerSøknadDto.KursPeriode(
            periode = PeriodeDto(
                fom = fom,
                tom = tom
            )
        )


    private fun OpplaeringspengerSøknadDto.feil() = MapOlpTilK9Format(
        dto = this,
        perioderSomFinnesIK9 = emptyList(),
        journalpostIder = setOf("123456789"),
        søknadId = "${UUID.randomUUID()}"
    ).feil()

    private fun List<Feil>.assertInneholderFeil() {
        assertThat(this).isNotEmpty
        assertThat(this.filter { it.feilkode == "uventetMappingfeil" }).isEmpty()
    }
}

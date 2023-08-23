package no.nav.k9punsj.integrasjoner.arbeidsgivere

import no.nav.k9punsj.integrasjoner.arbeidsgivere.AaregClient.Companion.deserialiser
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class AaregClientTest {

    @Test
    internal fun sjekk_at_deserialiser_takler_navArbeidsforholdId() {
        //language=json
        val json = """[{
                        "arbeidstaker": {
                            "type": "Person",
                            "offentligIdent": "12",
                            "aktoerId": "123"
                        },    
                        "arbeidssted": {
                            "type": "Organisasjon",
                            "identer": [
                                {
                                  "ident": "1234",
                                  "type": "ORGANISASJONSNUMMER"
                                }
                            ]
                        },
                        "ansettelsesperiode": {
                          "startdato": "2022-01-01",
                          "sluttdato": "2022-12-31"
                        }
                    }]
                    """
        val arbeidsforholdList = json.deserialiser<List<AaregClient.Companion.AaregArbeidsforhold>>()
        val aaregIdent: AaregClient.Companion.AaregIdent = arbeidsforholdList[0].arbeidssted.identer
            .first { it.type == AaregClient.Companion.AaregIdentType.ORGANISASJONSNUMMER }

        Assertions.assertThat(aaregIdent.ident).isEqualTo("1234")
    }
}

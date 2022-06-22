package no.nav.k9punsj.integrasjoner.arbeidsgivere

import no.nav.k9punsj.integrasjoner.arbeidsgivere.AaregClient.Companion.deserialiser
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class AaregClientTest {

    @Test
    internal fun sjekk_at_deserialiser_takler_navArbeidsforholdId() {
        val json = """[{
                        "arbeidstaker": {
                            "type": "Person",
                            "offentligIdent": "12",
                            "aktoerId": "123"
                        },    
                        "arbeidsgiver": {
                            "type": "Organisasjon",
                            "organisasjonsnummer": "1234"
                        }
                        }]
                    """
        val arbeidsforholdList = json.deserialiser<List<AaregClient.Companion.AaregArbeidsforhold>>()
        Assertions.assertThat(arbeidsforholdList[0].arbeidsgiver.organisasjonsnummer).isEqualTo("1234")
    }
}

package no.nav.k9punsj.util

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.k9punsj.felles.PunsjFagsakYtelseType
import no.nav.k9punsj.felles.Søknadstype
import no.nav.k9punsj.utils.objectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class PunsjFagsakYtelseTypeMappingTest {

    @Test
    fun `mapper ytelsetype fra punsjetsøknad`() {
        @Language("JSON")
        val json = """
                {
                   "søknadId":"49c7d65b-713c-4eb3-8070-eac00ae5803e",
                   "versjon":"1.1.0",
                   "mottattDato":"2023-05-26T08:20:00.000Z",
                   "søker":{
                      "norskIdentitetsnummer":"1111111111"
                   },
                   "ytelse":{
                      "type":"OMP_UT",
                      "fosterbarn":[
                         
                      ],
                      "aktivitet":{
                         
                      },
                      "fraværsperioder":[
                         {
                            "periode":"2023-04-03/2023-04-09",
                            "duration":"PT2H30M",
                            "delvisFravær":{
                               "normalarbeidstid":"PT7H",
                               "fravær":"PT2H"
                            },
                            "årsak":"ORDINÆRT_FRAVÆR",
                            "søknadÅrsak":"NYOPPSTARTET_HOS_ARBEIDSGIVER",
                            "aktivitetFravær":[
                               "ARBEIDSTAKER"
                            ],
                            "arbeidsforholdId":null,
                            "arbeidsgiverOrgNr":"222222222"
                         }
                      ],
                      "fraværsperioderKorrigeringIm":null,
                      "bosteder":null,
                      "utenlandsopphold":null
                   },
                   "språk":"nb",
                   "journalposter":[
                      {
                         "inneholderInfomasjonSomIkkeKanPunsjes":null,
                         "inneholderInformasjonSomIkkeKanPunsjes":false,
                         "inneholderMedisinskeOpplysninger":true,
                         "journalpostId":"333333333"
                      }
                   ],
                   "begrunnelseForInnsending":{
                      "tekst":null
                   },
                   "kildesystem":null
                }
        """

        val jsonSøknad = objectMapper().readTree(json) as ObjectNode

        val fagsakYtelseTypeKode = Søknadstype.fraK9FormatYtelsetype(jsonSøknad["ytelse"]["type"].asText()).k9YtelseType
        val punsjFagsakYtelseType = PunsjFagsakYtelseType.fromKode(fagsakYtelseTypeKode)
        Assertions.assertEquals(PunsjFagsakYtelseType.OMSORGSPENGER, punsjFagsakYtelseType)
    }
}

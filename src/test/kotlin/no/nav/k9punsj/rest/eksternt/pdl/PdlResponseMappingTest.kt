package no.nav.k9punsj.rest.eksternt.pdl

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.k9punsj.integrasjoner.pdl.Personopplysninger
import no.nav.k9punsj.integrasjoner.pdl.mapBarnFraRelasjoner
import no.nav.k9punsj.integrasjoner.pdl.mapPersonopplysninger
import no.nav.k9punsj.utils.objectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class PdlResponseMappingTest {

    @Test
    fun `mappe henting av identitetsnummer på barn`() {
        @Language("JSON")
        val eksempelResponse = """
        {
            "data": {
                "hentPerson": {
                    "forelderBarnRelasjon": [{
                            "relatertPersonsIdent": "11111111111",
                            "relatertPersonsRolle": "BARN"
                        },
                        {
                            "relatertPersonsIdent": "22222222222",
                            "relatertPersonsRolle": "BARN"
                        },
                        {
                            "relatertPersonsIdent": "33333333333",
                            "relatertPersonsRolle": "MOR"
                        }
                    ]
                }
            }
        }
        """.trimIndent()

        val objectNode = objectMapper().readTree(eksempelResponse) as ObjectNode

        val forventet = setOf("11111111111", "22222222222")

        assertEquals(forventet, objectNode.mapBarnFraRelasjoner())
    }

    @Test
    fun `mappe personopplysninger`() {
        @Language("JSON")
        val eksempelResponse = """
        {
            "data": {
                "hentPersonBolk": [{
                        "ident": "1111111111",
                        "person": {
                            "folkeregisteridentifikator": [{
                                "identifikasjonsnummer": "1111111111"
                            }],
                            "navn": [{
                                "fornavn": "OLA",
                                "mellomnavn": null,
                                "etternavn": "NORDMANN"
                            }],
                            "foedselsdato": [{
                                "foedselsdato": "1980-12-02"
                            }],
                            "adressebeskyttelse": []
                        }
                    },
                    {
                        "ident": "22222222222",
                        "person": {
                            "folkeregisteridentifikator": [{
                                "identifikasjonsnummer": "22222222222"
                            }],
                            "navn": [{
                                "fornavn": "KARI",
                                "mellomnavn": "MELLOMSTE",
                                "etternavn": "NORDMANN"
                            }],
                            "foedselsdato": [{
                                "foedselsdato": "1990-12-02"
                            }],
                            "adressebeskyttelse": [{
                                "gradering": "STRENGT_FORTROLIG"
                            }]
                        }
                    }
                ]
            }
        }
        """.trimIndent()

        val objectNode = objectMapper().readTree(eksempelResponse) as ObjectNode

        val forventet = setOf(
            Personopplysninger(
                identitetsnummer = "1111111111",
                fornavn = "OLA",
                mellomnavn = null,
                etternavn = "NORDMANN",
                fødselsdato = LocalDate.parse("1980-12-02"),
                gradering = Personopplysninger.Gradering.UGRADERT
            ),
            Personopplysninger(
                identitetsnummer = "22222222222",
                fornavn = "KARI",
                mellomnavn = "MELLOMSTE",
                etternavn = "NORDMANN",
                fødselsdato = LocalDate.parse("1990-12-02"),
                gradering = Personopplysninger.Gradering.STRENGT_FORTROLIG
            )
        )

        assertEquals(forventet, objectNode.mapPersonopplysninger())
    }
}

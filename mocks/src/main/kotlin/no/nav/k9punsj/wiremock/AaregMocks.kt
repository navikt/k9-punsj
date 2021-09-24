package no.nav.k9punsj.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import org.intellij.lang.annotations.Language

private const val path = "/aareg-mock"

private fun WireMockServer.stubHentArbeidsforhold(
    identitetsnummer: StringValuePattern,
    response: String
): WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$path/arbeidstaker/arbeidsforhold.*"))
            .withHeader("Authorization", WireMock.matching("Bearer ey.*"))
            .withHeader("Nav-Consumer-Token", WireMock.matching("Bearer ey.*"))
            .withHeader("Nav-Call-Id", AnythingPattern())
            .withHeader("Accept", WireMock.equalTo("application/json"))
            .withHeader("Nav-Personident", identitetsnummer)
            .withQueryParam("historikk", WireMock.equalTo("false"))
            .withQueryParam("sporingsinformasjon", WireMock.equalTo("false"))
            .withQueryParam("ansettelsesperiodeFom", AnythingPattern())
            .withQueryParam("ansettelsesperiodeTom", AnythingPattern())
            .willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withBody(response)
            )
    )
    return this
}

fun WireMockServer.stubAareg() : WireMockServer =
    stubHentArbeidsforhold(identitetsnummer = AnythingPattern(), response = DefaultResponse)
    .stubHentArbeidsforhold(identitetsnummer = WireMock.equalTo("22222222222"), response = "[]")

fun WireMockServer.getAaregBaseUrl() = baseUrl() + path

@Language("JSON")
private val DefaultResponse = """
[{
	"arbeidsforholdId": "979312059-arbf-1",
	"arbeidsgiver": {
		"organisasjonsnummer": "979312059",
		"type": "DuplikatKey",
		"type": "Organisasjon"
	}
},{
	"arbeidsforholdId": "979312059-arbf-2",
	"arbeidsgiver": {
		"organisasjonsnummer": "979312059",
		"type": "Organisasjon"
	}
}, {
	"arbeidsforholdId": "privat-arbef",
	"arbeidsgiver": {
		"atkoerId": "1234",
		"type": "Person"
	}
}]
""".trimIndent()

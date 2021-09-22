package no.nav.k9punsj.arbeidsgivere

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.coroutines.awaitStringResponse
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9punsj.hentCorrelationId
import org.springframework.stereotype.Component
import java.net.URI
import java.time.LocalDate
import kotlin.coroutines.coroutineContext

@Component
internal class AaregClient(
    private val baseUrl: URI,
    private val scopes: Set<String> = setOf("openid"),
    accessTokenClient: AccessTokenClient) {

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    internal suspend fun hentArbeidsgivere(
        identitetsnummer: String,
        fom: LocalDate,
        tom: LocalDate
    ) : Set<Arbeidsgiver> {
        val authorizationHeader = cachedAccessTokenClient.getAccessToken(scopes).asAuthoriationHeader()
        val url = "$baseUrl/arbeidstaker/arbeidsforhold?" +
            "ansettelsesperiodeFom=$fom&" +
            "ansettelsesperiodeTom=$tom&" +
            "regelverk=A_ORDNINGEN&" +
            "sporingsinformasjon=false&" +
            "historikk=false"

        val (_, response, responseBody) = ("$baseUrl/arbeidstaker/arbeidsforhold?" +
            "ansettelsesperiodeFom=$fom&" +
            "ansettelsesperiodeTom=$tom&" +
            "regelverk=A_ORDNINGEN&" +
            "sporingsinformasjon=false&" +
            "historikk=false").httpGet()
            .header("Authorization", authorizationHeader)
            .header("Nav-Consumer-Token", authorizationHeader)
            .header("Nav-Call-Id", coroutineContext.hentCorrelationId())
            .header("Nav-Personident", identitetsnummer)
            .awaitStringResponse()

        check(response.statusCode == 200) {
            "Uventet response fra Aareg. HttpStatus=${response.statusCode}, Response=$responseBody fra Url=$url"
        }

        val jsonArray = jacksonObjectMapper().readTree(responseBody) as ArrayNode

/*
    "arbeidsgiver": {
      "organisasjonsnummer": "123456789",
      "type": "DuplikatKey",
      "type": "Organisasjon"
    },
 */

    }

    private companion object {
        private data class AaregArbeidgiver(
            val organisasjonsnummer: String?
        )
        private data class AaregArbeidsforhold(
            val arbeidsgiver: AaregArbeidgiver
        )
    }
}
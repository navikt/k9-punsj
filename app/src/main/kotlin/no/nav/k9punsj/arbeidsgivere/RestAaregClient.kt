package no.nav.k9punsj.arbeidsgivere

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.coroutines.awaitStringResponse
import com.github.kittinunf.fuel.httpGet
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9punsj.hentCorrelationId
import no.nav.k9punsj.objectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.net.URI
import java.time.LocalDate
import kotlin.coroutines.coroutineContext

@Component
@Profile("!test & !local")
internal class RestAaregClient(
    @Value("\${AAREG_BASE_URL}") private val baseUrl: URI,
    @Qualifier("sts") accessTokenClient: AccessTokenClient) : AaregClient {
    private val scopes: Set<String> = setOf("openid")
    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    override suspend fun hentArbeidsforhold(
        identitetsnummer: String,
        fom: LocalDate,
        tom: LocalDate
    ) : Arbeidsforhold {
        val authorizationHeader = cachedAccessTokenClient.getAccessToken(scopes).asAuthoriationHeader()
        val url = "$baseUrl/arbeidstaker/arbeidsforhold?" +
            "ansettelsesperiodeFom=$fom&" +
            "ansettelsesperiodeTom=$tom&" +
            "regelverk=A_ORDNINGEN&" +
            "sporingsinformasjon=false&" +
            "historikk=false"

        val (_, response, responseBody) = url.httpGet()
            .header("Authorization", authorizationHeader)
            .header("Nav-Consumer-Token", authorizationHeader)
            .header("Nav-Call-Id", coroutineContext.hentCorrelationId())
            .header("Nav-Personident", identitetsnummer)
            .awaitStringResponse()

        check(response.statusCode == 200) {
            "Uventet response fra Aareg. HttpStatus=${response.statusCode}, Response=$responseBody fra Url=$url"
        }

        return Arbeidsforhold(
            organisasjoner = responseBody.deserialiser<List<AaregArbeidsforhold>>()
                .filterNot { it.arbeidsgiver.organisasjonsnummer.isNullOrBlank() }
                .map { OrganisasjonArbeidsforhold(
                    organisasjonsnummer = it.arbeidsgiver.organisasjonsnummer!!,
                    arbeidsforholdId = it.arbeidsgiver.arbeidsforholdId
                )}
                .toSet()
        )
    }

    private companion object {
        private data class AaregArbeidgiver(
            val arbeidsforholdId: String,
            val organisasjonsnummer: String?,
        )
        private data class AaregArbeidsforhold(
            val arbeidsgiver: AaregArbeidgiver
        )
        private inline fun <reified T>String.deserialiser() = kotlin.runCatching {
            objectMapper().readValue<T>(this)
        }.fold(
            onSuccess = { it },
            onFailure = {
                throw IllegalStateException("Feil ved deserialisering av Response=$this")
            }
        )
    }
}
package no.nav.k9punsj.integrasjoner.arbeidsgivere

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9punsj.hentCorrelationId
import no.nav.k9punsj.objectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI
import java.time.LocalDate
import kotlin.coroutines.coroutineContext

@Component
internal class AaregClient(
    @Value("\${AAREG_BASE_URL}") private val baseUrl: URI,
    @Qualifier("sts") accessTokenClient: AccessTokenClient
) {
    private val scopes: Set<String> = setOf("openid")
    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    internal suspend fun hentArbeidsforhold(
        identitetsnummer: String,
        fom: LocalDate,
        tom: LocalDate
    ): Arbeidsforhold {
        val authorizationHeader = cachedAccessTokenClient.getAccessToken(scopes).asAuthoriationHeader()
        val url = "$baseUrl/arbeidstaker/arbeidsforhold?" +
            "ansettelsesperiodeFom=$fom&" +
            "ansettelsesperiodeTom=$tom&" +
            "regelverk=A_ORDNINGEN&" +
            "sporingsinformasjon=false&" +
            "historikk=false"

        val (_, response, result) = url.httpGet()
            .header("Authorization", authorizationHeader)
            .header("Nav-Consumer-Token", authorizationHeader)
            .header("Nav-Call-Id", coroutineContext.hentCorrelationId())
            .header("Nav-Personident", identitetsnummer)
            .header("Accept", "application/json")
            .awaitStringResponseResult()

        val responseBody = result.fold(success = { it }, failure = { String(it.response.body().toByteArray()) })

        check(response.statusCode == 200) {
            "Uventet response fra Aareg. HttpStatus=${response.statusCode}, Response=$responseBody fra Url=$url"
        }

        return Arbeidsforhold(
            organisasjoner = responseBody.deserialiser<List<AaregArbeidsforhold>>()
                .filterNot { it.arbeidsgiver.organisasjonsnummer.isNullOrBlank() }
                .map {
                    OrganisasjonArbeidsforhold(
                        organisasjonsnummer = it.arbeidsgiver.organisasjonsnummer!!,
                        arbeidsforholdId = it.arbeidsforholdId
                    )
                }
                .toSet()
        )
    }

    companion object {
        data class AaregArbeidgiver(
            val organisasjonsnummer: String?
        )

        data class AaregArbeidsforhold(
            val arbeidsforholdId: String?,
            val arbeidsgiver: AaregArbeidgiver
        )

        inline fun <reified T> String.deserialiser() = kotlin.runCatching {
            objectMapper().readValue<T>(this)
        }.fold(
            onSuccess = { it },
            onFailure = {
                throw IllegalStateException("Feil ved deserialisering av Response=$this", it)
            }
        )
    }
}

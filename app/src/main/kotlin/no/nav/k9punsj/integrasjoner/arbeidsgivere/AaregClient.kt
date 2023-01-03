package no.nav.k9punsj.integrasjoner.arbeidsgivere

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9punsj.hentCorrelationId
import no.nav.k9punsj.objectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI
import java.time.LocalDate
import kotlin.coroutines.coroutineContext

@Component
internal class AaregClient(
    @Value("\${AAREG_BASE_URL}") private val baseUrl: URI,
    @Value("\${AAREG_SCOPE}") private val scope: String,
    @Qualifier("azure") accessTokenClient: AccessTokenClient,
) {
    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    internal suspend fun hentArbeidsforhold(
        identitetsnummer: String,
        fom: LocalDate,
        tom: LocalDate,
    ): Arbeidsforhold {
        val authorizationHeader = cachedAccessTokenClient.getAccessToken(setOf(scope)).asAuthoriationHeader()
        val url =
            """$baseUrl/arbeidstaker/arbeidsforhold?rapporteringsordning=A_ORDNINGEN&sporingsinformasjon=false&historikk=false"""

        val (_, response, result) = url.httpGet()
            .header("Authorization", authorizationHeader)
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
                .filter { arbeidsforhold -> arbeidsforhold.arbeidssted.identer.any { it.type == AaregIdentType.ORGANISASJONSNUMMER } }
                .filter { it.ansettelsesperiode.harArbeidsforholdIPerioden(fom, tom) }
                .map {
                    OrganisasjonArbeidsforhold(
                        organisasjonsnummer = it.arbeidssted.identer.first().ident,
                        arbeidsforholdId = it.id
                    )
                }
                .toSet()
        )
    }

    private fun AaregAnsettelsesperiode.harArbeidsforholdIPerioden(start: LocalDate, slutt: LocalDate): Boolean {
        return startdato.erLikEllerFør(slutt) && (sluttdato == null || sluttdato.erLikEllerEtter(start))
    }

    private fun LocalDate.erLikEllerEtter(dato: LocalDate) = isEqual(dato) || isAfter(dato)
    private fun LocalDate.erLikEllerFør(dato: LocalDate) = isEqual(dato) || isBefore(dato)

    companion object {
        private val logger = LoggerFactory.getLogger(AaregClient::class.java)

        enum class AaregIdentType { ORGANISASJONSNUMMER, AKTORID, FOLKEREGISTERIDENT }

        data class AaregIdent(
            val ident: String,
            val type: AaregIdentType,
        )

        data class AaregArbeidssted(
            val identer: List<AaregIdent>,
        )

        data class AaregAnsettelsesperiode(
            val startdato: LocalDate,
            val sluttdato: LocalDate? = null,
        )

        data class AaregArbeidsforhold(
            val id: String?,
            val arbeidssted: AaregArbeidssted,
            val ansettelsesperiode: AaregAnsettelsesperiode,
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

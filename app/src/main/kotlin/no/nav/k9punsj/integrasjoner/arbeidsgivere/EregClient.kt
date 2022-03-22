package no.nav.k9punsj.integrasjoner.arbeidsgivere

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import no.nav.k9punsj.hentCorrelationId
import no.nav.k9punsj.objectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI
import kotlin.coroutines.coroutineContext

@Component
internal class EregClient(
    @Value("\${EREG_BASE_URL}") private val baseUrl: URI) {

    internal suspend fun hentOrganisasjonsnavn(organisasjonsummer: String) : String? {
        val url = "$baseUrl/organisasjon/$organisasjonsummer/noekkelinfo"

        val (_, response, result) = url.httpGet()
            .header("Nav-Call-Id", coroutineContext.hentCorrelationId())
            .header("Nav-Consumer-Id", "k9-punsj")
            .header("Accept", "application/json")
            .awaitStringResponseResult()

        val responseBody = result.fold(success = { it }, failure = { String(it.response.body().toByteArray()) })

        check(response.statusCode == 200 || response.statusCode == 404) {
            "Uventet response fra Ereg. HttpStatus=${response.statusCode}, Response=$responseBody fra Url=$url"
        }

        if (response.statusCode == 404) {
            return null
        }

        val eregNavn = responseBody.deserialiser<EregNøkkelinfo>().navn
        return listOf(
            eregNavn.navnelinje1,
            eregNavn.navnelinje2,
            eregNavn.navnelinje3,
            eregNavn.navnelinje4,
            eregNavn.navnelinje5
        ).filterNot { it.isNullOrBlank() }.joinToString(" ")
    }

    private companion object {
        private data class EregNavn(
            val navnelinje1: String? = "",
            val navnelinje2: String? = "",
            val navnelinje3: String? = "",
            val navnelinje4: String? = "",
            val navnelinje5: String? = ""
        )
        private data class EregNøkkelinfo(
            val navn: EregNavn
        )

        private inline fun <reified T>String.deserialiser() = kotlin.runCatching {
            objectMapper().readValue<T>(this)
        }.fold(
            onSuccess = { it },
            onFailure = {
                throw IllegalStateException("Feil ved deserialisering av Response=$this", it)
            }
        )
    }
}
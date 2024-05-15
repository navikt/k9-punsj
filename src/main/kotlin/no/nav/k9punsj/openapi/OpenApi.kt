package no.nav.k9punsj.openapi

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.OAuthFlow
import io.swagger.v3.oas.models.security.OAuthFlows
import io.swagger.v3.oas.models.security.Scopes
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import no.nav.k9punsj.fordel.K9FordelType
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import java.net.URI
import java.time.LocalDate
import java.time.LocalTime

@Component
internal class OpenApi(
    @Value("\${springdoc.oAuthFlow.authorizationUrl}") val authorizationUrl: String,
    @Value("\${springdoc.oAuthFlow.tokenUrl}") val tokenUrl: String,
    @Value("\${springdoc.oAuthFlow.apiScope}") val apiScope: String
) {

    @Bean
    internal fun openApi(
        @Value("\${no.nav.navn}") navn: String,
        @Value("\${no.nav.beskrivelse}") beskrivelse: String,
        @Value("\${no.nav.versjon}") versjon: String,
        @Value("\${no.nav.swagger_server_base_url}") swaggerServerBaseUrl: URI,
        @Value("\${no.nav.security.jwt.client.azure.client_id}") azureClientId: String
    ): OpenAPI = OpenAPI()
        .addServersItem(Server().url("$swaggerServerBaseUrl/api").description("Swagger Server"))
        .info(
            Info()
                .title(navn)
                .description("$beskrivelse\n\nScope for å tjenesten: `$azureClientId/.default`")
                .version(versjon)
                .contact(
                    Contact()
                        .name("Arbeids- og velferdsdirektoratet")
                        .url("https://www.nav.no")
                )
                .license(
                    License()
                        .name("MIT")
                        .url("https://github.com/navikt/k9-punsj/blob/master/LICENSE")
                )
        )
        .components(
            Components()
                .addSecuritySchemes("oauth2", azureLogin())
                .addSecuritySchemes("Authorization", azureLogin())
        )
        .addSecurityItem(
            SecurityRequirement()
                .addList("oauth2", listOf("read", "write"))
                .addList("Authorization")
        )

    private fun azureLogin(): SecurityScheme {
        return SecurityScheme()
            .name("oauth2")
            .type(SecurityScheme.Type.OAUTH2)
            .scheme("oauth2")
            .`in`(SecurityScheme.In.HEADER)
            .flows(
                OAuthFlows()
                    .authorizationCode(
                        OAuthFlow().authorizationUrl(authorizationUrl)
                            .tokenUrl(tokenUrl)
                            .scopes(Scopes().addString(apiScope, "read,write"))
                    )
            )
    }
}

data class OasFeil(
    val feil: String?
)

data class OasSøknadId(
    val soeknadId: String
)

data class OasSoknadsfeil(
    val mappeId: String?,
    val feil: List<FeilDto>?
) {
    data class FeilDto(
        val felt: String?,
        val feilkode: String?,
        val feilmelding: String?
    )
}

data class OasDokumentInfo(
    val dokument_id: String
)

data class OasJournalpostIder(
    val poster: List<OasJournalpostDto>
)

data class OasJournalpostDto(
    val journalpostId: String,
    val gosysoppgaveId: String? = null,
    val dokumenter: Set<OasDokumentInfo>?,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val dato: LocalDate?,
    @JsonFormat(pattern = "HH:mm")
    val klokkeslett: LocalTime?,
    val punsjInnsendingType: K9FordelType?
)

package no.nav.k9punsj.openapi

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverter
import io.swagger.v3.core.converter.ModelConverterContext
import io.swagger.v3.core.util.Json
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.servers.Server
import no.nav.k9punsj.fordel.PunsjInnsendingType
import no.nav.k9punsj.integrasjoner.pdl.PdlPersonDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

@Component
internal class OpenApi {

    @Bean
    internal fun modelConverter(): ModelConverter = DurationMockConverter()

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
}

// Disse klassene er nødvendige for å eksponere søknadsformatet, så lenge applikasjonen benytter userialisert json internt

data class OasIdentDto(
    val norskIdent: String
)

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

data class OasJournalpostInfo(
    val dokumenter: Set<OasDokumentInfo>,
    val venter: OasVentDto?
)

data class OasJournalpostIder(
    val poster: List<OasJournalpostDto>
)

data class OasJournalpostDto(
    val journalpostId: String,
    val dokumenter: Set<OasDokumentInfo>?,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val dato: LocalDate?,
    @JsonFormat(pattern = "HH:mm")
    val klokkeslett: LocalTime?,
    val punsjInnsendingType: PunsjInnsendingType?
)

data class OasVentDto(
    val venteÅrsak: String,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val venterTil: LocalDate
)

data class OasPunsjBolleDto(
    val brukerIdent: String,
    val barnIdent: String,
    val journalpostId: String
)

data class OasSkalTilInfotrygdSvar(
    val k9sak: Boolean
)

data class OasHentPerson(
    val norskIdent: String
)

data class OasPdlResponse(
    val person: PdlPersonDto
)

/*
Denne konvertereren brukes kun fordi OpenAPI
ikke har implementert java.time.Duration - noe som førte til at api-dokumentasjon ble ubrukelig.
https://github.com/swagger-api/swagger-core/issues/1445
https://github.com/swagger-api/swagger-core/issues/2784
 */
private class DurationMockConverter : ModelConverter {
    override fun resolve(
        type: AnnotatedType,
        context: ModelConverterContext,
        chain: Iterator<ModelConverter>
    ): Schema<*>? {
        if (type.isSchemaProperty) {
            val _type = Json.mapper().constructType(type.type)
            if (_type != null) {
                val cls = _type.rawClass
                if (Duration::class.java.isAssignableFrom(cls)) {
                    return ObjectSchema().example("PT7H25M")
                }
            }
        }
        return if (chain.hasNext()) {
            chain.next().resolve(type, context, chain)
        } else {
            null
        }
    }
}

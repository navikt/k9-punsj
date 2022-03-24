package no.nav.k9punsj.rest.web.openapi

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.core.converter.ModelConverter
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import no.nav.k9punsj.db.datamodell.MappeId
import no.nav.k9punsj.domenetjenester.dto.BunkeIdDto
import no.nav.k9punsj.domenetjenester.dto.JournalpostIdDto
import no.nav.k9punsj.domenetjenester.dto.NorskIdentDto
import no.nav.k9punsj.domenetjenester.dto.PdlPersonDto
import no.nav.k9punsj.domenetjenester.dto.PeriodeDto
import no.nav.k9punsj.pleiepengersyktbarn.PleiepengerSyktBarnSøknadDto
import no.nav.k9punsj.domenetjenester.dto.SøknadDto
import no.nav.k9punsj.domenetjenester.dto.SøknadIdDto
import no.nav.k9punsj.fordel.PunsjInnsendingType
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import java.net.URI
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
        @Value("\${no.nav.security.jwt.client.azure.client_id}") azureClientId: String,
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
data class OasHentSøknad(
    val norskIdent: NorskIdentDto,
)

data class OasMatchfagsak(
    val brukerIdent: NorskIdentDto,
    val barnIdent: NorskIdentDto,
)

data class OasMatchfagsakMedPeriode(
    val brukerIdent: NorskIdentDto,
    val periodeDto: PeriodeDto,
)

data class OasIdentDto(
    val norskIdent: NorskIdentDto,
)

data class OasFeil(
    val feil: String?,
)

data class OasSøknadId(
    val soeknadId: SøknadIdDto,
)

data class OasSendSøknad(
    val norskIdent: NorskIdentDto,
    val soeknadId: SøknadIdDto,
)

data class OasOpprettNySøknad(
    val norskIdent: NorskIdentDto,
    val journalpostId: JournalpostIdDto,
)

data class OasPleiepengerSyktBarSoknadMappeSvar(
    val mappeId: MappeId,
    val søker: NorskIdentDto,
    val bunker: List<OasBunkeDto>?,
) {
    data class OasBunkeDto(
        val bunkeId: BunkeIdDto,
        val fagsakKode: String,
        val søknader: List<SøknadDto<PleiepengerSyktBarnSøknadDto>>?,
    )
}

data class OasSoknadsfeil(
    val mappeId: MappeId?,
    val feil: List<FeilDto>?,
) {
    data class FeilDto(
        val felt: String?,
        val feilkode: String?,
        val feilmelding: String?,
    )
}

data class OasDokumentInfo(
    val dokument_id: String,
)

data class OasJournalpostInfo(
    val dokumenter: Set<OasDokumentInfo>,
    val venter: OasVentDto?,
)

data class OasJournalpostIder(
    val poster: List<OasJournalpostDto>,
)

data class OasJournalpostDto(
    val journalpostId: JournalpostIdDto,
    val dokumenter: Set<OasDokumentInfo>?,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val dato: LocalDate?,
    @JsonFormat(pattern = "HH:mm")
    val klokkeslett: LocalTime?,
    val punsjInnsendingType: PunsjInnsendingType?,
)

data class OasVentDto(
    val venteÅrsak: String,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val venterTil: LocalDate,
)

data class OasPunsjBolleDto(
    val brukerIdent: NorskIdentDto,
    val barnIdent: NorskIdentDto,
    val journalpostId: JournalpostIdDto,
)

data class OasSkalTilInfotrygdSvar(
    val k9sak: Boolean,
)

data class OasHentPerson(
    val norskIdent: NorskIdentDto,
)

data class OasPdlResponse(
    val person: PdlPersonDto,
)

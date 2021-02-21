package no.nav.k9punsj.rest.web.openapi

import io.swagger.v3.core.converter.ModelConverter
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import no.nav.k9punsj.db.datamodell.NorskIdent
import no.nav.k9punsj.db.datamodell.Person
import no.nav.k9punsj.fagsak.FagsakRoutes
import no.nav.k9punsj.fordel.HendelseRoutes
import no.nav.k9punsj.gosys.GosysRoutes
import no.nav.k9punsj.journalpost.JournalpostRoutes
import no.nav.k9punsj.rest.eksternt.k9sak.K9SakRoutes
import no.nav.k9punsj.rest.eksternt.pdl.PdlRoutes
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.time.LocalDate

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

@RestController
@SecurityScheme(
    name = "BearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
@Tag(name = "Journalposter", description = "Håndtering av journalposter")
internal class JournalpostController {
    @PostMapping(
        JournalpostRoutes.Urls.OmfordelJournalpost,
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "Journalpost omfordelt"
            ),
            ApiResponse(
                responseCode = "401",
                description = "Ikke innlogget"
            ),
            ApiResponse(
                responseCode = "403",
                description = "Ikke tilgang til journalposten"
            ),
            ApiResponse(
                responseCode = "404",
                description = "Journalpost eksisterer ikke"
            )
        ]
    )
    @Operation(
        summary = "Omfordele journalpost",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    fun OmfordelJournalpost(
        @PathVariable("journalpost_id") journalpostId: String,
        @RequestBody body: JournalpostRoutes.OmfordelingRequest,
    ) {
    }

    @GetMapping(JournalpostRoutes.Urls.JournalpostInfo, produces = ["application/json"])
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Liste med dokumenter som er i journalposten.",
                content = [Content(
                    schema = Schema(
                        implementation = OasJournalpostInfo::class
                    )
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Journalpost kan ikke håndteres i Punsj"
            ),
            ApiResponse(
                responseCode = "401",
                description = "Ikke innlogget"
            ),
            ApiResponse(
                responseCode = "403",
                description = "Ikke tilgang til journalposten"
            ),
            ApiResponse(
                responseCode = "404",
                description = "Journalpost eksisterer ikke"
            )
        ]
    )
    @Operation(
        summary = "Hente informasjon om en journalpost",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    fun HenteJournalpostInfo(
        @PathVariable("journalpost_id") journalpostId: String,
    ) {
    }

    @GetMapping(JournalpostRoutes.Urls.Dokument, produces = ["application/pdf"])
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Dokumentet."
            ),
            ApiResponse(
                responseCode = "401",
                description = "Ikke innlogget"
            ),
            ApiResponse(
                responseCode = "403",
                description = "Ikke tilgang til journalposten"
            ),
            ApiResponse(
                responseCode = "404",
                description = "Journalpost eksisterer ikke"
            )
        ]
    )
    @Operation(
        summary = "Hente dokumentet",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    fun HenteDokument(
        @PathVariable("journalpost_id") journalpostId: String,
        @PathVariable("dokument_id") dokumentId: String,
    ) {
    }
}

data class OasDokumentInfo(
    val dokument_id: String,
)

data class OasJournalpostInfo(
    val dokumenter: Set<OasDokumentInfo>,
)

@RestController
@Tag(name = "Fagsaker", description = "Liste fagsaker")
internal class FagsakerController {
    @GetMapping(FagsakRoutes.Urls.HenteFagsakinfo, produces = ["application/json"])
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Liste med fagsaker tilknyttet personen.",
                content = [Content(
                    schema = Schema(
                        implementation = OasFagsakListe::class
                    )
                )]
            )
        ]
    )
    @Operation(
        summary = "Hente liste med fagsaker tilknyttet personen.",
        description = "ytelse må være 'pleiepenger-sykt-barn'"
    )
    fun HenteFagsaker(
        @RequestParam("ytelse") ytelse: String,
        @PathVariable("norsk_ident") norskIdent: String,
    ) {
    }
}

data class OasFagsakListe(
    val fagsaker: Set<OasFagsak>,
)

data class OasFagsak(
    val fagsak_id: String,
    val url: String,
    val fra_og_med: LocalDate,
    val til_og_med: LocalDate?,
    val barn: OasFagsakBarn,
)

data class OasFagsakBarn(
    val fødselsdato: LocalDate,
    val navn: String,
)

@RestController
@Tag(name = "Pdl", description = "Hent aktørid fra norsk ident")
internal class PdlController {
    @PostMapping(PdlRoutes.Urls.HentIdent, consumes = ["application/json"], produces = ["application/json"])
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Henter aktørid fra fnummer",
                content = [Content(
                    schema = Schema(
                        implementation = AktørResponse::class
                    )
                )]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Ikke innlogget"
            ),
            ApiResponse(
                responseCode = "403",
                description = "Ikke tilgang til å slå opp personen"
            ),
            ApiResponse(
                responseCode = "404",
                description = "Personen eksisterer ikke"
            )
        ]
    )

    @Operation(
        summary = "Henter aktørid fra fnummer",
        description = "Henter aktørid fra fnummer",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    fun Hentident(@RequestBody body: NorskIdent) {

    }
}

data class AktørResponse(
    val person: Person,
)


@RestController
@Tag(name = "Gosys", description = "Opprett journalføringsoppgave")
internal class GosysController {
    @PostMapping(
        GosysRoutes.Urls.OpprettJournalføringsoppgave,
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Oppretter journalføringsoppgave for fnummer og journalpostid",
                content = [Content(
                    schema = Schema(
                        implementation = GosysRoutes.GosysOpprettJournalføringsOppgaveRequest::class
                    )
                )]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Ikke innlogget"
            ),
            ApiResponse(
                responseCode = "403",
                description = "Ikke tilgang til å opprette journalføringsoppgave"
            ),
            ApiResponse(
                responseCode = "404",
                description = "Eksisterer ikke"
            )
        ]
    )

    @Operation(
        summary = "Oppretter journalføringsoppgave",
        description = "",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    fun OpprettJournalføringsoppgave(@RequestBody body: GosysRoutes.GosysOpprettJournalføringsOppgaveRequest) {

    }
}

@RestController
@Tag(name = "HendelseMottaker", description = "Prosesserer")
internal class HendelseController {
    @PostMapping(
        HendelseRoutes.Urls.ProsesserHendelse,
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Prosessert",
                content = [Content(
                    schema = Schema(
                        implementation = HendelseRoutes.FordelPunsjEventDto::class
                    )
                )]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Ikke innlogget"
            ),
            ApiResponse(
                responseCode = "403",
                description = "Ikke tilgang til å opprette journalføringsoppgave"
            ),
            ApiResponse(
                responseCode = "404",
                description = "Eksisterer ikke"
            )
        ]
    )

    @Operation(summary = "Prosesser hendelse", description = "", security = [SecurityRequirement(name = "BearerAuth")])
    fun ProsesserHendelse(@RequestBody body: HendelseRoutes.FordelPunsjEventDto) {

    }
}

@RestController
@Tag(name = "K9Sak", description = "Håndtering kall mot k9Sak")
internal class K9SakController {
    @PostMapping(
      K9SakRoutes.Urls.HentSisteVersjonAvPleiepengerSøknad,
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Prosessert",
                content = [Content(
                    schema = Schema(
                        implementation = K9SakRoutes.K9SakSøknadDto::class
                    )
                )]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Ikke innlogget"
            ),
            ApiResponse(
                responseCode = "403",
                description = "Ikke tilgang til å opprette journalføringsoppgave"
            ),
            ApiResponse(
                responseCode = "404",
                description = "Eksisterer ikke"
            )
        ]
    )

    @Operation(summary = "Prosesser hendelse", description = "", security = [SecurityRequirement(name = "BearerAuth")])
    fun ProsesserHendelse(@RequestBody body: HendelseRoutes.FordelPunsjEventDto) {

    }
}

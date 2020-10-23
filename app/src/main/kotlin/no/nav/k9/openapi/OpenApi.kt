package no.nav.k9.openapi

import io.netty.handler.codec.http.HttpScheme.HTTPS
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
import no.nav.k9.JournalpostInnhold
import no.nav.k9.NorskIdent
import no.nav.k9.fagsak.FagsakRoutes
import no.nav.k9.journalpost.JournalpostRoutes
import no.nav.k9.mappe.MappeId
import no.nav.k9.mappe.PersonDTO
import no.nav.k9.pdl.PdlRoutes
import no.nav.k9.pleiepengersyktbarn.soknad.PleiepengerSyktBarnRoutes
import no.nav.k9.pleiepengersyktbarn.soknad.PleiepengerSyktBarnSoknad
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

@RestController
@Tag(name = "Pleiepenger sykt barn søknad", description = "Håndtering av papirsøknader")
internal class PleiepengerSyktBarnSoknadController {
    @GetMapping(PleiepengerSyktBarnRoutes.Urls.HenteMapper, produces = ["application/json"])
    @Operation(
            summary = "Hente eksisterende mapper på en person som inneholder ufullstendige søknader.",
            description = "Kan sendes en eller fler NorskIdenter som headere. Viser kun mapper hvor alle Norske Identer er med i."
    )
    @ApiResponses(value = [
        ApiResponse(
                responseCode = "200",
                description = "Alle mapper lagret på personen(e) sendt inn.",
                content = [Content(
                        schema = Schema(
                                implementation = OasPleiepengerSyktBarSoknadMapperSvar::class
                        )
                )]
        )
    ])
    fun HenteMapper(
            @RequestHeader("X-Nav-NorskIdent") norskIdenter: Set<String>
    ) {
    }

    @GetMapping(PleiepengerSyktBarnRoutes.Urls.EksisterendeSøknad, produces = ["application/json"])
    @Operation(
            summary = "Hente eksisterende mappe"
    )
    @ApiResponses(value = [
        ApiResponse(
                responseCode = "200",
                description = "Mappen",
                content = [Content(
                        schema = Schema(
                                implementation = OasPleiepengerSyktBarSoknadMappeSvar::class
                        )
                )]
        ),
        ApiResponse(
                responseCode = "404",
                description = "Mappen finnes ikke"
        )
    ])
    fun HenteMappe(
            @PathVariable("mappe_id") mappeId: String
    ) {
    }


    @PutMapping(PleiepengerSyktBarnRoutes.Urls.EksisterendeSøknad, consumes = ["application/json"], produces = ["application/json"])
    @Operation(summary = "Oppdatere en søknad i en eksisterende mappe.")
    @ApiResponses(value = [
        ApiResponse(
                responseCode = "200",
                description = "Innhold på søknader er oppdatert og søknadene er klare for innsending.",
                content = [Content(
                        schema = Schema(
                                implementation = OasPleiepengerSyktBarSoknadMappeSvar::class
                        )
                )]
        ),
        ApiResponse(
                responseCode = "400",
                description = "Innhold på søknader er oppdatert, men inneholder fortsatt mangler.",
                content = [Content(
                        schema = Schema(
                                implementation = OasPleiepengerSyktBarSoknadMapperSvar::class
                        )
                )]
        )
    ])
    fun OppdatereSøknad(
            @PathVariable("mappe_id") mappeId: String,
            @RequestBody søknad: OasInnsending
    ) {
    }


    @PostMapping(PleiepengerSyktBarnRoutes.Urls.EksisterendeSøknad, consumes = ["application/json"], produces = ["application/json"])
    @Operation(summary = "Sende inn søknad til behandling i saksbehsandlingssystemet.")
    @ApiResponses(value = [
        ApiResponse(
                responseCode = "202",
                description = "Søknaden er sendt til behandling og personen fjernet fra mappen.",
                content = [Content(
                        schema = Schema(
                                implementation = Void::class
                        )
                )]
        ),
        ApiResponse(
                responseCode = "400",
                description = "Innsending feilet grunnet mangler i søknaden.",
                content = [Content(
                        schema = Schema(
                                implementation = OasPleiepengerSyktBarSoknadMapperSvar::class
                        )
                )]
        )
    ])
    fun SendSøknad(
            @PathVariable("mappe_id") mappeId: String,
            @RequestHeader("X-Nav-NorskIdent") norskIdenter: String
    ) {
    }

    @PostMapping(PleiepengerSyktBarnRoutes.Urls.NySøknad,
            consumes = ["application/json"],
            produces = ["application/json"]
    )
    @Operation(summary = "Starte en helt ny søknad")
    @ApiResponses(value = [
        ApiResponse(
                responseCode = "201",
                description = "Opprettet mappe for en ny søknad. Se 'Location' header for URL til mappen.",
                content = [Content(
                        schema = Schema(
                                implementation = OasPleiepengerSyktBarSoknadMappeSvar::class
                        )
                )]
        )
    ])
    fun NySøknad(
            @RequestBody søknad: OasInnsending
    ) {
    }
}

// Disse klassene er nødvendige for å eksponere søknadsformatet, så lenge applikasjonen benytter userialisert json internt
data class OasInnsending(
        val personer: Map<String, JournalpostInnhold<PleiepengerSyktBarnSoknad>>
)

data class OasPleiepengerSyktBarSoknadMappeSvar(
        val mappeId: MappeId,
        val personer: MutableMap<NorskIdent, PersonDTO<PleiepengerSyktBarnSoknad>>
)

data class OasPleiepengerSyktBarSoknadMapperSvar(
        val mapper: List<OasPleiepengerSyktBarSoknadMappeSvar>
)

@RestController
@SecurityScheme(
        name = "BearerAuth",
        type = SecuritySchemeType.APIKEY,
        scheme = "bearer",
        bearerFormat = "jwt"
)
@Tag(name = "Journalposter", description = "Håndtering av journalposter")
internal class JournalpostController {
    @PostMapping(JournalpostRoutes.Urls.OmfordelJournalpost, consumes = ["application/json"], produces = ["application/json"])
    @ApiResponses(value = [
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
    ])
    @Operation(
            summary = "Omfordele journalpost",
            security = [SecurityRequirement(name = "BearerAuth")]
    )
    fun OmfordelJournalpost(
            @PathVariable("journalpost_id") journalpostId: String,
            @RequestBody body: JournalpostRoutes.OmfordelingRequest) {
    }

    @GetMapping(JournalpostRoutes.Urls.JournalpostInfo, produces = ["application/json"])
    @ApiResponses(value = [
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
    ])
    @Operation(
            summary = "Hente informasjon om en journalpost",
            security = [SecurityRequirement(name = "BearerAuth")]
    )
    fun HenteJournalpostInfo(
            @PathVariable("journalpost_id") journalpostId: String) {
    }

    @GetMapping(JournalpostRoutes.Urls.Dokument, produces = ["application/pdf"])
    @ApiResponses(value = [
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
    ])
    @Operation(
            summary = "Hente dokumentet",
            security = [SecurityRequirement(name = "BearerAuth")]
    )
    fun HenteDokument(
            @PathVariable("journalpost_id") journalpostId: String,
            @PathVariable("dokument_id") dokumentId: String) {
    }
}

data class OasDokumentInfo(
        val dokument_id: String
)

data class OasJournalpostInfo(
        val dokumenter: Set<OasDokumentInfo>
)

@RestController
@Tag(name = "Fagsaker", description = "Liste fagsaker")
internal class FagsakerController {
    @GetMapping(FagsakRoutes.Urls.HenteFagsakinfo, produces = ["application/json"])
    @ApiResponses(value = [
        ApiResponse(
                responseCode = "200",
                description = "Liste med fagsaker tilknyttet personen.",
                content = [Content(
                        schema = Schema(
                                implementation = OasFagsakListe::class
                        )
                )]
        )
    ])
    @Operation(summary = "Hente liste med fagsaker tilknyttet personen.", description = "ytelse må være 'pleiepenger-sykt-barn'")
    fun HenteFagsaker(
            @RequestParam("ytelse") ytelse: String,
            @PathVariable("norsk_ident") norskIdent: String) {
    }
}

data class OasFagsakListe(
        val fagsaker: Set<OasFagsak>
)

data class OasFagsak(
        val fagsak_id: String,
        val url: String,
        val fra_og_med: LocalDate,
        val til_og_med: LocalDate?,
        val barn: OasFagsakBarn
)

data class OasFagsakBarn(
        val fødselsdato: LocalDate,
        val navn: String
)

@RestController
@Tag(name = "Pdl", description = "Hent aktørid fra norsk ident")
internal class PdlController {
    @PostMapping(PdlRoutes.Urls.HentIdent, consumes = ["application/json"], produces = ["application/json"])
    @ApiResponses(value = [
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
    ])

    @Operation(summary = "Henter aktørid fra fnummer", description = "Henter aktørid fra fnummer'", security = [SecurityRequirement(name = "BearerAuth")])
    fun Hentident( @RequestBody body: PdlRoutes.NorskIdent) {

    }
}

data class AktørResponse(
        val norskIdent: NorskIdent,
        val aktørid: String
)

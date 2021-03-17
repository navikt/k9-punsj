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
import no.nav.k9punsj.db.datamodell.MappeId
import no.nav.k9punsj.fagsak.FagsakRoutes
import no.nav.k9punsj.fordel.HendelseRoutes
import no.nav.k9punsj.gosys.GosysRoutes
import no.nav.k9punsj.journalpost.JournalpostRoutes
import no.nav.k9punsj.rest.eksternt.k9sak.K9SakRoutes
import no.nav.k9punsj.rest.eksternt.pdl.PdlRoutes
import no.nav.k9punsj.rest.web.SøknadJson
import no.nav.k9punsj.rest.web.dto.*
import no.nav.k9punsj.rest.web.ruter.PleiepengerSyktBarnRoutes
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
@Tag(name = "Pleiepenger sykt barn søknad", description = "Håndtering av papirsøknader")
internal class PleiepengerSyktBarnSoknadController {
    @GetMapping(PleiepengerSyktBarnRoutes.Urls.HenteMappe, produces = ["application/json"])
    @Operation(
        summary = "Henter mappen til en person som inneholder søknader.",
        description = "Sendes NorskIdente til person som headere.",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Henter mappen til en person som inneholder alle søknader",
                content = [Content(
                    schema = Schema(
                        implementation = PleiepengerSøknadVisningDto::class
                    )
                )]
            )
        ]
    )
    fun HenteMappe(@RequestHeader("X-Nav-NorskIdent") norskIdent: String) {
    }

    @GetMapping(PleiepengerSyktBarnRoutes.Urls.HenteSøknad, produces = ["application/json"])
    @Operation(
        summary = "Hente eksisterende mappe"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Søknaden",
                content = [Content(
                    schema = Schema(
                        implementation = SvarDto::class
                    )
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Mappen finnes ikke"
            )
        ]
    )
    fun HenteSøknad(
        @PathVariable("soeknad_id") søknadId: String,
    ) {
    }

    @PutMapping(
        PleiepengerSyktBarnRoutes.Urls.OppdaterEksisterendeSøknad,
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    @Operation(
        summary = "Oppdatere en søknad i en eksisterende mappe.",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Innhold på søknader er oppdatert og søknadene er klare for innsending.",
                content = [Content(
                    schema = Schema(
                        implementation = PleiepengerSøknadVisningDto::class
                    )
                )]
            )
        ]
    )
    fun OppdatereSøknad(
        @RequestBody søknad: PleiepengerSøknadVisningDto,
    ) {
    }


    @PostMapping(
        PleiepengerSyktBarnRoutes.Urls.SendEksisterendeSøknad,
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    @Operation(
        summary = "Sende inn søknad til behandling i saksbehsandlingssystemet.",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "202",
                description = "Søknaden er lukket for endring og sendt til behandling.",
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
                        implementation = SøknadFeil::class
                    )
                )]
            )
        ]
    )
    fun SendSøknad(
        @RequestBody søknad: OasSendSøknad,
    ) {
    }

    @PostMapping(
        PleiepengerSyktBarnRoutes.Urls.NySøknad,
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    @Operation(
        summary = "Starte en helt ny søknad",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Opprettet en mappe, bunke og en tom søknad. Jobb videre mot søknadIden for å oppdatere søknaden.",
                content = [Content(
                    schema = Schema(
                        implementation = PleiepengerSøknadVisningDto::class
                    )
                )]
            )
        ]
    )
    fun NySøknad(
        @RequestBody søknad: OasOpprettNySøknad,
    ) {
    }

    @PostMapping(
        PleiepengerSyktBarnRoutes.Urls.HentSøknadFraK9Sak,
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    @Operation(
        summary = "Hente siste psb søknad fra k9-sak",
        description = "Hente siste psb søknad fra k9-sak",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Henter siste pleiepengersøknad fra k9-sak og gjør den tilgjengelig for visning",
                content = [Content(
                    schema = Schema(
                        implementation = SvarDto::class
                    )
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Fant ingen gjeldene søknad"
            )
        ]
    )
    fun HentSøknadFraK9Sak(@RequestBody hentSøknad: OasHentSøknad) {
    }
}

// Disse klassene er nødvendige for å eksponere søknadsformatet, så lenge applikasjonen benytter userialisert json internt
data class OasHentSøknad(
    val norskIdent: NorskIdentDto,
    val periode: PeriodeDto,
)

data class OasInnsending(
    val norskIdent: NorskIdentDto,
    val journalpostId: JournalpostIdDto,
    val soeknad: SøknadJson,
    val soeknadId: SøknadIdDto
)

data class OasSendSøknad(
    val norskIdent: NorskIdentDto,
    val soeknadId: SøknadIdDto
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
        val søknader: List<SøknadDto<PleiepengerSøknadVisningDto>>?,
    )
}

data class OasPleiepengerSyktBarSoknadMapperSvar(
    val mapper: List<OasPleiepengerSyktBarSoknadMappeSvar>,
)

data class OasPleiepengerSyktBarnFeil(
    val mappeId: MappeId?,
    val feil: List<FeilDto>?,
) {
    data class FeilDto(
        val felt: String?,
        val feilkode: String?,
        val feilmelding: String?,
    )
}

data class OasPleiepengerSøknadDto<T>(
    val søknadId: SøknadIdDto,
    val erFraK9: Boolean = false,
    val søknad: T,
)

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
                        implementation = OasPdlResponse::class
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
    fun Hentident(@RequestBody body: OasHentPerson) {
    }
}

data class OasHentPerson(
    val norskIdent: NorskIdentDto,
)

data class OasPdlResponse(
    val person: PdlPersonDto
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

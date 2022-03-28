package no.nav.k9punsj.openapi

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.k9punsj.journalpost.IdentOgJournalpost
import no.nav.k9punsj.journalpost.JournalpostRoutes

@RestController
@SecurityScheme(
    name = "BearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
@Tag(name = "Journalposter", description = "Håndtering av journalposter")
internal class JournalpostOpenApi {
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

    @PostMapping(
        JournalpostRoutes.Urls.HentJournalposter,
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Hent journalposter som ikke er ferdig behandlet på person",
                content = [Content(
                    schema = Schema(
                        implementation = OasJournalpostIder::class
                    )
                )]
            ),
        ]
    )
    @Operation(
        summary = "Hent journalposter som ikke er ferdig behandlet på person",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    fun HentJournalposter(
        @RequestBody body: OasIdentDto,
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

    @PostMapping(JournalpostRoutes.Urls.SettPåVent, produces = ["application/json"])
    @Operation(
        summary = "Hente informasjon om en journalpost",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    fun SettPåVent(
        @PathVariable("journalpost_id") journalpostId: String,
        @RequestBody body: OasSøknadId,
    ) {
    }

    @PostMapping(JournalpostRoutes.Urls.LukkJournalpost, produces = ["application/json"])
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Lukket ok",
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
    fun LukkJournalpost(
        @PathVariable("journalpost_id") journalpostId: String,
    ) {
    }

    @PostMapping(JournalpostRoutes.Urls.SkalTilK9sak, produces = ["application/json"])
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "True når den skal til k9sak, false hvis den skal til infotrygd",
                content = [Content(
                    schema = Schema(
                        implementation = OasSkalTilInfotrygdSvar::class
                    )
                )]
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
        summary = "Sjekker om jornalposten må behandles av infotrygd",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    fun SkalTilK9Sak(
        @RequestBody body: OasPunsjBolleDto,
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

    @GetMapping(JournalpostRoutes.Urls.ResettInfoOmJournalpost, produces = ["application/json"])
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Når resett er gjennomført",
            ),
            ApiResponse(
                responseCode = "400",
                description = "Kan ikke endre på journalpost som har blitt sendt inn"
            ),
            ApiResponse(
                responseCode = "404",
                description = "Journalpost eksisterer ikke"
            )
        ]
    )
    @Operation(
        summary = "Resette journalpost",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    fun ResettInfoOmJournalpost(
        @PathVariable("journalpost_id") journalpostId: String,
    ) {
    }

    @GetMapping(JournalpostRoutes.Urls.HentHvaSomHarBlittSendtInn, produces = ["application/json"])
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Gir hva som har blitt sendt inn for journalposten",
                content = [Content(
                    schema = Schema(
                        implementation = no.nav.k9.søknad.Søknad::class
                    )
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Denne journalposten har ikke blitt sendt inn"
            ),
            ApiResponse(
                responseCode = "404",
                description = "Journalpost eksisterer ikke"
            )
        ]
    )
    @Operation(
        summary = "Gir som svar tilbake hva som har blitt sendt inn på journalposten",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    fun HentHvaSomHarBlittSendtInn(
        @PathVariable("journalpost_id") journalpostId: String,
    ) {
    }

    @GetMapping(JournalpostRoutes.Urls.LukkJournalpostDebugg, produces = ["application/json"])
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Hvis journalposten har blitt lukket",
            ),
            ApiResponse(
                responseCode = "400",
                description = "Denne journalposten har allerede blitt lukket"
            ),
            ApiResponse(
                responseCode = "404",
                description = "Journalpost eksisterer ikke i punsj"
            )
        ]
    )
    @Operation(
        summary = "Lukker en journalpost i k9-punsj og k9-los",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    fun LukkJournalpostDebugg(
        @PathVariable("journalpost_id") journalpostId: String,
    ) {
    }

    @PostMapping(JournalpostRoutes.Urls.JournalførPåGenerellSak, produces = ["application/json"])
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Hvis journalposten har blitt ferdigstilt på generell sak",
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error eller Doarkiv har fått Internal server error"
            )
        ]
    )
    @Operation(
        summary = "Journalfører journalposten mot generell sak og ferdigstiller",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    fun JournalførPåGenerellSak(
        @RequestBody body: IdentOgJournalpost,
    ) {
    }
}

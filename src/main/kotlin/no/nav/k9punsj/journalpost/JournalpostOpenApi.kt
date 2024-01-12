package no.nav.k9punsj.journalpost

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.k9punsj.felles.IdentOgJournalpost
import no.nav.k9punsj.journalpost.dto.IdentDto
import no.nav.k9punsj.journalpost.dto.JournalpostInfoDto
import no.nav.k9punsj.journalpost.dto.LukkJournalpostDto
import no.nav.k9punsj.openapi.OasJournalpostIder
import no.nav.k9punsj.openapi.OasSøknadId
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

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
        JournalpostRoutes.Urls.HentJournalposter,
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Hent journalposter som ikke er ferdig behandlet på person",
                content = [
                    Content(
                        schema = Schema(
                            implementation = OasJournalpostIder::class
                        )
                    )
                ]
            )
        ]
    )
    @Operation(
        summary = "Hent journalposter som ikke er ferdig behandlet på person",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    fun HentJournalposter(
        @RequestBody body: IdentDto
    ) {
    }

    @GetMapping(JournalpostRoutes.Urls.JournalpostInfo, produces = ["application/json"])
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Liste med dokumenter som er i journalposten.",
                content = [
                    Content(
                        schema = Schema(
                            implementation = JournalpostInfoDto::class
                        )
                    )
                ]
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
        @PathVariable("journalpost_id") journalpostId: String
    ) {
    }

    @PostMapping(JournalpostRoutes.Urls.SettPåVent, produces = ["application/json"])
    @Operation(
        summary = "Hente informasjon om en journalpost",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    fun SettPåVent(
        @PathVariable("journalpost_id") journalpostId: String,
        @RequestBody body: OasSøknadId
    ) {
    }

    @PostMapping(JournalpostRoutes.Urls.LukkJournalpost, produces = ["application/json"])
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Lukket ok"
            ),
            ApiResponse(
                responseCode = "404",
                description = "Journalpost eksisterer ikke"
            )
        ]
    )
    @Operation(
        summary = "Lukker oppgave i LOS og ferdigstiller gosysoppgave og journalpost",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    fun LukkJournalpost(
        @PathVariable("journalpost_id") journalpostId: String,
        @RequestBody lukkJournalpostDto: LukkJournalpostDto
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
        @PathVariable("dokument_id") dokumentId: String
    ) {
    }

    @GetMapping(JournalpostDriftRoutes.Urls.ResettInfoOmJournalpost, produces = ["application/json"])
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Når resett er gjennomført"
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
        security = [SecurityRequirement(name = "BearerAuth")],
        tags = ["Drift"]
    )
    fun ResettInfoOmJournalpost(
        @PathVariable("journalpost_id") journalpostId: String
    ) {
    }

    @GetMapping(JournalpostDriftRoutes.Urls.HentHvaSomHarBlittSendtInn, produces = ["application/json"])
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Gir hva som har blitt sendt inn for journalposten",
                content = [
                    Content(
                        schema = Schema(
                            implementation = no.nav.k9.søknad.Søknad::class
                        )
                    )
                ]
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
        security = [SecurityRequirement(name = "BearerAuth")],
        tags = ["Drift"]
    )
    fun HentHvaSomHarBlittSendtInn(
        @PathVariable("journalpost_id") journalpostId: String
    ) {
    }

    @GetMapping(JournalpostDriftRoutes.Urls.LukkJournalpostDebugg, produces = ["application/json"])
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Hvis journalposten har blitt lukket"
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
        security = [SecurityRequirement(name = "BearerAuth")],
        tags = ["Drift"]
    )
    fun LukkJournalpostDebugg(
        @PathVariable("journalpost_id") journalpostId: String
    ) {
    }

    @PostMapping(JournalpostDriftRoutes.Urls.LukkJournalposterDebugg, produces = ["application/json"])
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Hvis en eller flere journalposter har blitt lukket"
            ),
            ApiResponse(
                responseCode = "400",
                description = "Alle journalposter har allerede blitt lukket i punsj, eller alle er åpne i SAF"
            ),
            ApiResponse(
                responseCode = "404",
                description = "Ingen av journalpostene eksisterer ikke i punsj"
            )
        ]
    )
    @Operation(
        summary = "Lukker alle journalposter i k9-punsj og k9-los. Sjekker først om de er lukket i SAF",
        security = [SecurityRequirement(name = "BearerAuth")],
        tags = ["Drift"]
    )
    fun LukkJournalposterDebugg(
        @RequestBody body: JournalpostRoutes.JournalpostIderRequest
    ) {
    }

    @PostMapping(JournalpostRoutes.Urls.JournalførPåGenerellSak, produces = ["application/json"])
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Hvis journalposten har blitt ferdigstilt på generell sak"
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
        @RequestBody body: IdentOgJournalpost
    ) {
    }
}

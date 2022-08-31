package no.nav.k9punsj.integrasjoner.gosys

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.k9punsj.integrasjoner.gosys.GosysRoutes.Urls.GosysoppgaveIdKey
import no.nav.k9punsj.openapi.OasFeil
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Gosys", description = "Opprett journalføringsoppgave")
internal class GosysOpenApi {
    @PostMapping(
        GosysRoutes.Urls.OpprettJournalføringsoppgave,
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Oppretter journalføringsoppgave for fnummer og journalpostid"
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
            ),

            ApiResponse(
                responseCode = "500",
                description = "Eksisterer ikke",
                content = [
                    Content(
                        schema = Schema(
                            implementation = OasFeil::class
                        )
                    )
                ]
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

    @DeleteMapping(GosysRoutes.Urls.FerdigstillGosysoppgave, produces = ["application/json"])
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Oppgave lukket."
            ),
            ApiResponse(
                responseCode = "401",
                description = "Ikke innlogget"
            ),
            ApiResponse(
                responseCode = "403",
                description = "Ikke tilgang til å lukke gosysoppgaven"
            ),
            ApiResponse(
                responseCode = "404",
                description = "Gosysoppgave eksisterer ikke"
            ),
            ApiResponse(
                responseCode = "409",
                description = "Konflikt"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Ukjent feilsituasjon",
                content = [
                    Content(
                        schema = Schema(
                            implementation = OasFeil::class
                        )
                    )
                ]
            )
        ]
    )
    @Operation(
        summary = "Lukker gosysoppgave",
        description = "Lukker gosysoppgave",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    fun lukkGosysoppgave(@PathVariable(GosysoppgaveIdKey) gosysoppgaveId: String) {
    }

    @GetMapping(GosysRoutes.Urls.Gjelder, produces = ["application/json"])
    @Operation(
        summary = "Hente gyldige gjelder-verdier. (Public endpoint)",
        description = "Nøkkel-verdien brukes som 'gjelder' ved opprettelse av journalføringsoppgave."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Gyldige gjelder-verdier med tekst"
            )
        ]
    )
    fun HenteGjelder() {
    }
}

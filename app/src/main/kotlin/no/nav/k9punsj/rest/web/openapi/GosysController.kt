package no.nav.k9punsj.rest.web.openapi

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import no.nav.k9punsj.integrasjoner.gosys.GosysRoutes

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
                content = [Content(
                    schema = Schema(
                        implementation = OasFeil::class
                    )
                )]
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

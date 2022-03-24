package no.nav.k9punsj.openapi

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.k9punsj.fordel.FordelPunsjEventDto
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "HendelseMottaker", description = "Prosesserer")
internal class HendelseController {
    @PostMapping(
        "/prosesserHendelse/",
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
                        implementation = FordelPunsjEventDto::class
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
    fun ProsesserHendelse(@RequestBody body: FordelPunsjEventDto) {
    }
}

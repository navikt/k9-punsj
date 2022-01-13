package no.nav.k9punsj.rest.web.openapi

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import no.nav.k9punsj.rest.eksternt.pdl.PdlRoutes

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

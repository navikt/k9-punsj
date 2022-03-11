package no.nav.k9punsj.sak

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.tags.Tag

@RestController
@SecurityScheme(
    name = "BearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
@Tag(name = "Saker", description = "Håndtering av saker")
internal class SakerController {
    @PostMapping(SakerRoutes.Urls.HentSaker, produces = ["application/json"])
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Hvis saker hentes",
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error eller saf har fått Internal server error"
            )
        ]
    )
    @Operation(
        summary = "Henter saker",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    fun hentSaker(
        @RequestBody body: SakerRoutes.HentSakerRequest,
    ) {
    }
}

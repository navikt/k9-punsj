package no.nav.k9punsj.notat

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
@Tag(name = "Notater", description = "Håndtering av Notater")
internal class NotatController {
    @PostMapping(NotatRoutes.Urls.OpprettNotat, produces = ["application/json"])
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Hvis notaten har blitt opprettet",
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error eller Doarkiv har fått Internal server error"
            )
        ]
    )
    @Operation(
        summary = "Oppretter ny Notat i dokarkiv",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    fun opprettNotat(
        @RequestBody body: NyNotat,
    ) {
    }
}

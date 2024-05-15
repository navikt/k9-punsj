package no.nav.k9punsj.notat

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.k9punsj.openapi.OpenApi
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Notater", description = "Håndtering av Notater")
internal class NotatController {
    @PostMapping(NotatRoutes.Urls.OpprettNotat, produces = ["application/json"])
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Hvis notaten har blitt opprettet"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error eller Doarkiv har fått Internal server error"
            )
        ]
    )
    @Operation(
        summary = "Oppretter ny Notat i dokarkiv",
        security = [SecurityRequirement(name = OpenApi.OAUTH2)]
    )
    fun opprettNotat(
        @RequestBody body: NyNotat
    ) {
    }
}

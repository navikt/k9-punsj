package no.nav.k9punsj.forvaltning

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.k9punsj.openapi.OpenApi.SecuurityScheme.OAUTH2
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@SecurityScheme(
    name = "BearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = OAUTH2,
    bearerFormat = "JWT"
)
@Tag(name = "Forvaltning", description = "Håndtering av forvaltningsoppgaver")
internal class ForvaltningOpenApi {
    @PostMapping(ForvaltningRoutes.Urls.SlettMappeMedAlleTilkoblinger)
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Alle mapper med bunker og søknader er slettet",
            ),
            ApiResponse(
                responseCode = "500",
                description = "Uventet feil",
                content = [
                    Content(
                        schema = Schema(
                            implementation = ProblemDetail::class
                        )
                    )
                ]
            )
        ]
    )
    @Operation(
        summary = "Sletter alle mapper med tilknyttede søknader og bunker",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    fun håndterSlettingAvMapper() {
    }
}

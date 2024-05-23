package no.nav.k9punsj.journalpost.postmottak

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
@SecurityScheme(
    name = "BearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
@Tag(name = "Postmottak", description = "Håndtering av mottak av journalposter")
internal class PostMottakOpenApi {
    @PostMapping(PostMottakRoutes.Urls.Mottak)
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "Journalposten er oppdatert med ny data & akjsonspunkt sendt til k9-los"
            ),
            ApiResponse(
                responseCode = "403",
                description = "Saksbehandler har ikke tilgang"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Uventet feil"
            )
        ]
    )
    @Operation(
        summary = "Oppdaterer journalpost med ny data & sender aksjonspunkt med oppdatering til k9-los.",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    fun håndterMottak(
        @RequestHeader("X-Nav-NorskIdent") norskIdent: String,
        @RequestBody body: JournalpostMottaksHaandteringDto,
    ) {
    }
}

package no.nav.k9punsj.sak

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.openapi.OpenApi
import no.nav.k9punsj.sak.dto.SakInfoDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Saker", description = "Håndtering av saker")
internal class SakerController {
    @GetMapping(SakerRoutes.Urls.HentSaker, produces = ["application/json"])
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Hvis saker hentes",
                content = [
                    Content(
                        array = ArraySchema(
                            schema = Schema(
                                implementation = SakInfoDto::class
                            )
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error eller saf har fått Internal server error"
            ),
            ApiResponse(
                responseCode = "403",
                description = "Saksbehandler har ikke tilgang til saker for søker."
            )
        ]
    )
    @Operation(
        summary = "Henter saker",
        security = [SecurityRequirement(name = OpenApi.OAUTH2)]
    )
    fun getHentSaker(@RequestHeader("X-Nav-NorskIdent") norskIdent: String) {
    }

    @PostMapping(SakerRoutes.Urls.HentSaker, consumes = ["application/json"], produces = ["application/json"])
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Hvis saker hentes",
                content = [
                    Content(
                        array = ArraySchema(
                            schema = Schema(
                                implementation = SakInfoDto::class
                            )
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error eller saf har fått Internal server error"
            ),
            ApiResponse(
                responseCode = "403",
                description = "Saksbehandler har ikke tilgang til saker for søker."
            )
        ]
    )
    @Operation(
        summary = "Henter saker",
        security = [SecurityRequirement(name = OpenApi.OAUTH2)]
    )
    fun postHentSaker(@RequestBody norskIdent: String) {
    }

    @PostMapping(SakerRoutes.Urls.HentPerioder, consumes = ["application/json"], produces = ["application/json"])
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Hvis sakens perioder hentes",
                content = [
                    Content(
                        array = ArraySchema(
                            schema = Schema(
                                implementation = PeriodeDto::class
                            )
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error eller saf har fått Internal server error"
            ),
            ApiResponse(
                responseCode = "403",
                description = "Saksbehandler har ikke tilgang til saken."
            )
        ]
    )
    @Operation(
        summary = "Henter perioder",
        security = [SecurityRequirement(name = OpenApi.OAUTH2)]
    )
    fun hentPerioder(@RequestParam("saksnummer") saksnummer: String) {
    }

    @PostMapping(SakerRoutes.Urls.GjenåpneHistoriskSak, consumes = ["application/json"], produces = ["application/json"])
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Hvis saken gjenåpnes",
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error eller saf har fått Internal server error"
            ),
            ApiResponse(
                responseCode = "403",
                description = "Saksbehandler har ikke tilgang til saken."
            )
        ]
    )
    @Operation(
        summary = "Gjenåpner historisk sak",
        security = [SecurityRequirement(name = OpenApi.OAUTH2)]
    )
    fun gjenåpneHistoriskSak(@RequestParam("saksnummer") saksnummer: String) {
    }
}

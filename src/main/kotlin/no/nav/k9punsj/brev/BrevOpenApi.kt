package no.nav.k9punsj.brev

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.k9.formidling.kontrakt.hendelse.Dokumentbestilling
import no.nav.k9punsj.brev.dto.DokumentbestillingDto
import no.nav.k9punsj.openapi.OasFeil
import no.nav.k9punsj.openapi.OpenApi
import org.springframework.web.bind.annotation.*

@RestController
@Tag(
    name = "Brev",
    description = "Håndtering av brevbestillinger fra punsj til k9formidling via kafka"
)
internal class BrevOpenApi {
    @PostMapping(
        BrevRoutes.Urls.BestillBrev,
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    @Operation(
        summary = "Sender en brevbestilling med gitt mal og innhold",
        description = "Sender en brevbestilling med gitt mal og innhold",
        security = [SecurityRequirement(name = OpenApi.OAUTH2)]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Bestillt brev.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = Dokumentbestilling::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Innsending feilet grunnet mangler i bestillingen.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = OasFeil::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Innsending feilet grunnet intern feil.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = OasFeil::class)
                    )
                ]
            )
        ]
    )
    fun BestillBrev(
        @RequestBody bestilling: DokumentbestillingDto
    ) {
    }
}

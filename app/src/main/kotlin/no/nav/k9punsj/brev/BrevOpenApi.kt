package no.nav.k9punsj.brev

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.k9punsj.brev.dto.DokumentbestillingDto
import org.springframework.web.bind.annotation.*
import no.nav.k9punsj.openapi.OasFeil

@RestController
@Tag(name = "Brev-bestilling", description = "Håndtering av brevbestillinger fra punsj til k9formidling via kafka")
internal class BrevOpenApi {
    @PostMapping(
        BrevRoutes.Urls.BestillBrev,
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    @Operation(
        summary = "Sender en brevbestilling med gitt mal og innhold",
        description = "Sender en brevbestilling med gitt mal og innhold",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "Bestillt brev."
            ),
            ApiResponse(
                responseCode = "400",
                description = "Innsending feilet grunnet mangler i bestillingen.",
                content = [Content(
                    schema = Schema(
                        implementation = OasFeil::class
                    )
                )]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Innsending feilet grunnet intern feil.",
                content = [Content(
                    schema = Schema(
                        implementation = OasFeil::class
                    )
                )]
            )
        ]
    )
    fun BestillBrev(
        @RequestBody bestilling: DokumentbestillingDto,
    ) {
    }
}

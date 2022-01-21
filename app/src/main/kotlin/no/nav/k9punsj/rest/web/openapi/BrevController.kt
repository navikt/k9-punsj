package no.nav.k9punsj.rest.web.openapi

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*
import no.nav.k9punsj.brev.BrevVisningDto
import no.nav.k9punsj.brev.DokumentbestillingDto
import no.nav.k9punsj.rest.web.dto.AktørIdDto
import no.nav.k9punsj.rest.web.ruter.BrevRoutes

@RestController
@Tag(name = "Brev-bestilling", description = "Håndtering av brevbestillinger fra punsj til k9formidling via kafka")
internal class BrevController {
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
                responseCode = "200",
                description = "Bestiller brev",
                content = [Content(
                    schema = Schema(
                        implementation = DokumentbestillingDto::class
                    )
                )]
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
        ]
    )
    fun BestillBrev(
        @RequestBody bestilling: DokumentbestillingDto,
    ) {
    }

    @GetMapping(BrevRoutes.Urls.HentAlleBrev, produces = ["application/json"])
    @Operation(
        summary = "Sender en brevbestilling med gitt mal og innhold",
        description = "Sender en brevbestilling med gitt mal og innhold",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Bestiller brev",
                content = [Content(
                    schema = Schema(
                        implementation = BrevVisningDto::class
                    )
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Fant ingen brev",
            )
        ]
    )
    fun HentAlleBrev(
        @PathVariable("journalpost_id") journalpostId: String,
    ) {
    }

    @GetMapping(BrevRoutes.Urls.HentAktørId, produces = ["application/json"])
    @Operation(
        summary = "Sender en brevbestilling med gitt mal og innhold",
        description = "Sender en brevbestilling med gitt mal og innhold",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Gir aktørId",
                content = [Content(
                    schema = Schema(
                        implementation = AktørIdDto::class
                    )
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Klarte ikke hent aktør for gitt fnr",
            )
        ]
    )
    fun HentAktørId(
        @RequestHeader("X-Nav-NorskIdent") norskIdent: String
    ) {
    }
}

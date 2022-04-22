package no.nav.k9punsj.omsorgspengerkronisksyktbarn

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.k9punsj.felles.dto.SendSøknad
import no.nav.k9punsj.domenetjenester.dto.SøknadFeil
import no.nav.k9punsj.felles.IdentOgJournalpost
import org.springframework.web.bind.annotation.*
import no.nav.k9punsj.openapi.OasFeil

@RestController
@Tag(name = "Omsorgspenger kronisk sykt barn søknad", description = "Håndtering av søknader av typen omsorgspenger - kronisk sykt barn")
internal class OmsorgspengerKroniskSykBarnSoknadOpenApi {
    @GetMapping(OmsorgspengerKroniskSyktBarnRoutes.Urls.HenteMappe, produces = ["application/json"])
    @Operation(
        summary = "Henter data på person for omsorgspenger kronisk sykt barn",
        description = "Sender NorskIdent til person i header.",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Henter mappen til en person som inneholder alle søknader",
                content = [Content(
                    schema = Schema(
                        implementation = OmsorgspengerKroniskSyktBarnSøknadDto::class
                    )
                )]
            )
        ]
    )
    fun HenteMappe(@RequestHeader("X-Nav-NorskIdent") norskIdent: String) {
    }

    @PostMapping(
        OmsorgspengerKroniskSyktBarnRoutes.Urls.NySøknad,
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    @Operation(
        summary = "Starte en helt ny søknad",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Opprettet en mappe, bunke og en tom søknad. Jobb videre mot søknadIden for å oppdatere søknaden.",
                content = [Content(
                    schema = Schema(
                        implementation = OmsorgspengerKroniskSyktBarnSøknadDto::class
                    )
                )]
            )
        ]
    )
    fun NySøknad(
        @RequestBody søknad: IdentOgJournalpost,
    ) {
    }

    @GetMapping(OmsorgspengerKroniskSyktBarnRoutes.Urls.HenteSøknad, produces = ["application/json"])
    @Operation(
        summary = "Hente eksisterende mappe"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Søknaden",
                content = [Content(
                    schema = Schema(
                        implementation = SvarOmsKSBDto::class
                    )
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Mappen finnes ikke"
            )
        ]
    )
    fun HenteSøknad(
        @PathVariable("soeknad_id") søknadId: String,
    ) {
    }

    @PutMapping(
        OmsorgspengerKroniskSyktBarnRoutes.Urls.OppdaterEksisterendeSøknad,
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    @Operation(
        summary = "Oppdatere en søknad i en eksisterende mappe.",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Innhold på søknader er oppdatert og søknadene er klare for innsending.",
                content = [Content(
                    schema = Schema(
                        implementation = OmsorgspengerKroniskSyktBarnSøknadDto::class
                    )
                )]
            )
        ]
    )
    fun OppdatereSøknad(
        @RequestBody søknad: OmsorgspengerKroniskSyktBarnSøknadDto,
    ) {
    }

    @PostMapping(
        OmsorgspengerKroniskSyktBarnRoutes.Urls.SendEksisterendeSøknad,
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    @Operation(
        summary = "Sende inn søknad til behandling i saksbehsandlingssystemet.",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "202",
                description = "Søknaden er lukket for endring og sendt til behandling.",
                content = [Content(
                    schema = Schema(
                        implementation = no.nav.k9.søknad.Søknad::class
                    )
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Innsending feilet grunnet mangler i søknaden.",
                content = [Content(
                    schema = Schema(
                        implementation = SøknadFeil::class
                    )
                )]
            ), ApiResponse(
                responseCode = "409",
                description = "En eller flere journalposter har blitt sendt inn fra før",
                content = [Content(
                    schema = Schema(
                        implementation = OasFeil::class
                    )
                )]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Hvis det feiler uventet på server",
                content = [Content(
                    schema = Schema(
                        implementation = OasFeil::class
                    )
                )]
            )
        ]
    )
    fun SendSøknad(
        @RequestBody søknad: SendSøknad,
    ) {
    }

    @PostMapping(
        OmsorgspengerKroniskSyktBarnRoutes.Urls.ValiderSøknad,
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    @Operation(
        summary = "Valider søknad mot k9-format sin kontrakt",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "202",
                description = "Søknaden er valider ok.",
                content = [Content(
                    schema = Schema(
                        implementation = no.nav.k9.søknad.Søknad::class
                    )
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Innsending feilet grunnet mangler i søknaden.",
                content = [Content(
                    schema = Schema(
                        implementation = SøknadFeil::class
                    )
                )]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Hvis det feiler uventet på server",
                content = [Content(
                    schema = Schema(
                        implementation = OasFeil::class
                    )
                )]
            )
        ]
    )
    fun ValiderSøknad(
        @RequestBody søknad: SendSøknad,
    ) {
    }
}
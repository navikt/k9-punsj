package no.nav.k9punsj.pleiepengersyktbarn

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.k9punsj.felles.IdentOgJournalpost
import no.nav.k9punsj.felles.dto.Matchfagsak
import no.nav.k9punsj.felles.dto.PerioderDto
import no.nav.k9punsj.felles.dto.SendSøknad
import no.nav.k9punsj.felles.dto.SøknadFeil
import no.nav.k9punsj.openapi.OasFeil
import no.nav.k9punsj.openapi.OpenApi
import org.springframework.web.bind.annotation.*

@RestController
@Tag(name = "Pleiepenger sykt barn søknad", description = "Håndtering av papirsøknader")
internal class PleiepengerSyktBarnSoknadOpenApi {
    @GetMapping(PleiepengerSyktBarnRoutes.Urls.HenteMappe, produces = ["application/json"])
    @Operation(
        summary = "Henter mappen til en person som inneholder søknader.",
        description = "Sendes NorskIdente til person som headere.",
        security = [SecurityRequirement(name = OpenApi.OAUTH2)]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Henter mappen til en person som inneholder alle søknader",
                content = [
                    Content(
                        schema = Schema(
                            implementation = PleiepengerSyktBarnSøknadDto::class
                        )
                    )
                ]
            )
        ]
    )
    fun HenteMappe(@RequestHeader("X-Nav-NorskIdent") norskIdent: String) {
    }

    @GetMapping(PleiepengerSyktBarnRoutes.Urls.HenteSøknad, produces = ["application/json"])
    @Operation(
        summary = "Hente eksisterende mappe"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Søknaden",
                content = [
                    Content(
                        schema = Schema(
                            implementation = SvarPsbDto::class
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Mappen finnes ikke"
            )
        ]
    )
    fun HenteSøknad(
        @PathVariable("soeknad_id") søknadId: String
    ) {
    }

    @PutMapping(
        PleiepengerSyktBarnRoutes.Urls.OppdaterEksisterendeSøknad,
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    @Operation(
        summary = "Oppdatere en søknad i en eksisterende mappe.",
        security = [SecurityRequirement(name = OpenApi.OAUTH2)]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Innhold på søknader er oppdatert og søknadene er klare for innsending.",
                content = [
                    Content(
                        schema = Schema(
                            implementation = PleiepengerSyktBarnSøknadDto::class
                        )
                    )
                ]
            )
        ]
    )
    fun OppdatereSøknad(
        @RequestBody søknad: PleiepengerSyktBarnSøknadDto
    ) {
    }

    @PostMapping(
        PleiepengerSyktBarnRoutes.Urls.SendEksisterendeSøknad,
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    @Operation(
        summary = "Sende inn søknad til behandling i saksbehsandlingssystemet.",
        security = [SecurityRequirement(name = OpenApi.OAUTH2)]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "202",
                description = "Søknaden er lukket for endring og sendt til behandling.",
                content = [
                    Content(
                        schema = Schema(
                            implementation = no.nav.k9.søknad.Søknad::class
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Innsending feilet grunnet mangler i søknaden.",
                content = [
                    Content(
                        schema = Schema(
                            implementation = SøknadFeil::class
                        )
                    )
                ]
            ), ApiResponse(
                responseCode = "409",
                description = "En eller flere journalposter har blitt sendt inn fra før",
                content = [
                    Content(
                        schema = Schema(
                            implementation = OasFeil::class
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Hvis det feiler uventet på server",
                content = [
                    Content(
                        schema = Schema(
                            implementation = OasFeil::class
                        )
                    )
                ]
            )
        ]
    )
    fun SendSøknad(
        @RequestBody søknad: SendSøknad
    ) {
    }

    @PostMapping(
        PleiepengerSyktBarnRoutes.Urls.ValiderSøknad,
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    @Operation(
        summary = "Valider søknad mot k9-format sin kontrakt",
        security = [SecurityRequirement(name = OpenApi.OAUTH2)]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "202",
                description = "Søknaden er valider ok.",
                content = [
                    Content(
                        schema = Schema(
                            implementation = no.nav.k9.søknad.Søknad::class
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Innsending feilet grunnet mangler i søknaden.",
                content = [
                    Content(
                        schema = Schema(
                            implementation = SøknadFeil::class
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Hvis det feiler uventet på server",
                content = [
                    Content(
                        schema = Schema(
                            implementation = OasFeil::class
                        )
                    )
                ]
            )
        ]
    )
    fun ValiderSøknad(
        @RequestBody søknad: SendSøknad
    ) {
    }

    @PostMapping(
        PleiepengerSyktBarnRoutes.Urls.NySøknad,
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    @Operation(
        summary = "Starte en helt ny søknad",
        security = [SecurityRequirement(name = OpenApi.OAUTH2)]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Opprettet en mappe, bunke og en tom søknad. Jobb videre mot søknadIden for å oppdatere søknaden.",
                content = [
                    Content(
                        schema = Schema(
                            implementation = PleiepengerSyktBarnSøknadDto::class
                        )
                    )
                ]
            )
        ]
    )
    fun NySøknad(
        @RequestBody søknad: IdentOgJournalpost
    ) {
    }

    @PostMapping(
        PleiepengerSyktBarnRoutes.Urls.HentInfoFraK9sak,
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    @Operation(
        summary = "Henter perioder som ligger i k9-sak",
        description = "Henter perioder som ligger i k9-sak",
        security = [SecurityRequirement(name = OpenApi.OAUTH2)]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Henter siste pleiepengersøknad fra k9-sak og gjør den tilgjengelig for visning",
                content = [
                    Content(
                        schema = Schema(
                            implementation = PerioderDto::class
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Fant ingen gjeldene søknad"
            )
        ]
    )
    fun HentInfoFraK9sak(@RequestBody matchFagsak: Matchfagsak) {
    }
}

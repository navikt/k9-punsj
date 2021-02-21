import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.k9punsj.db.datamodell.MappeId
import no.nav.k9punsj.db.datamodell.NorskIdent
import no.nav.k9punsj.rest.web.JournalpostInnhold
import no.nav.k9punsj.rest.web.dto.NorskIdentDto
import no.nav.k9punsj.rest.web.dto.PersonDTO
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadDto
import no.nav.k9punsj.rest.web.ruter.PleiepengerSyktBarnRoutes
import org.springframework.web.bind.annotation.*

@RestController
@Tag(name = "Pleiepenger sykt barn søknad", description = "Håndtering av papirsøknader")
internal class PleiepengerSyktBarnSoknadController {
    @GetMapping(PleiepengerSyktBarnRoutes.Urls.HenteMapper, produces = ["application/json"])
    @Operation(
        summary = "Hente eksisterende mapper på en person som inneholder ufullstendige søknader.",
        description = "Kan sendes en eller fler NorskIdenter som headere. Viser kun mapper hvor alle Norske Identer er med i."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Alle mapper lagret på personen(e) sendt inn.",
                content = [Content(
                    schema = Schema(
                        implementation = OasPleiepengerSyktBarSoknadMapperSvar::class
                    )
                )]
            )
        ]
    )
    fun HenteMapper(@RequestHeader("X-Nav-NorskIdent") norskIdenter: Set<String>) {    }

    @GetMapping(PleiepengerSyktBarnRoutes.Urls.EksisterendeSøknad, produces = ["application/json"])
    @Operation(
        summary = "Hente eksisterende mappe"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Mappen",
                content = [Content(
                    schema = Schema(
                        implementation = OasPleiepengerSyktBarSoknadMappeSvar::class
                    )
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Mappen finnes ikke"
            )
        ]
    )
    fun HenteMappe(
        @PathVariable("mappe_id") mappeId: String,
    ) {
    }


    @PutMapping(
        PleiepengerSyktBarnRoutes.Urls.EksisterendeSøknad,
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    @Operation(summary = "Oppdatere en søknad i en eksisterende mappe.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Innhold på søknader er oppdatert og søknadene er klare for innsending.",
                content = [Content(
                    schema = Schema(
                        implementation = OasPleiepengerSyktBarSoknadMappeSvar::class
                    )
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Innhold på søknader er oppdatert, men inneholder fortsatt mangler.",
                content = [Content(
                    schema = Schema(
                        implementation = OasPleiepengerSyktBarSoknadMapperSvar::class
                    )
                )]
            )
        ]
    )
    fun OppdatereSøknad(
        @PathVariable("mappe_id") mappeId: String,
        @RequestBody søknad: OasInnsending,
    ) {
    }


    @PostMapping(
        PleiepengerSyktBarnRoutes.Urls.EksisterendeSøknad,
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    @Operation(summary = "Sende inn søknad til behandling i saksbehsandlingssystemet.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "202",
                description = "Søknaden er sendt til behandling og personen fjernet fra mappen.",
                content = [Content(
                    schema = Schema(
                        implementation = Void::class
                    )
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Innsending feilet grunnet mangler i søknaden.",
                content = [Content(
                    schema = Schema(
                        implementation = OasPleiepengerSyktBarnFeil::class
                    )
                )]
            )
        ]
    )
    fun SendSøknad(
        @PathVariable("mappe_id") mappeId: String,
        @RequestHeader("X-Nav-NorskIdent") norskIdenter: String,
    ) {
    }

    @PostMapping(
        PleiepengerSyktBarnRoutes.Urls.NySøknad,
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    @Operation(summary = "Starte en helt ny søknad")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Opprettet mappe for en ny søknad. Se 'Location' header for URL til mappen.",
                content = [Content(
                    schema = Schema(
                        implementation = OasPleiepengerSyktBarSoknadMappeSvar::class
                    )
                )]
            )
        ]
    )
    fun NySøknad(
        @RequestBody søknad: OasInnsending,
    ) {
    }

    @PostMapping(
        PleiepengerSyktBarnRoutes.Urls.HentSøknadFraK9Sak,
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    @Operation(summary = "Hente siste psb søknad fra k9-sak")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Viser den siste gjeldene søknaden som ligger i k9-sak",
                content = [Content(
                    schema = Schema(
                        implementation = PleiepengerSøknadDto::class
                    )
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Fant ingen gjeldene søknad"
            )
        ]
    )
    fun HentSøknadFraK9Sak(
        @RequestHeader("X-Nav-NorskIdent") norskIdenter: Set<String>,
        @RequestBody hentSøknad: OasHentSøknad,
    ) {
    }
}

// Disse klassene er nødvendige for å eksponere søknadsformatet, så lenge applikasjonen benytter userialisert json internt
data class OasHentSøknad(
    val norskIdent: NorskIdentDto,
    val periode: String
)

data class OasInnsending(
    val personer: Map<String, JournalpostInnhold<PleiepengerSøknadDto>>,
)

data class OasPleiepengerSyktBarSoknadMappeSvar(
    val mappeId: MappeId,
    val personer: MutableMap<NorskIdent, PersonDTO<PleiepengerSøknadDto>>?,
)

data class OasPleiepengerSyktBarSoknadMapperSvar(
    val mapper: List<OasPleiepengerSyktBarSoknadMappeSvar>,
)

data class OasPleiepengerSyktBarnFeil(
    val mappeId: MappeId?,
    val feil: List<FeilDto>?,
){
    data class FeilDto(
        val felt: String?,
        val feilkode: String?,
        val feilmelding: String?,
    )
}

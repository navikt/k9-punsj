/*
package no.nav.k9

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.models.*
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import no.nav.k9.fagsak.FagsakRoutes
import no.nav.k9.journalpost.JournalpostRoutes
import no.nav.k9.pleiepengersyktbarn.soknad.FellesDel
import no.nav.k9.pleiepengersyktbarn.soknad.PersonligDel
import no.nav.k9.pleiepengersyktbarn.soknad.PleiepengerSyktBarnRoutes
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.time.LocalDate

@Component
internal class OpenApi {


    @Bean
    internal fun openApi(
            @Value("\${no.nav.navn}") navn: String,
            @Value("\${no.nav.beskrivelse}") beskrivelse: String,
            @Value("\${no.nav.versjon}") versjon: String,
            @Value("\${no.nav.swagger_server_base_url}") swaggerServerBaseUrl: URI,
            @Value("\${no.nav.security.jwt.client.azure.client_id}") azureClientId: String
    ): OpenAPI = OpenAPI()
            .addServersItem(Server().url("$swaggerServerBaseUrl/api").description("Swagger Server"))
            .info(
                    Info()
                            .title(navn)
                            .description("$beskrivelse\n\nScope for å tjenesten: `$azureClientId/.default`")
                            .version(versjon)
                            .contact(
                                    Contact()
                                            .name("Arbeids- og velferdsdirektoratet")
                                            .url("https://www.nav.no")
                            )
                            .license(
                                    License()
                                            .name("MIT")
                                            .url("https://github.com/navikt/k9-punsj/blob/master/LICENSE")
                            )
            )
}

@RestController
@Tag(name = "Pleiepenger sykt barn søknad", description = "Håndtering av papirsøknader")
internal class PleiepengerSyktBarnSoknadController {
    @GetMapping(PleiepengerSyktBarnRoutes.Urls.HenteMapper, produces = ["application/json"])
    @Operation(summary = "Hente eksisterende mapper på en person som inneholder ufullstendige søknader.")
    fun HenteMapper(@PathVariable("norsk_ident") norskIdent: String
    ) : Set<OasPleiepengerSyktBarnSoknadMappe> = setOf()

    @PutMapping(PleiepengerSyktBarnRoutes.Urls.EksisterendeSøknad, consumes = ["application/json"], produces = ["application/json"])
    @Operation(summary = "Oppdatere en søknad i en eksisterende mappe.")
    @ApiResponses(value = [
        ApiResponse(
                responseCode = "200",
                description = "Innhold på søknad er oppdatert og søknaden er klar for innsending.",
                content = [Content(
                        schema = Schema(
                                implementation = OasPleiepengerSyktBarnSoknadMappe::class
                        )
                )]
        ),
        ApiResponse(
                responseCode = "400",
                description = "Innhold på søknad er oppdatert, men inneholder fortsatt mangler.",
                content = [Content(
                        schema = Schema(
                                implementation = OasPleiepengerSyktBarnSoknadMappe::class
                        )
                )]
        )
    ])
    fun OppdatereSøknad(
            @PathVariable("mappe_id") mappeId: String,
            @RequestBody søknad: OasPleiepengerSykBarnInnsending<Innsen>
    ) {}

    @PostMapping(PleiepengerSyktBarnRoutes.Urls.EksisterendeSøknad, consumes = ["application/json"], produces = ["application/json"])
    @Operation(summary = "Sende inn søknad til behandling i saksbehsandlingssystemet.")
    @ApiResponses(value = [
        ApiResponse(
                responseCode = "202",
                description = "Søknaden er sendt til behandling og mappen slettet.",
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
                                implementation = OasPleiepengerSyktBarnSoknadMappe::class
                        )
                )]
        )
    ])
    fun SendSøknad(
            @PathVariable("mappe_id") mappeId: String
    ) {}

    @PostMapping(PleiepengerSyktBarnRoutes.Urls.NySøknad,
            consumes = ["application/json"],
            produces = ["application/json"]
    )
    @Operation(summary = "Starte en helt ny søknad")
    @ApiResponses(value = [
        ApiResponse(
                responseCode = "201",
                description = "Opprettet mappe for en ny søknad. Se 'Location' header for URL til mappen.",
                content = [Content(
                        schema = Schema(
                                implementation = OasPleiepengerSyktBarnSoknadMappe::class
                        )
                )]
        )
    ])
    fun NySøknad(
            @RequestBody søknad: OasPleiepengerSykBarnInnsending<Innsending>
    ){}
}

data class OasPleiepengerSykBarnInnsending(
        val felles_innhold: FellesDel,
        val personlig_innhold: Map<String, PersonligDel>
)

data class OasMangel(
        val attributt: String,
        val ugyldig_verdi: Any?,
        val melding: String
)
data class OasPleiepengerSyktBarnSoknadMappe(
        val mappe_id : String,
        val innsendinger: Set<String>,
        val innhold: Søknad,
        val mangler: Set<OasMangel>
)

@RestController
@SecurityScheme(
    name = "BearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "jwt"
)
@Tag(name = "Journalposter", description = "Håndtering av journalposter")
internal class JournalpostController {
    @GetMapping(JournalpostRoutes.Urls.HenteJournalpostInfo, produces = ["application/json"])
    @ApiResponses(value = [
        ApiResponse(
                responseCode = "200",
                description = "Liste med dokumenter som er i journalposten.",
                content = [Content(
                        schema = Schema(
                                implementation = OasJournalpostInfo::class
                        )
                )]
        ),
        ApiResponse(
                responseCode = "400",
                description = "Journalpost kan ikke håndteres i Punsj"
        ),
        ApiResponse(
                responseCode = "401",
                description = "Ikke innlogget"
        ),
        ApiResponse(
                responseCode = "403",
                description = "Ikke tilgang til journalposten"
        ),
        ApiResponse(
                responseCode = "404",
                description = "Journalpost eksisterer ikke"
        )
    ])
    @Operation(
            summary = "Hente informasjon om en journalpost",
            security = [SecurityRequirement(name = "BearerAuth")]
    )
    fun HenteJournalpostInfo(
            @PathVariable("journalpost_id") journalpostId : String){}
    @GetMapping(JournalpostRoutes.Urls.HenteDokument, produces = ["application/pdf"])
    @ApiResponses(value = [
        ApiResponse(
                responseCode = "200",
                description = "Dokumentet."
        ),
        ApiResponse(
                responseCode = "401",
                description = "Ikke innlogget"
        ),
        ApiResponse(
                responseCode = "403",
                description = "Ikke tilgang til journalposten"
        ),
        ApiResponse(
                responseCode = "404",
                description = "Journalpost eksisterer ikke"
        )
    ])
    @Operation(
            summary = "Hente dokumentet",
            security = [SecurityRequirement(name = "BearerAuth")]
    )
    fun HenteDokument(
            @PathVariable("journalpost_id") journalpostId : String,
            @PathVariable("dokument_id") dokumentId : String){}
}

data class OasDokumentInfo(
        val dokument_id: String
)
data class OasJournalpostInfo(
        val dokumenter : Set<OasDokumentInfo>
)

@RestController
@Tag(name = "Fagsaker", description = "Liste fagsaker")
internal class FagsakerController {
    @GetMapping(FagsakRoutes.Urls.HenteFagsakinfo, produces = ["application/json"])
    @ApiResponses(value = [
        ApiResponse(
                responseCode = "200",
                description = "Liste med fagsaker tilknyttet personen.",
                content = [Content(
                        schema = Schema(
                                implementation = OasFagsakListe::class
                        )
                )]
        )
    ])
    @Operation(summary = "Hente liste med fagsaker tilknyttet personen.", description = "ytelse må være 'pleiepenger-sykt-barn'")
    fun HenteFagsaker(
            @RequestParam("ytelse") ytelse: String,
            @PathVariable("norsk_ident") norskIdent : String){}
}

data class OasFagsakListe(
        val fagsaker: Set<OasFagsak>
)
data class OasFagsak(
        val fagsak_id: String,
        val url: String,
        val fra_og_med: LocalDate,
        val til_og_med: LocalDate?,
        val barn: OasFagsakBarn
)
data class OasFagsakBarn(
        val fødselsdato: LocalDate,
        val navn: String
)*/

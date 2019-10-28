package no.nav.k9

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.models.*
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import no.nav.k9.pleiepengersyktbarn.soknad.PleiepengerSyktBarnRoutes
import no.nav.k9.pleiepengersyktbarn.soknad.Søknad
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*

@Component
internal class OpenApi {
    @Bean
    internal fun openApi(
            @Value("\${nav.navn}") navn: String,
            @Value("\${nav.beskrivelse}") beskrivelse: String,
            @Value("\${nav.versjon}") versjon: String,
            @Value("\${nav.swagger-url:http://localhost:8080}") swaggerUrl: String
    ): OpenAPI = OpenAPI()
            .addServersItem(Server().url("$swaggerUrl/api").description("Swagger Server"))
            .info(
                    Info()
                            .title(navn)
                            .description(beskrivelse)
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
@Tag(name = "Pleiepenger Sykt Barn Søknad", description = "Håndterer domain av papirsøknader")
internal class PleiepengerSyktBarnSoknadController {
    @GetMapping(PleiepengerSyktBarnRoutes.Urls.HenteMapper, produces = ["application/json"])
    @Operation(summary = "Hente lagrede mapper på peronen med ufullstendige søknader")
    fun HenteMapper(@PathVariable("norsk_ident") norskIdent: String
    ) : Set<MappeDTO> = setOf()

    @PutMapping(PleiepengerSyktBarnRoutes.Urls.OppdaterSøknad, consumes = ["application/json"], produces = ["application/json"])
    @Operation(summary = "Oppdatere en søknad på en eksisterende mappe")
    fun OppdatereSøknad(
            @PathVariable("mappe_id") mappeId: String,
            @RequestBody søknad: Søknad
    ) : MappeDTO? = null

    @PostMapping(PleiepengerSyktBarnRoutes.Urls.NySøknad, consumes = ["application/json"], produces = ["application/json"])
    @Operation(summary = "Ny søknad")
    fun NySøknad(
            @RequestBody søknad: Søknad
    ) : MappeDTO? = null
}
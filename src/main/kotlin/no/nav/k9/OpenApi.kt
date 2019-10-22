package no.nav.k9

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
internal class OpenApi {
    @Bean
    internal fun openApi(
            @Value("\${nav.navn}") navn: String,
            @Value("\${nav.beskrivelse}") beskrivelse: String,
            @Value("\${nav.versjon}") versjon: String
    ): OpenAPI = OpenAPI()
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

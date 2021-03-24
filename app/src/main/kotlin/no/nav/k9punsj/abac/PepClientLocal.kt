package no.nav.k9punsj.abac

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("local")
class PepClientLocal : IPepClient {
    override suspend fun harBasisTilgang(fnr: String): Boolean {
        return true
    }
}

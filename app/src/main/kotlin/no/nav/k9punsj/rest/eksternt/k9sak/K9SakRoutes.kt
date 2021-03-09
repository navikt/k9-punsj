package no.nav.k9punsj.rest.eksternt.k9sak

import no.nav.k9punsj.AuthenticationHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration


@Configuration
internal class K9SakRoutes(
    private val authenticationHandler: AuthenticationHandler,
    private val k9SakService: K9SakService
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(K9SakRoutes::class.java)
    }

    internal object Urls {
        internal const val HentSisteVersjonAvPleiepengerSøknad = "/behandling/hentSøknad"
    }

    data class K9SakSøknadDto(
        val navn: String?
    )
}

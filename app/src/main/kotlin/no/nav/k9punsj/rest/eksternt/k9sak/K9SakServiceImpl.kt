package no.nav.k9punsj.rest.eksternt.k9sak

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9punsj.db.datamodell.NorskIdent
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadDto
import no.nav.k9punsj.rest.web.dto.SaksnummerDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.ReactiveHealthIndicator
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import reactor.core.publisher.Mono
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

@Configuration
@Profile("!test")
class K9SakServiceImpl(
        @Value("\${no.nav.pdl.base_url}") baseUrl: URI,
        @Qualifier("sts") private val accessTokenClient: AccessTokenClient
) : ReactiveHealthIndicator, K9SakService {

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)


    override fun health(): Mono<Health> {
        TODO("Not yet implemented")
    }

    override suspend fun hentSisteMottattePsbSøknad(norskIdent: NorskIdent, periode: String): PleiepengerSøknadDto? {
        val json = lesFraFil()
        return objectMapper().readValue<PleiepengerSøknadDto>(json)
    }

    override suspend fun opprettEllerHentFagsakNummer(): SaksnummerDto {
        TODO("Not yet implemented")
    }


    private fun lesFraFil(): String{
        try {
            return Files.readString(Path.of("src/main/resources/skal_bort/komplett-søknad.json"))
        } catch (e: IOException) {
            throw IllegalStateException(e)
        }
    }
}

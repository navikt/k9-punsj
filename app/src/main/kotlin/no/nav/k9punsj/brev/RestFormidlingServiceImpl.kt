package no.nav.k9punsj.brev

import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.ReactiveHealthIndicator
import reactor.core.publisher.Mono
import java.net.URI

class RestFormidlingServiceImpl(
    @Value("\${no.nav.k9sak.base_url}") private val baseUrl: URI,
    @Qualifier("sts") private val accessTokenClient: AccessTokenClient,
    ) : ReactiveHealthIndicator, RestFormidlingService {

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)
    val log = LoggerFactory.getLogger("RestFormidlingService")!!


    override fun health(): Mono<Health> {
        TODO("Not yet implemented")
    }
}

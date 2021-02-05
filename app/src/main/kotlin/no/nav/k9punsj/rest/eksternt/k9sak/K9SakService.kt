package no.nav.k9punsj.rest.eksternt.k9sak

import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.ReactiveHealthIndicator
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.net.URI

@Service
class K9SakService(
        @Value("\${no.nav.pdl.base_url}") baseUrl: URI,
        @Qualifier("sts") private val accessTokenClient: AccessTokenClient
) : ReactiveHealthIndicator {




    override fun health(): Mono<Health> {
        TODO("Not yet implemented")
    }
}

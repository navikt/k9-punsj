package no.nav.k9punsj

import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.util.*

@Component
class LoggFilter :WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val callId = exchange.request.headers["x_callId"]?.firstOrNull() ?: UUID.randomUUID().toString()
        exchange.response.headers.add("punsj_callId", callId)
        return chain.filter(exchange)
    }
}
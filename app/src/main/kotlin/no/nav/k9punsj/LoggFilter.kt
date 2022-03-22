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
        val callIdKey = exchange.request.headers["x_callId"]?.firstOrNull() ?: UUID.randomUUID().toString()
        val response = exchange.response
        response.beforeCommit {
            response.headers.set("punsj_callId", callIdKey)
            return@beforeCommit Mono.empty()
        }
        return chain.filter(exchange)
    }
}
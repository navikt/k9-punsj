package no.nav.k9

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.server.CoRouterFunctionDsl
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.coRouter
import java.util.*
import net.logstash.logback.argument.StructuredArguments.e

private val logger: Logger = LoggerFactory.getLogger(CoroutineRequestContext::class.java)
private const val RequestIdHeader = "X-Request-ID"
private const val RequestId = "request_id"
private const val CorrelationId = "correlation_id"

private class CoroutineRequestContext : AbstractCoroutineContextElement(Key) {
    internal companion object Key : CoroutineContext.Key<CoroutineRequestContext>
    internal val attributter: MutableMap<String, String> = mutableMapOf()
}

private fun CoroutineContext.requestContext() = get(CoroutineRequestContext.Key) ?: throw IllegalStateException("Request Context ikke satt.")
internal fun CoroutineContext.hentAttributt(key: String) : String? = requestContext().attributter.getOrDefault(key, null)
internal fun CoroutineContext.settAttributt(key: String, value: String) = requestContext().attributter.put(key, value)
private fun CoroutineContext.settCorrelationId(correlationId: String) = requestContext().attributter.put(CorrelationId, correlationId)
internal fun CoroutineContext.hentCorrelationId() = hentAttributt(CorrelationId) ?: throw IllegalStateException("$CorrelationId ikke satt")

internal fun Routes(
        routes : CoRouterFunctionDsl.() -> Unit
) = coRouter {
    before { serverRequest ->
        val requestId = serverRequest
                .headers()
                .header(RequestIdHeader)
                .firstOrNull() ?: UUID.randomUUID().toString()
        val correlationId = UUID.randomUUID().toString()
        serverRequest.attributes()[RequestId] = requestId
        serverRequest.attributes()[CorrelationId] = correlationId
        logger.info("-> ${serverRequest.methodName()} ${serverRequest.path()}", e(serverRequest.contextMap()))
        serverRequest
    }
    after { serverRequest, serverResponse ->
        logger.info("<- HTTP ${serverResponse.rawStatusCode()}", e(serverRequest.contextMap()))
        serverResponse
    }
    routes()
}

internal suspend fun <T> RequestContext(
        context: CoroutineContext,
        serverRequest: ServerRequest,
        block: suspend CoroutineScope.() -> T
) : T {
    return withContext(
            context
            .plus(MDCContext(serverRequest.contextMap()))
            .plus(CoroutineRequestContext())) {
        coroutineContext.settCorrelationId(serverRequest.correlationId())
        block()
    }
}

private fun ServerRequest.requestId() = attribute(RequestId).get() as String
private fun ServerRequest.correlationId() = attribute(CorrelationId).get() as String
private fun ServerRequest.contextMap(
        requestId: String = requestId(),
        correlationId: String = correlationId()
) = pathVariables().toMutableMap().apply {
    put(RequestId, requestId)
    put(CorrelationId, correlationId)
}
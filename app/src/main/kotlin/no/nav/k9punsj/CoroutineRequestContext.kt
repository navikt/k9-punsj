package no.nav.k9punsj

import de.huxhorn.sulky.ulid.ULID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import net.logstash.logback.argument.StructuredArguments.e
import no.nav.k9punsj.felles.IkkeFunnet
import no.nav.k9punsj.felles.SøknadFinnsIkke
import no.nav.k9punsj.tilgangskontroll.AuthenticationHandler
import no.nav.security.token.support.core.jwt.JwtToken
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.codec.DecodingException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.CoRouterFunctionDsl
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import org.springframework.web.reactive.function.server.coRouter
import org.springframework.web.reactive.function.server.json
import java.net.URI
import java.util.*
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

private val logger: Logger = LoggerFactory.getLogger(CoroutineRequestContext::class.java)
private const val RequestIdHeader = "X-Request-ID"
private const val RequestIdKey = "request_id"
private const val CorrelationIdKey = "correlation_id"
private const val CallIdKey = "callId"
private const val AuthenticationKey = "authentication"

private class CoroutineRequestContext : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<CoroutineRequestContext>
    val attributter: MutableMap<String, Any> = mutableMapOf()
}

private fun CoroutineContext.requestContext() =
    get(CoroutineRequestContext.Key) ?: throw IllegalStateException("Request Context ikke satt.")

internal fun CoroutineContext.hentAttributt(key: String): Any? = requestContext().attributter.getOrDefault(key, null)
private fun CoroutineContext.settCorrelationId(correlationId: String) =
    requestContext().attributter.put(CorrelationIdKey, correlationId)

private fun CoroutineContext.settCallId(callId: String) =
    requestContext().attributter.put(CallIdKey, callId)

internal fun CoroutineContext.hentCallId(): String =
    hentAttributt(CallIdKey) as? String ?: throw IllegalStateException("$CallIdKey ikke satt")

internal fun CoroutineContext.hentCorrelationId(): CorrelationId =
    hentAttributt(CorrelationIdKey) as? CorrelationId ?: throw IllegalStateException("$CorrelationIdKey ikke satt")

private fun CoroutineContext.settAuthentication(authorizationHeader: String) =
    requestContext().attributter.put(AuthenticationKey, Authentication(authorizationHeader))

internal fun CoroutineContext.hentAuthentication(): Authentication =
    hentAttributt(AuthenticationKey) as? Authentication ?: throw IllegalStateException("$AuthenticationKey ikke satt")

internal typealias CorrelationId = String

internal fun K9SakRoutes(
    authenticationHandler: AuthenticationHandler,
    routes: CoRouterFunctionDsl.() -> Unit
) = Routes(authenticationHandler, routes, setOf("naissts")) { jwtToken ->
    jwtToken.containsClaim("sub", "srvk9sak")
}

internal fun SaksbehandlerRoutes(
    authenticationHandler: AuthenticationHandler,
    routes: CoRouterFunctionDsl.() -> Unit
) = Routes(authenticationHandler, routes, setOf("azurev2")) { true }

internal fun PublicRoutes(
    routes: CoRouterFunctionDsl.() -> Unit
) = Routes(null, routes, null, null)

private fun Routes(
    authenticationHandler: AuthenticationHandler?,
    routes: CoRouterFunctionDsl.() -> Unit,
    issuerNames: Set<String>?,
    isAccepted: ((jwtToken: JwtToken) -> Boolean)?
) = coRouter {
    before { serverRequest ->
        val requestId = serverRequest
            .headers()
            .header(RequestIdHeader)
            .firstOrNull() ?: UUID.randomUUID().toString()
        val callId = serverRequest
            .headers()
            .header(CallIdKey)
            .firstOrNull() ?: UUID.randomUUID().toString()
        val correlationId = callId
        serverRequest.attributes()[RequestIdKey] = requestId
        serverRequest.attributes()[CorrelationIdKey] = correlationId
        serverRequest.attributes()[CallIdKey] = callId
        logger.info("-> ${serverRequest.methodName()} ${serverRequest.path()}", e(serverRequest.contextMap()))
        serverRequest
    }
    filter { serverRequest, requestedOperation ->
        val serverResponse = authenticationHandler?.authenticatedRequest(
            serverRequest = serverRequest,
            requestedOperation = requestedOperation,
            issuerNames = issuerNames!!,
            isAccepted = isAccepted!!
        ) ?: requestedOperation(serverRequest)
        logger.info("<- HTTP ${serverResponse.rawStatusCode()}", e(serverRequest.contextMap()))
        serverResponse
    }
    onError<IkkeFunnet> { _, _ ->
        ServerResponse
            .notFound()
            .buildAndAwait()
    }
    onError<SøknadFinnsIkke> { error, _ ->
        ServerResponse
            .badRequest()
            .json()
            .bodyValueAndAwait("Søknad finns ikke, error: ${error.message}")
    }
    onError<Throwable> { error, serverRequest ->
        val exceptionId = serverRequest.headers().header(CallIdKey).firstOrNull() ?: UUID.randomUUID().toString()
        logger.error("Ukjent feil med id $exceptionId . URI: ${serverRequest.uri()}", error)

        ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).bodyValueAndAwait(
            ExceptionResponse(
                message = error.message ?: "Uhåndtert feil uten detaljer",
                uri = serverRequest.uri(),
                exceptionId = exceptionId
            )
        )
    }
    onError<DecodingException> { error, serverRequest ->
        val exceptionId = ULID().nextValue().toString()
        logger.error("DecodingException med id $exceptionId . URI: ${serverRequest.uri()}", error)

        ServerResponse.badRequest().bodyValueAndAwait(
            ExceptionResponse(
                message = error.message ?: "Ingen detaljer",
                uri = serverRequest.uri(),
                exceptionId = exceptionId
            )
        )
    }
    routes()
}

internal suspend fun <T> RequestContext(
    context: CoroutineContext,
    serverRequest: ServerRequest,
    block: suspend CoroutineScope.() -> T
): T {
    return withContext(
        context
            .plus(MDCContext(serverRequest.contextMap()))
            .plus(CoroutineRequestContext())
    ) {
        serverRequest.headers().header(HttpHeaders.AUTHORIZATION).firstOrNull()?.apply {
            coroutineContext.settAuthentication(this)
        }
        coroutineContext.settCorrelationId(serverRequest.correlationId())
        coroutineContext.settCallId(serverRequest.callId())
        block()
    }
}

private fun ServerRequest.requestId() = attribute(RequestIdKey).get() as String
private fun ServerRequest.correlationId() = attribute(CorrelationIdKey).get() as String
private fun ServerRequest.callId() = attribute(CallIdKey).get() as String
private fun ServerRequest.contextMap() = pathVariables().toMutableMap().apply {
    put(RequestIdKey, requestId())
    put(CorrelationIdKey, correlationId())
    put(CallIdKey, callId())
}

data class Authentication(
    internal val authorizationHeader: String
) {
    private val tokenType: String
    internal val accessToken: String

    init {
        val split = authorizationHeader.split(" ")
        require(split.size == 2) { "Ugyldig authorization header." }
        tokenType = split.first()
        accessToken = split[1]
    }
}

data class ExceptionResponse(
    val message: String,
    val uri: URI,
    val exceptionId: String
)

package no.nav.k9punsj

import de.huxhorn.sulky.ulid.ULID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import net.logstash.logback.argument.StructuredArguments.e
import no.nav.k9punsj.felles.IkkeFunnet
import no.nav.k9punsj.tilgangskontroll.AuthenticationHandler
import no.nav.security.token.support.core.jwt.JwtToken
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.codec.DecodingException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.ErrorResponseException
import org.springframework.web.reactive.function.server.CoRouterFunctionDsl
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import org.springframework.web.reactive.function.server.coRouter
import java.net.URI
import java.util.*
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

private val logger: Logger = LoggerFactory.getLogger(CoroutineRequestContext::class.java)
const val REQUEST_ID_KEY = "request_id"
const val CORRELATION_ID_KEY = "correlation_id"
const val CALL_ID_KEY = "callId"

private class CoroutineRequestContext : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<CoroutineRequestContext>

    val attributter: MutableMap<String, Any> = mutableMapOf()
}

private fun CoroutineContext.requestContext(): CoroutineRequestContext =
    get(CoroutineRequestContext.Key) ?: throw IllegalStateException("Request Context ikke satt.")

internal fun CoroutineContext.hentAttributt(key: String): Any? = requestContext().attributter.getOrDefault(key, null)
private fun CoroutineContext.settCorrelationId(correlationId: String) =
    requestContext().attributter.put(CORRELATION_ID_KEY, correlationId)

private fun CoroutineContext.settCallId(callId: String) =
    requestContext().attributter.put(CALL_ID_KEY, callId)

internal fun CoroutineContext.hentCallId(): String =
    hentAttributt(CALL_ID_KEY) as? String ?: throw IllegalStateException("$CALL_ID_KEY ikke satt")

internal fun CoroutineContext.hentCorrelationId(): String =
    hentAttributt(CORRELATION_ID_KEY) as? String ?: throw IllegalStateException("$CORRELATION_ID_KEY ikke satt")

private fun CoroutineContext.settAuthentication(authorizationHeader: String) =
    requestContext().attributter.put("authentication", Authentication(authorizationHeader))

internal fun CoroutineContext.hentAuthentication(): Authentication =
    hentAttributt("authentication") as? Authentication ?: throw IllegalStateException("Authentication ikke satt")

internal fun SaksbehandlerRoutes(
    authenticationHandler: AuthenticationHandler,
    routes: CoRouterFunctionDsl.() -> Unit,
) = Routes(
    authenticationHandler,
    routes,
    setOf("azurev2")
) { true }

private fun Routes(
    authenticationHandler: AuthenticationHandler?,
    routes: CoRouterFunctionDsl.() -> Unit,
    issuerNames: Set<String>?,
    isAccepted: ((jwtToken: JwtToken) -> Boolean)?,
) = coRouter {
    before { serverRequest ->
        val callId = serverRequest
            .headers()
            .header(CALL_ID_KEY)
            .firstOrNull() ?: UUID.randomUUID().toString()
        serverRequest.attributes()[REQUEST_ID_KEY] =
            serverRequest.headers().header("X-Request-ID").firstOrNull() ?: UUID.randomUUID().toString()
        serverRequest.attributes()[CORRELATION_ID_KEY] = callId
        serverRequest.attributes()[CALL_ID_KEY] = callId
        logger.info("-> HTTP ${serverRequest.method().name()} ${serverRequest.path()}", e(serverRequest.contextMap()))
        serverRequest
    }
    filter { serverRequest, requestedOperation ->
        val serverResponse = authenticationHandler?.authenticatedRequest(
            serverRequest = serverRequest,
            requestedOperation = requestedOperation,
            issuerNames = issuerNames!!,
            isAccepted = isAccepted!!
        ) ?: requestedOperation(serverRequest)
        logger.info(
            "<- HTTP ${serverRequest.method().name()} ${serverRequest.path()} ${
                serverResponse.statusCode().value()
            }", e(serverRequest.contextMap())
        )
        serverResponse
    }
    // Håndtering for problem-details
    onError<ErrorResponseException> { error: Throwable, serverRequest: ServerRequest ->
        if (error is ErrorResponseException) {
            serverResponseAsProblemDetails(error, serverRequest)
        } else {
            throw error
        }
    }
    onError<IkkeFunnet> { _, _ ->
        ServerResponse
            .notFound()
            .buildAndAwait()
    }
    onError<DecodingException> { error, serverRequest ->
        val exceptionId = ULID().nextValue().toString()
        logger.error("DecodingException med id $exceptionId . URI: ${serverRequest.uri()}", error)

        val responseError = findErrorResponseException(error)
        if (responseError != null) {
            serverResponseAsProblemDetails(responseError, serverRequest)
        } else {
            ServerResponse.badRequest().bodyValueAndAwait(
                ExceptionResponse(
                    message = error.message ?: "No details",
                    uri = serverRequest.uri(),
                    exceptionId = exceptionId
                )
            )
        }
    }
    onError<Throwable> { error, serverRequest ->
        val exceptionId = serverRequest.headers().header(CALL_ID_KEY).firstOrNull() ?: UUID.randomUUID().toString()
        logger.error("Ukjent feil med id $exceptionId . URI: ${serverRequest.uri()}", error)

        ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).bodyValueAndAwait(
            ExceptionResponse(
                message = error.message ?: "Uhåndtert feil uten detaljer",
                uri = serverRequest.uri(),
                exceptionId = exceptionId
            )
        )
    }
    routes()
}

fun findErrorResponseException(error: Throwable?): ErrorResponseException? {
    var cause = error
    while (cause != null) {
        if (cause is ErrorResponseException) {
            return cause
        }
        cause = cause.cause
    }
    return null
}

private suspend fun serverResponseAsProblemDetails(
    error: ErrorResponseException,
    serverRequest: ServerRequest,
): ServerResponse {
    val problemDetail = error.body
    problemDetail.instance = serverRequest.uri()
    problemDetail.setProperty("correlationId", serverRequest.correlationId())

    logger.error("{}", problemDetail)
    return ServerResponse
        .status(error.statusCode)
        .bodyValueAndAwait(problemDetail)
}

internal suspend fun <T> RequestContext(
    context: CoroutineContext,
    serverRequest: ServerRequest,
    block: suspend CoroutineScope.() -> T,
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

private fun ServerRequest.requestId() = attribute(REQUEST_ID_KEY).get() as String
private fun ServerRequest.correlationId() = attribute(CORRELATION_ID_KEY).get() as String
private fun ServerRequest.callId() = attribute(CALL_ID_KEY).get() as String
private fun ServerRequest.contextMap() = pathVariables().toMutableMap().apply {
    put(REQUEST_ID_KEY, requestId())
    put(CORRELATION_ID_KEY, correlationId())
    put(CALL_ID_KEY, callId())
}

data class Authentication(
    internal val authorizationHeader: String,
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
    val exceptionId: String,
)

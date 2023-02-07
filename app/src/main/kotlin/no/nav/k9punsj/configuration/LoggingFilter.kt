package no.nav.k9punsj.configuration

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.GenericFilterBean
import java.util.UUID

@Component
class LoggingFilter: GenericFilterBean() {

    private val log = LoggerFactory.getLogger(LoggingFilter::class.java.name)

    override fun doFilter(request: ServletRequest?, response: ServletResponse?, chain: FilterChain?) {
        val servletRequest = request as HttpServletRequest
        putValues(servletRequest::class.java.cast(request))
    }

    private fun putValues(request: HttpServletRequest) {
        try {
            val callId = request.getHeader(CallIdKey) ?: ("K9-punsj-" + UUID.randomUUID().toString())
            MDC.put(CallIdKey, callId)
            MDC.put(RequestIdKey, request.getHeader(RequestIdKey) ?: callId)
            MDC.put(CorrelationIdKey, request.getHeader(CorrelationIdKey) ?: callId)
        } catch (e: Exception) {
            log.warn("Noe gikk galt ved setting av MDC-verdier for request {}, MDC-verdier er inkomplette", request.requestURI, e)
        }
    }

    private companion object {
        const val RequestIdKey = "request_id"
        const val CorrelationIdKey = "correlation_id"
        const val CallIdKey = "callId"
    }
}
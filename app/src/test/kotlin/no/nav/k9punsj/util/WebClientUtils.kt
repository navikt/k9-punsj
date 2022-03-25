package no.nav.k9punsj.util

import no.nav.k9punsj.domenetjenester.dto.NorskIdentDto
import org.junit.jupiter.api.Assertions
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ReactiveHttpOutputMessage
import org.springframework.web.reactive.function.BodyInserter
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange

object WebClientUtils {
    suspend inline fun <reified ResponseType> WebClient.getAndAssert(
        norskIdent: NorskIdentDto? = null,
        authorizationHeader: String,
        assertStatus: HttpStatus,
        vararg pathSegment: String
    ): ResponseType {
        val spec = get()
            .uri { it.pathSegment(*pathSegment).build() }
            .header(HttpHeaders.AUTHORIZATION, authorizationHeader)

        norskIdent?.let { spec.header("X-Nav-NorskIdent", it) }

        val (status, body: ResponseType) = spec.awaitStatusWithBody<ResponseType>()
        Assertions.assertEquals(assertStatus, status)
        return body
    }

    suspend inline fun <reified RequestType> WebClient.postAndAssert(
        authorizationHeader: String,
        assertStatus: HttpStatus,
        requestBody: BodyInserter<RequestType, ReactiveHttpOutputMessage>,
        vararg pathSegment: String
    ): ClientResponse {
        val clientResponse = post()
            .uri { it.pathSegment(*pathSegment).build() }
            .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
            .body(requestBody)
            .awaitExchangeBlocking()
        Assertions.assertEquals(assertStatus, clientResponse.statusCode())
        return clientResponse
    }

    suspend inline fun <reified RequestType, reified ResponsType> WebClient.postAndAssertAwaitWithStatusAndBody(
        authorizationHeader: String,
        assertStatus: HttpStatus,
        requestBody: BodyInserter<RequestType, ReactiveHttpOutputMessage>,
        vararg pathSegment: String
    ): ResponsType {
        val (status: HttpStatus, body: ResponsType) = post()
            .uri { it.pathSegment(*pathSegment).build() }
            .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
            .body(requestBody)
            .awaitStatusWithBody<ResponsType>()
        Assertions.assertEquals(assertStatus, status)
        return body
    }

    suspend inline fun <reified RequestType, reified ResponseType> WebClient.putAndAssert(
        norskIdent: String? = null,
        authorizationHeader: String,
        assertStatus: HttpStatus,
        requestBody: BodyInserter<RequestType, ReactiveHttpOutputMessage>,
        vararg pathSegment: String
    ): ResponseType {
        val spec = put()
            .uri { it.pathSegment(*pathSegment).build() }
            .header(HttpHeaders.AUTHORIZATION, authorizationHeader)

        norskIdent?.let { spec.header("X-Nav-NorskIdent", it) }

        val (status, body) = spec
            .body(requestBody)
            .awaitStatusWithBody<ResponseType>()

        Assertions.assertEquals(assertStatus, status)
        return body
    }


    suspend fun WebClient.RequestHeadersSpec<*>.awaitExchangeBlocking(): ClientResponse {
        return awaitExchange { it }
    }

    suspend inline fun <reified T> WebClient.RequestHeadersSpec<*>.awaitStatusWithBody(): Pair<HttpStatus, T> {
        return awaitExchange { Pair(it.statusCode(), it.awaitBody()) }
    }

    suspend inline fun <reified T> WebClient.RequestHeadersSpec<*>.awaitBodyWithType(): T {
        return awaitExchange { it.awaitBody() }
    }

    suspend fun WebClient.RequestHeadersSpec<*>.awaitStatuscode(): HttpStatus {
        return awaitExchange { it.statusCode() }
    }
}

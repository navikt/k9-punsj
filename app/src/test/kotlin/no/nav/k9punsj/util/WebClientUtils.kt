package no.nav.k9punsj.util

import no.nav.k9punsj.awaitExchangeBlocking
import no.nav.k9punsj.awaitStatusWithBody
import no.nav.k9punsj.domenetjenester.dto.NorskIdentDto
import org.junit.jupiter.api.Assertions
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ReactiveHttpOutputMessage
import org.springframework.web.reactive.function.BodyInserter
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient

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

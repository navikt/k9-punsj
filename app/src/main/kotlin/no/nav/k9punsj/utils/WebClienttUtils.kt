package no.nav.k9punsj.utils

import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.WebClientResponseException

object WebClienttUtils {
    fun Result<ResponseEntity<String>>.håndterFeil() = fold(
        onSuccess = { responseEntity: ResponseEntity<String> -> responseEntity },
        onFailure = { throwable: Throwable ->
            when (throwable) {
                is WebClientResponseException -> ResponseEntity
                    .status(throwable.statusCode)
                    .body(throwable.responseBodyAsString)

                else -> throw throwable
            }
        }
    )
}

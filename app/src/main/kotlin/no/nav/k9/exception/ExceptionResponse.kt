package no.nav.k9.exception

import java.net.URI

data class ExceptionResponse(
    val message: String,
    val uri: URI,
    val exceptionId: String
)

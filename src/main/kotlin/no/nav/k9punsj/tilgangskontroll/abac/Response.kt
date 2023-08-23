package no.nav.k9punsj.tilgangskontroll.abac

import com.fasterxml.jackson.annotation.JsonProperty

data class Response(
    @JsonProperty("Response")
    val response: List<Response>
) {
    data class Response(
        @JsonProperty("Decision")
        val decision: String
    )
}

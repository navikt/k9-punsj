package no.nav.k9punsj.tilgangskontroll.token

import com.fasterxml.jackson.annotation.JsonProperty

data class JWTToken(
    val NAVident : String, // Z994048
    val aud: String, // 5afad323-c9df-4e14-b481-b278e9d2bf69
    val azp: String, // a084abb8-6a38-4506-84c2-e4ac8b438a05
    val exp: Int, // 1586930888
    val groups: List<String>,
    val iat: Int, // 1586926990
    val iss: String, // https://login.microsoftonline.com/966ac572-f5b7-4bbe-aa88-c76419c0f851/v2.0
    val nbf: Int, // 1586926990
    val oid: String, // 5613f256-cf39-4bd4-84b2-07985271e832
    val scp: String, // defaultaccess
    val sub: String, // D-FuwXVYwIHbEjtMQsjfo1-DceBJE9FfUX2cwsfB4JQ
)

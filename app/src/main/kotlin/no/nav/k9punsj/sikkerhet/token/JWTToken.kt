package no.nav.k9punsj.sikkerhet.token


import com.fasterxml.jackson.annotation.JsonProperty

data class JWTToken(
    val aio: String, // ATQAy/8PAAAA6s6UsBsUOzNWRnSavXm4AEzHtQkCCN4tzvZe3DWwIP4lj74AIe3rp8C+jYiN1LxM
    val aud: String, // 5afad323-c9df-4e14-b481-b278e9d2bf69
    val azp: String, // a084abb8-6a38-4506-84c2-e4ac8b438a05
    val azpacr: String, // 2
    val exp: Int, // 1586930888
    val groups: List<String>,
    val iat: Int, // 1586926990
    val iss: String, // https://login.microsoftonline.com/966ac572-f5b7-4bbe-aa88-c76419c0f851/v2.0
    val name: String, // F_Z994048 E_Z994048
    val nbf: Int, // 1586926990
    val oid: String, // 5613f256-cf39-4bd4-84b2-07985271e832
    @JsonProperty("preferred_username")
    val preferredUsername: String, // F_Z994048.E_Z994048@trygdeetaten.no
    val scp: String, // defaultaccess
    val sub: String, // D-FuwXVYwIHbEjtMQsjfo1-DceBJE9FfUX2cwsfB4JQ
    val tid: String, // 966ac572-f5b7-4bbe-aa88-c76419c0f851
    val uti: String, // IpDIBSCX2Uqc5uaHvNlUAA
    val ver: String // 2.0
)

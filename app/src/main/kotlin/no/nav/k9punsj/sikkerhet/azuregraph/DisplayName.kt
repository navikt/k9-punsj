package no.nav.k9punsj.sikkerhet.azuregraph

import com.fasterxml.jackson.annotation.JsonProperty

data class DisplayName(
    @JsonProperty("@odata.context")
    val odataContext: String,
    val displayName: String,
    val PremisesSamAccountName: String
)

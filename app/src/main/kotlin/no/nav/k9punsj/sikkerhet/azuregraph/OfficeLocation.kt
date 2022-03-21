package no.nav.k9punsj.sikkerhet.azuregraph


import com.fasterxml.jackson.annotation.JsonProperty

data class OfficeLocation(
    @JsonProperty("@odata.context")
    val odataContext: String,
    val officeLocation: String
)

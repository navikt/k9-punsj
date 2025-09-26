package no.nav.k9punsj.tilgangskontroll.abac

interface IPepClient {

    suspend fun harLesetilgang(fnr: List<String>, fnrForSporingslogg: List<String>, urlKallet: String): Boolean

    suspend fun harLesetilgang(fnr: String, urlKallet: String): Boolean

    suspend fun harSendeInnTilgang(fnr: String, urlKallet: String): Boolean

    suspend fun harSendeInnTilgang(fnr: List<String>, fnrForSporingslogg: List<String>, urlKallet: String): Boolean

    suspend fun erSaksbehandler(): Boolean
}

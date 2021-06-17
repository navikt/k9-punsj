package no.nav.k9punsj.abac

interface IPepClient {


    suspend fun harBasisTilgang(fnr: List<String>, urlKallet: String): Boolean

    suspend fun harBasisTilgang(fnr: String, urlKallet: String): Boolean

    suspend fun sendeInnTilgang(fnr: String, urlKallet: String): Boolean

    suspend fun sendeInnTilgang(fnr: List<String>, urlKallet: String): Boolean

    suspend fun erSaksbehandler(): Boolean
}

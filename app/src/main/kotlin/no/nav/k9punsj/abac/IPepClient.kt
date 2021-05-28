package no.nav.k9punsj.abac

interface IPepClient {

    suspend fun harBasisTilgang(fnr: String): Boolean

    suspend fun harBasisTilgang(fnr: List<String>): Boolean

    suspend fun  sendeInnTilgang(fnr: String): Boolean
}

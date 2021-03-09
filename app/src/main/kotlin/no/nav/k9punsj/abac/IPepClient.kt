package no.nav.k9punsj.abac

interface IPepClient {

    suspend fun harBasisTilgang(fnr: String): Boolean
}

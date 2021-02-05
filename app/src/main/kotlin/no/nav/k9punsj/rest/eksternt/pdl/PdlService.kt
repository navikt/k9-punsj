package no.nav.k9punsj.rest.eksternt.pdl

interface PdlService {

    suspend fun identifikator(fnummer: String): PdlResponse?

    suspend fun identifikatorMedAktørId(aktørId: String): PdlResponse?

}

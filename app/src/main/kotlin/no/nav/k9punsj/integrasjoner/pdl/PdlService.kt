package no.nav.k9punsj.integrasjoner.pdl

interface PdlService {

    suspend fun identifikator(fnummer: String): PdlResponse?

    suspend fun identifikatorMedAktørId(aktørId: String): PdlResponse?

    suspend fun aktørIdFor(fnummer: String): String?

    suspend fun hentBarn(identitetsnummer: String): Set<String>

    suspend fun hentPersonopplysninger(identitetsnummer: Set<String>): Set<Personopplysninger>
}

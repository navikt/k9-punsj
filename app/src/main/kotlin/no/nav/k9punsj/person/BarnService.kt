package no.nav.k9punsj.person

internal interface BarnService {
    suspend fun hentBarn(identitetsnummer: String) : Set<Barn>
}
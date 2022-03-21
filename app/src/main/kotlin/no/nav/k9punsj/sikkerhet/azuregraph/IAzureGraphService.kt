package no.nav.k9punsj.sikkerhet.azuregraph

interface IAzureGraphService {
    suspend fun hentIdentTilInnloggetBruker(): String

    suspend fun hentEnhetForInnloggetBruker(): String
}

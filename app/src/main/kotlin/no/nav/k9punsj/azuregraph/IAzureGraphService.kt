package no.nav.k9punsj.azuregraph

interface IAzureGraphService {
    suspend fun hentIdentTilInnloggetBruker(): String

    suspend fun hentEnhetForInnloggetBruker(): String
}

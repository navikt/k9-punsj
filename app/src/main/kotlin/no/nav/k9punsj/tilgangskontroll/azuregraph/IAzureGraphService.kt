package no.nav.k9punsj.tilgangskontroll.azuregraph

interface IAzureGraphService {
    suspend fun hentIdentTilInnloggetBruker(): String

    suspend fun hentEnhetForInnloggetBruker(): String
}

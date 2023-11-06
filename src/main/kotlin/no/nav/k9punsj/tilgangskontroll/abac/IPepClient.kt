package no.nav.k9punsj.tilgangskontroll.abac

interface IPepClient {

    suspend fun harInnloggetBrukerTilgangTilOgLeseSakForFnr(fnr: List<String>, urlKallet: String): Boolean

    suspend fun harInnloggetBrukerTilgangTilOgSkriveSakForFnr(fnr: String, urlKallet: String): Boolean

    suspend fun harInnloggetBrukerTilgangTilOgSkriveSakForFnr(fnr: List<String>, urlKallet: String): Boolean

    suspend fun erSaksbehandler(): Boolean
}

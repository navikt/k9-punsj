package no.nav.k9punsj.abac

import no.nav.k9punsj.LokalProfil
import no.nav.k9punsj.TestProfil
import no.nav.k9punsj.tilgangskontroll.abac.IPepClient
import org.springframework.stereotype.Component

@Component
@TestProfil
@LokalProfil
internal class AlltidTilgangPepClient : IPepClient {
    override suspend fun harInnloggetBrukerTilgangTilOgLeseSakForFnr(fnr: List<String>, urlKallet: String) = true
    override suspend fun harInnloggetBrukerTilgangTilOgSkriveSakForFnr(fnr: String, urlKallet: String) = true
    override suspend fun harInnloggetBrukerTilgangTilOgSkriveSakForFnr(fnr: List<String>, urlKallet: String) = true
    override suspend fun erSaksbehandler() = true
}

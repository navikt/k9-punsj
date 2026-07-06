package no.nav.k9punsj.tilgangskontroll.abac

import no.nav.k9.sak.typer.Saksnummer
import no.nav.sif.abac.kontrakt.abac.resultat.Tilgangsbeslutning
import no.nav.sif.abac.kontrakt.abac.resultat.TilgangsbeslutningOgHistoriskSak
import no.nav.sif.abac.kontrakt.person.AktørId

interface IPepClient {

    suspend fun harLesetilgang(fnr: List<String>, fnrForSporingslogg: List<String>, urlKallet: String): Boolean

    suspend fun harLesetilgang(fnr: String, urlKallet: String): Boolean

    suspend fun harLesetilgangTilSaksnummer(fnr: Saksnummer, urlKallet: String): Tilgangsbeslutning

    suspend fun harLesetilgangTilSaksnummerUtenAuditlogg(saksnummer: Saksnummer): Tilgangsbeslutning

    suspend fun sjekkTilgangTilBrukersSakerOgGiInformasjonOmHistoriskSak(brukerAktørId : AktørId, urlKallet: String): TilgangsbeslutningOgHistoriskSak

    suspend fun harSendeInnTilgang(fnr: String, urlKallet: String): Boolean

    suspend fun harSendeInnTilgang(fnr: List<String>, fnrForSporingslogg: List<String>, urlKallet: String): Boolean

    suspend fun erSaksbehandler(): Boolean
}

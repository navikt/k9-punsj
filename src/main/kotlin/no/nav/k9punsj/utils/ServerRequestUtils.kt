package no.nav.k9punsj.utils

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9punsj.felles.dto.MatchFagsakMedPeriode
import no.nav.k9punsj.felles.dto.Matchfagsak
import no.nav.k9punsj.felles.dto.OpprettNySøknad
import no.nav.k9punsj.felles.dto.SendSøknad
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest

object ServerRequestUtils {

    internal fun ServerRequest.hentNorskIdentHeader(): String =
        headers().header("X-Nav-NorskIdent").first()!!

    internal suspend fun ServerRequest.mapNySøknad(): OpprettNySøknad =
        body(BodyExtractors.toMono(OpprettNySøknad::class.java)).awaitFirst()

    internal suspend fun ServerRequest.mapSendSøknad(): SendSøknad =
        body(BodyExtractors.toMono(SendSøknad::class.java)).awaitFirst()

    internal suspend fun ServerRequest.mapMatchFagsak(): Matchfagsak =
        body(BodyExtractors.toMono(Matchfagsak::class.java)).awaitFirst()

    internal suspend fun ServerRequest.mapMatchFagsakMedPerioder(): MatchFagsakMedPeriode =
        body(BodyExtractors.toMono(MatchFagsakMedPeriode::class.java)).awaitFirst()

    internal fun ServerRequest.søknadLocationUri(søknadId: String) =
        uriBuilder().pathSegment("mappe", søknadId).build()
}

package no.nav.k9punsj.utils

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9punsj.domenetjenester.dto.SøknadIdDto
import no.nav.k9punsj.rest.web.MatchFagsakMedPeriode
import no.nav.k9punsj.rest.web.Matchfagsak
import no.nav.k9punsj.rest.web.OpprettNySøknad
import no.nav.k9punsj.rest.web.SendSøknad
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest

object ServerRequestUtils {

    internal fun ServerRequest.norskIdent(): String {
        return headers().header("X-Nav-NorskIdent").first()!!
    }

    internal suspend fun ServerRequest.opprettNy() =
        body(BodyExtractors.toMono(OpprettNySøknad::class.java)).awaitFirst()

    internal suspend fun ServerRequest.sendSøknad() = body(BodyExtractors.toMono(SendSøknad::class.java)).awaitFirst()
    internal suspend fun ServerRequest.matchFagsak() = body(BodyExtractors.toMono(Matchfagsak::class.java)).awaitFirst()
    internal suspend fun ServerRequest.matchFagsakMedPerioder() = body(BodyExtractors.toMono(MatchFagsakMedPeriode::class.java)).awaitFirst()


    internal fun ServerRequest.søknadLocation(søknadId: SøknadIdDto) =
        uriBuilder().pathSegment("mappe", søknadId).build()

}

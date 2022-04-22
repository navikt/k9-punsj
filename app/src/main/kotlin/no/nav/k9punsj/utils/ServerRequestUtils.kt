package no.nav.k9punsj.utils

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9punsj.domenetjenester.dto.*
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest

object ServerRequestUtils {

    internal fun ServerRequest.hentNorskIdentHeader(): String {
        return headers().header("X-Nav-NorskIdent").first()!!
    }

    internal suspend fun ServerRequest.mapNySøknad() =
        body(BodyExtractors.toMono(OpprettNySøknad::class.java)).awaitFirst()

    internal suspend fun ServerRequest.mapSendSøknad(): SendSøknad = body(BodyExtractors.toMono(SendSøknad::class.java)).awaitFirst()
    internal suspend fun ServerRequest.mapMatchFagsak() = body(BodyExtractors.toMono(Matchfagsak::class.java)).awaitFirst()
    internal suspend fun ServerRequest.mapMatchFagsakMedPerioder() = body(BodyExtractors.toMono(MatchFagsakMedPeriode::class.java)).awaitFirst()


    internal fun ServerRequest.søknadLocationUri(søknadId: String) =
        uriBuilder().pathSegment("mappe", søknadId).build()

}

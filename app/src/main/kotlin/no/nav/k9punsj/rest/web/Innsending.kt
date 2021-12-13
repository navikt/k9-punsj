package no.nav.k9punsj.rest.web

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9punsj.brev.DokumentbestillingDto
import no.nav.k9punsj.db.datamodell.NorskIdent
import no.nav.k9punsj.rest.web.dto.*
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest

typealias JournalpostId = String

typealias SøknadJson = MutableMap<String, Any?>

data class OpprettNySøknad(
    val norskIdent: NorskIdentDto,
    val journalpostId: JournalpostIdDto,
    val barnIdent: NorskIdentDto?,
)

data class HentSøknad(
    val norskIdent: NorskIdent,
)

data class IdentOgJournalpost(
    val norskIdent: NorskIdentDto,
    val journalpostId: JournalpostIdDto,
)

data class SendSøknad(
    val norskIdent: NorskIdentDto,
    val soeknadId: SøknadIdDto,
)

data class Matchfagsak(
    val brukerIdent: NorskIdentDto,
    val barnIdent: NorskIdentDto,
)

data class MatchFagsakMedPeriode(
    val brukerIdent: NorskIdentDto,
    val periodeDto: PeriodeDto
)

data class PunsjBolleDto(
    val brukerIdent: NorskIdentDto,
    val barnIdent: NorskIdentDto,
    val journalpostId: JournalpostIdDto,
)

data class SettPåVentDto(
    val soeknadId: SøknadIdDto
)


data class OpprettNyOmsSøknad(
    val norskIdent: NorskIdentDto,
    val journalpostId: JournalpostIdDto,
)

internal fun Boolean.httpStatus() = if (this) HttpStatus.OK else HttpStatus.BAD_REQUEST


internal fun ServerRequest.norskIdent(): String {
    return headers().header("X-Nav-NorskIdent").first()!!
}

internal fun ServerRequest.barnIdent(): String? {
    return headers().header("X-Nav-AktorId-barn").firstOrNull()
}

internal suspend fun ServerRequest.pleiepengerSøknad() =
    body(BodyExtractors.toMono(PleiepengerSøknadDto::class.java)).awaitFirst()

internal suspend fun ServerRequest.omsorgspengerSøknad() =
    body(BodyExtractors.toMono(OmsorgspengerSøknadDto::class.java)).awaitFirst()

internal suspend fun ServerRequest.brevBestilling() =
    body(BodyExtractors.toMono(DokumentbestillingDto::class.java)).awaitFirst()

internal suspend fun ServerRequest.opprettNy() =
    body(BodyExtractors.toMono(OpprettNySøknad::class.java)).awaitFirst()

internal suspend fun ServerRequest.identOgJournalpost() =
    body(BodyExtractors.toMono(IdentOgJournalpost::class.java)).awaitFirst()

internal suspend fun ServerRequest.sendSøknad() = body(BodyExtractors.toMono(SendSøknad::class.java)).awaitFirst()
internal suspend fun ServerRequest.matchFagsak() = body(BodyExtractors.toMono(Matchfagsak::class.java)).awaitFirst()
internal suspend fun ServerRequest.matchFagsakMedPerioder() = body(BodyExtractors.toMono(MatchFagsakMedPeriode::class.java)).awaitFirst()


internal fun ServerRequest.søknadLocation(søknadId: SøknadIdDto) =
    uriBuilder().pathSegment("mappe", søknadId).build()


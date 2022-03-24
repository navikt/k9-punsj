package no.nav.k9punsj.rest.web

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9punsj.brev.DokumentbestillingDto
import no.nav.k9punsj.db.datamodell.NorskIdent
import no.nav.k9punsj.domenetjenester.dto.AktørIdDto
import no.nav.k9punsj.domenetjenester.dto.JournalpostIdDto
import no.nav.k9punsj.domenetjenester.dto.NorskIdentDto
import no.nav.k9punsj.domenetjenester.dto.OmsorgspengerAleneOmsorgSøknadDto
import no.nav.k9punsj.omsorgspengerkronisksyktbarn.OmsorgspengerKroniskSyktBarnSøknadDto
import no.nav.k9punsj.omsorgspengermidlertidigalene.OmsorgspengerMidlertidigAleneSøknadDto
import no.nav.k9punsj.domenetjenester.dto.OmsorgspengerSøknadDto
import no.nav.k9punsj.domenetjenester.dto.PeriodeDto
import no.nav.k9punsj.pleiepengerlivetssluttfase.PleiepengerLivetsSluttfaseSøknadDto
import no.nav.k9punsj.pleiepengersyktbarn.PleiepengerSyktBarnSøknadDto
import no.nav.k9punsj.domenetjenester.dto.SøknadIdDto
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest

typealias JournalpostId = String

typealias SøknadJson = MutableMap<String, Any?>

data class OpprettNySøknad(
    val norskIdent: NorskIdentDto,
    val journalpostId: JournalpostIdDto,
    val pleietrengendeIdent: NorskIdentDto?,
    val annenPart: NorskIdentDto?,
    //TODO endre til å bare bruke pleietrengendeIdent, men støtter både barnIdent og pleietrengendeIdent
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

data class SøkUferdigJournalposter(
    val aktorIdentDto: AktørIdDto,
    val aktorIdentBarnDto: AktørIdDto?
)

data class PunsjBolleDto(
    val brukerIdent: NorskIdentDto,
    //todo bytt navn til pleietrengende
    val barnIdent: NorskIdentDto?,
    val annenPart: NorskIdentDto?,
    val journalpostId: JournalpostIdDto,
    val fagsakYtelseType: FagsakYtelseType
)

data class SettPåVentDto(
    val soeknadId: SøknadIdDto?
)


data class OpprettNyOmsSøknad(
    val norskIdent: NorskIdentDto,
    val journalpostId: JournalpostIdDto,
)

internal fun Boolean.httpStatus() = if (this) HttpStatus.OK else HttpStatus.BAD_REQUEST


internal fun ServerRequest.norskIdent(): String {
    return headers().header("X-Nav-NorskIdent").first()!!
}

internal suspend fun ServerRequest.pleiepengerSøknad() =
    body(BodyExtractors.toMono(PleiepengerSyktBarnSøknadDto::class.java)).awaitFirst()

internal suspend fun ServerRequest.pleiepengerPlsSøknad() =
    body(BodyExtractors.toMono(PleiepengerLivetsSluttfaseSøknadDto::class.java)).awaitFirst()

internal suspend fun ServerRequest.omsorgspengerSøknad() =
    body(BodyExtractors.toMono(OmsorgspengerSøknadDto::class.java)).awaitFirst()

internal suspend fun ServerRequest.omsorgspengerKroniskSyktBarnSøknad() =
    body(BodyExtractors.toMono(OmsorgspengerKroniskSyktBarnSøknadDto::class.java)).awaitFirst()

internal suspend fun ServerRequest.omsorgspengerMidlertidigAleneSøknad() =
    body(BodyExtractors.toMono(OmsorgspengerMidlertidigAleneSøknadDto::class.java)).awaitFirst()

internal suspend fun ServerRequest.omsorgspengerAleneOmsorgSøknad() =
    body(BodyExtractors.toMono(OmsorgspengerAleneOmsorgSøknadDto::class.java)).awaitFirst()

internal suspend fun ServerRequest.brevBestilling() =
    body(BodyExtractors.toMono(DokumentbestillingDto::class.java)).awaitFirst()

internal suspend fun ServerRequest.opprettNy() =
    body(BodyExtractors.toMono(OpprettNySøknad::class.java)).awaitFirst()

internal suspend fun ServerRequest.identOgJournalpost() =
    body(BodyExtractors.toMono(IdentOgJournalpost::class.java)).awaitFirst()

internal suspend fun ServerRequest.søkUferdigJournalposter() =
    body(BodyExtractors.toMono(SøkUferdigJournalposter::class.java)).awaitFirst()

internal suspend fun ServerRequest.sendSøknad() = body(BodyExtractors.toMono(SendSøknad::class.java)).awaitFirst()
internal suspend fun ServerRequest.matchFagsak() = body(BodyExtractors.toMono(Matchfagsak::class.java)).awaitFirst()
internal suspend fun ServerRequest.matchFagsakMedPerioder() = body(BodyExtractors.toMono(MatchFagsakMedPeriode::class.java)).awaitFirst()


internal fun ServerRequest.søknadLocation(søknadId: SøknadIdDto) =
    uriBuilder().pathSegment("mappe", søknadId).build()


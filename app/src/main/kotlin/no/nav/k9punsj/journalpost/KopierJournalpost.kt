package no.nav.k9punsj.journalpost

import de.huxhorn.sulky.ulid.ULID
import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.abac.IPepClient
import no.nav.k9punsj.hentCorrelationId
import no.nav.k9punsj.innsending.InnsendingClient
import no.nav.k9punsj.innsending.KopierJournalpostInfo
import no.nav.k9punsj.journalpost.KopierJournalpost.ikkeTilgang
import no.nav.k9punsj.journalpost.KopierJournalpost.journalpostId
import no.nav.k9punsj.journalpost.KopierJournalpost.kanIkkeKopieres
import no.nav.k9punsj.journalpost.KopierJournalpost.kopierJournalpostDto
import no.nav.k9punsj.journalpost.KopierJournalpost.sendtTilKopiering
import no.nav.k9punsj.rest.eksternt.punsjbollen.PunsjbolleService
import no.nav.k9punsj.rest.web.JournalpostId
import no.nav.k9punsj.rest.web.dto.NorskIdentDto
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.CoRouterFunctionDsl
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import kotlin.coroutines.coroutineContext

data class KopierJournalpostDto(
    val dedupKey: ULID.Value,
    val fra: NorskIdentDto,
    val til: NorskIdentDto,
    val barn: NorskIdentDto
)

internal fun CoRouterFunctionDsl.kopierJournalpostRoute(
    pepClient: IPepClient,
    punsjbolleService: PunsjbolleService,
    safGateway: SafGateway,
    innsendingClient: InnsendingClient) {

    suspend fun harTilgang(dto: KopierJournalpostDto): Boolean {
        pepClient.sendeInnTilgang(dto.fra, JournalpostRoutes.Urls.KopierJournalpost).also { tilgang -> if (!tilgang) { return false }}
        pepClient.sendeInnTilgang(dto.til, JournalpostRoutes.Urls.KopierJournalpost).also { tilgang -> if (!tilgang) { return false }}
        return pepClient.sendeInnTilgang(dto.barn, JournalpostRoutes.Urls.KopierJournalpost)
    }

    suspend fun kanRutesTilK9(person: NorskIdentDto, barn: NorskIdentDto, journalpostId: JournalpostId) = punsjbolleService.opprettEllerHentFagsaksnummer(
        søker = person,
        journalpostIdDto = journalpostId,
        barn = barn
    ) != null

    fun erInngåendeJournalpost(journalpost: SafDtos.Journalpost) = journalpost.journalposttype == "I"

    POST("/api${JournalpostRoutes.Urls.KopierJournalpost}") { request ->
        RequestContext(coroutineContext, request) {
            val journalpostId = request.journalpostId()
            val journalpost = safGateway.hentJournalpostInfo(journalpostId)?:return@RequestContext kanIkkeKopieres("Finner ikke journalpost.")
            if (!erInngåendeJournalpost(journalpost)) { return@RequestContext kanIkkeKopieres("Kan kun kopiere inngående journalposter.") }
            val dto = request.kopierJournalpostDto()
            if (!harTilgang(dto)) { return@RequestContext ikkeTilgang()}
            if (!kanRutesTilK9(dto.fra, dto.barn, journalpostId)) { return@RequestContext kanIkkeKopieres("Kan ikke rutes til K9 grunnet fra-person.")}
            if (!kanRutesTilK9(dto.til, dto.barn, journalpostId)) { return@RequestContext kanIkkeKopieres("Kan ikke rutes til K9 grunnet til-person.")}
            innsendingClient.sendKopierJournalpost(KopierJournalpostInfo(
                id = dto.dedupKey,
                journalpostId = journalpostId,
                fra = dto.fra,
                til = dto.til,
                pleietrengende = dto.barn,
                correlationId = coroutineContext.hentCorrelationId(),
                ytelse = FagsakYtelseType.PLEIEPENGER_SYKT_BARN
            ))
            return@RequestContext sendtTilKopiering()
        }
    }
}

internal object KopierJournalpost {
    internal val logger = LoggerFactory.getLogger(KopierJournalpost::class.java)

    internal suspend fun ikkeTilgang() = ServerResponse
        .status(HttpStatus.FORBIDDEN)
        .bodyValueAndAwait("Har ikke lov til å kopiere journalpost.")

    internal suspend fun kanIkkeKopieres(feil: String) = ServerResponse
        .status(HttpStatus.CONFLICT)
        .bodyValueAndAwait(feil)
        .also { logger.warn("Journalpost kan ikke kopieres: $feil") }

    internal suspend fun sendtTilKopiering() = ServerResponse
        .status(HttpStatus.ACCEPTED)
        .bodyValueAndAwait("Journalposten vil bli kopiert.")

    internal suspend fun ServerRequest.kopierJournalpostDto() =
        body(BodyExtractors.toMono(KopierJournalpostDto::class.java)).awaitFirst()

    internal fun ServerRequest.journalpostId(): JournalpostId = pathVariable("journalpost_id")

}


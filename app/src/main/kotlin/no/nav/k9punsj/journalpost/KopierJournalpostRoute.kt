package no.nav.k9punsj.journalpost

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.felles.PunsjbolleRuting
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.fordel.PunsjInnsendingType
import no.nav.k9punsj.innsending.InnsendingClient
import no.nav.k9punsj.integrasjoner.punsjbollen.PunsjbolleService
import no.nav.k9punsj.journalpost.KopierJournalpost.ikkeTilgang
import no.nav.k9punsj.journalpost.KopierJournalpost.kanIkkeKopieres
import no.nav.k9punsj.journalpost.KopierJournalpost.kopierJournalpostDto
import no.nav.k9punsj.journalpost.KopierJournalpost.sendtTilKopiering
import no.nav.k9punsj.tilgangskontroll.abac.IPepClient
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.CoRouterFunctionDsl
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import kotlin.coroutines.coroutineContext

data class KopierJournalpostDto(
    val fra: String,
    val til: String,
    // TODO bytt navn til pleietrengende
    val barn: String?,
    val annenPart: String?
) {
    init {
        require(barn != null || annenPart != null) {
            "Må sette minst en av barn og annenPart"
        }
    }
}

internal fun CoRouterFunctionDsl.kopierJournalpostRoute(
    pepClient: IPepClient,
    punsjbolleService: PunsjbolleService,
    journalpostService: JournalpostService,
    innsendingClient: InnsendingClient
) {
    suspend fun harTilgang(dto: KopierJournalpostDto): Boolean {
        val identListe = mutableListOf(dto.fra, dto.til)
        dto.barn?.let { identListe.add(it) }
        dto.annenPart?.let { identListe.add(it) }
        return pepClient.sendeInnTilgang(identListe, JournalpostRoutes.Urls.KopierJournalpost)
    }

    suspend fun fraKanRutesTilK9(
        dto: KopierJournalpostDto,
        journalpost: JournalpostInfo,
        fagsakYtelseType: FagsakYtelseType
    ) = punsjbolleService.ruting(
        søker = dto.fra,
        pleietrengende = dto.barn,
        annenPart = dto.annenPart,
        journalpostId = journalpost.journalpostId,
        periode = journalpost.mottattDato.toLocalDate().let {
            PeriodeDto(
                it,
                it
            )
        },
        fagsakYtelseType = fagsakYtelseType
    ) == PunsjbolleRuting.K9Sak

    suspend fun tilKanRutesTilK9(
        dto: KopierJournalpostDto,
        journalpost: JournalpostInfo,
        fagsakYtelseType: FagsakYtelseType
    ) = punsjbolleService.ruting(
        søker = dto.til,
        pleietrengende = dto.barn,
        annenPart = dto.annenPart,
        journalpostId = null, // For den det skal kopieres til sender vi ikke med referanse til journalposten som tilhører 'fra'-personen
        periode = journalpost.mottattDato.toLocalDate().let {
            PeriodeDto(
                it,
                it
            )
        },
        fagsakYtelseType = fagsakYtelseType
    ) == PunsjbolleRuting.K9Sak

    POST("/api${JournalpostRoutes.Urls.KopierJournalpost}") { request ->
        RequestContext(coroutineContext, request) {
            val journalpostId = request.pathVariable("journalpost_id")
            val journalpostInfo = journalpostService.hentJournalpostInfo(journalpostId)
                ?: return@RequestContext kanIkkeKopieres("Finner ikke journalpost.")
            val dto = request.kopierJournalpostDto()
            val journalpost = journalpostService.hentHvisJournalpostMedId(journalpostId)

            if (!harTilgang(dto)) {
                return@RequestContext ikkeTilgang()
            }

            val safJournalpost = journalpostService.hentSafJournalPost(journalpostId)
            if (safJournalpost != null && safJournalpost.journalposttype == "U") {
                return@RequestContext kanIkkeKopieres("Ikke støttet journalposttype: ${safJournalpost.journalposttype}")
            }

            val ytelseType = journalpost?.ytelse?.let {
                journalpost.utledeFagsakYtelseType(fagsakYtelseType = FagsakYtelseType.fraKode(it))
            } ?: return@RequestContext kanIkkeKopieres("Finner ikke ytelse for journalpost.")

            // Om det kopieres til samme person gjør vi kun rutingsjekk uten journalpostId
            if (dto.fra == dto.til) {
                if (!tilKanRutesTilK9(dto, journalpostInfo, ytelseType)) {
                    return@RequestContext kanIkkeKopieres("Kan ikke rutes til K9.")
                }
            } else {
                if (!fraKanRutesTilK9(dto, journalpostInfo, ytelseType)) {
                    return@RequestContext kanIkkeKopieres("Kan ikke rutes til K9 grunnet fra-person.")
                }
                if (!tilKanRutesTilK9(dto, journalpostInfo, ytelseType)) {
                    return@RequestContext kanIkkeKopieres("Kan ikke rutes til K9 grunnet til-person.")
                }
            }

            if (journalpost?.type != null && journalpost.type == PunsjInnsendingType.INNTEKTSMELDING_UTGÅTT.kode) {
                return@RequestContext kanIkkeKopieres("Kan ikke kopier journalpost med type inntektsmelding utgått.")
            }

            if (ytelseType != FagsakYtelseType.PLEIEPENGER_SYKT_BARN && ytelseType != FagsakYtelseType.OMSORGSPENGER_KS) {
                return@RequestContext kanIkkeKopieres("Støtter ikke kopiering av ${ytelseType.navn} for relaterte journalposter")
            }

            innsendingClient.sendKopierJournalpost(
                KopierJournalpostInfo(
                    journalpostId = journalpostId,
                    fra = dto.fra,
                    til = dto.til,
                    pleietrengende = dto.barn,
                    ytelse = ytelseType
                )
            )
            return@RequestContext sendtTilKopiering()
        }
    }
}

private object KopierJournalpost {
    val logger = LoggerFactory.getLogger(KopierJournalpost::class.java)

    suspend fun ikkeTilgang() = ServerResponse
        .status(HttpStatus.FORBIDDEN)
        .bodyValueAndAwait("Har ikke lov til å kopiere journalpost.")

    suspend fun kanIkkeKopieres(feil: String) = ServerResponse
        .status(HttpStatus.CONFLICT)
        .bodyValueAndAwait(feil)
        .also { logger.warn("Journalpost kan ikke kopieres: $feil") }

    suspend fun sendtTilKopiering() = ServerResponse
        .status(HttpStatus.ACCEPTED)
        .bodyValueAndAwait("Journalposten vil bli kopiert.")

    suspend fun ServerRequest.kopierJournalpostDto() =
        body(BodyExtractors.toMono(KopierJournalpostDto::class.java)).awaitFirst()
}

package no.nav.k9punsj.journalpost

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.fordel.PunsjInnsendingType
import no.nav.k9punsj.innsending.InnsendingClient
import no.nav.k9punsj.integrasjoner.pdl.PdlService
import no.nav.k9punsj.journalpost.KopierJournalpost.ikkeTilgang
import no.nav.k9punsj.journalpost.KopierJournalpost.kanIkkeKopieres
import no.nav.k9punsj.journalpost.KopierJournalpost.kopierJournalpostDto
import no.nav.k9punsj.journalpost.KopierJournalpost.sendtTilKopiering
import no.nav.k9punsj.ruting.Destinasjon
import no.nav.k9punsj.ruting.RutingService
import no.nav.k9punsj.tilgangskontroll.abac.IPepClient
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.CoRouterFunctionDsl
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import java.time.LocalDate
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
    journalpostService: JournalpostService,
    innsendingClient: InnsendingClient,
    rutingService: RutingService,
    pdlService: PdlService
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
        fagsakYtelseType: FagsakYtelseType,
        aktørIder: Set<String>
    ) = rutingService.destinasjon(
        søker = dto.fra,
        pleietrengende = dto.barn,
        annenPart = dto.annenPart,
        journalpostIds = setOf(journalpost.journalpostId),
        fagsakYtelseType = fagsakYtelseType,
        aktørIder = aktørIder,
        fraOgMed = LocalDate.now()
    ) == Destinasjon.K9Sak

    suspend fun tilKanRutesTilK9(
        dto: KopierJournalpostDto,
        journalpost: JournalpostInfo,
        fagsakYtelseType: FagsakYtelseType,
        aktørIder: Set<String>
    ) = rutingService.destinasjon(
        søker = dto.til,
        pleietrengende = dto.barn,
        annenPart = dto.annenPart,
        journalpostIds = setOf(journalpost.journalpostId),
        fagsakYtelseType = fagsakYtelseType,
        aktørIder = aktørIder,
        fraOgMed = LocalDate.now()
    ) == Destinasjon.K9Sak

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

            val aktørId = pdlService.aktørIdFor(dto.fra)?.let { setOf(it) }?.toSet() ?: emptySet()

            val k9FagsakYtelseType = journalpost?.ytelse?.let {
                journalpost.utledK9sakFagsakYtelseType(k9sakFagsakYtelseType = no.nav.k9.kodeverk.behandling.FagsakYtelseType.fraKode(it))
            } ?: return@RequestContext kanIkkeKopieres("Finner ikke ytelse for journalpost.")


            val fagsakYtelseType = FagsakYtelseType.fromKode(journalpost.ytelse)

            // Om det kopieres til samme person gjør vi kun rutingsjekk uten journalpostId
            if (dto.fra == dto.til) {
                if (!tilKanRutesTilK9(dto, journalpostInfo, fagsakYtelseType, aktørId)) {
                    return@RequestContext kanIkkeKopieres("Kan ikke rutes til K9.")
                }
            } else {
                if (!fraKanRutesTilK9(dto, journalpostInfo, fagsakYtelseType, aktørId)) {
                    return@RequestContext kanIkkeKopieres("Kan ikke rutes til K9 grunnet fra-person.")
                }
                if (!tilKanRutesTilK9(dto, journalpostInfo, fagsakYtelseType, aktørId)) {
                    return@RequestContext kanIkkeKopieres("Kan ikke rutes til K9 grunnet til-person.")
                }
            }

            if (journalpost?.type != null && journalpost.type == PunsjInnsendingType.INNTEKTSMELDING_UTGÅTT.kode) {
                return@RequestContext kanIkkeKopieres("Kan ikke kopier journalpost med type inntektsmelding utgått.")
            }

            val støttedeYtelseTyperForKopiering = listOf(
                FagsakYtelseType.OMSORGSPENGER_KRONISK_SYKT_BARN,
                FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
                FagsakYtelseType.PLEIEPENGER_LIVETS_SLUTTFASE
            )

            if (!støttedeYtelseTyperForKopiering.contains(fagsakYtelseType)) {
                return@RequestContext kanIkkeKopieres("Støtter ikke kopiering av ${fagsakYtelseType.navn} for relaterte journalposter")
            }

            innsendingClient.sendKopierJournalpost(
                KopierJournalpostInfo(
                    journalpostId = journalpostId,
                    fra = dto.fra,
                    til = dto.til,
                    pleietrengende = dto.barn,
                    ytelse = k9FagsakYtelseType
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

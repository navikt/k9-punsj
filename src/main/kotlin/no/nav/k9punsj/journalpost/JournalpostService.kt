package no.nav.k9punsj.journalpost

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import kotlinx.coroutines.runBlocking
import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9punsj.felles.Identitetsnummer
import no.nav.k9punsj.felles.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.k9punsj.felles.IkkeTilgang
import no.nav.k9punsj.felles.JournalpostId
import no.nav.k9punsj.felles.PunsjFagsakYtelseType
import no.nav.k9punsj.felles.Sak
import no.nav.k9punsj.felles.dto.JournalposterDto
import no.nav.k9punsj.felles.dto.SøknadEntitet
import no.nav.k9punsj.fordel.K9FordelType
import no.nav.k9punsj.integrasjoner.dokarkiv.DokarkivGateway
import no.nav.k9punsj.integrasjoner.dokarkiv.Dokument
import no.nav.k9punsj.integrasjoner.dokarkiv.JournalPostRequest
import no.nav.k9punsj.integrasjoner.dokarkiv.JournalPostResponse
import no.nav.k9punsj.integrasjoner.dokarkiv.SafDtos
import no.nav.k9punsj.integrasjoner.dokarkiv.SafGateway
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import no.nav.k9punsj.integrasjoner.k9sak.dto.HentK9SaksnummerGrunnlag
import no.nav.k9punsj.journalpost.dto.DokumentInfo
import no.nav.k9punsj.journalpost.dto.JournalpostInfo
import no.nav.k9punsj.journalpost.dto.KopierJournalpostDto
import no.nav.k9punsj.journalpost.dto.PunsjJournalpost
import no.nav.k9punsj.journalpost.dto.PunsjJournalpostKildeType
import no.nav.k9punsj.journalpost.dto.utledK9sakFagsakYtelseType
import no.nav.k9punsj.journalpost.postmottak.JournalpostMottaksHaandteringDto
import no.nav.k9punsj.utils.PeriodeUtils.somPeriodeDto
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

@Service
class JournalpostService(
    private val safGateway: SafGateway,
    private val journalpostRepository: JournalpostRepository,
    private val dokarkivGateway: DokarkivGateway,
    private val objectMapper: ObjectMapper
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(JournalpostService::class.java)
    }

    internal suspend fun hentDokument(journalpostId: String, dokumentId: String): Dokument? =
        safGateway.hentDokument(journalpostId, dokumentId)

    internal suspend fun hentSafJournalPost(journalpostId: String): SafDtos.Journalpost? =
        safGateway.hentJournalpostInfo(journalpostId)

    internal suspend fun hentBehandlingsAar(journalpostId: String): Int {
        val behandlingsAar = journalpostRepository.hent(journalpostId).behandlingsAar
        logger.info("Hentet behandlingsår ($behandlingsAar) for journalpost: $journalpostId")
        return behandlingsAar ?: LocalDate.now().year
    }

    internal suspend fun lagreBehandlingsAar(journalpostId: String, behandlingsAar: Int) {
        val journalpost = journalpostRepository.hentHvis(journalpostId)
        if (journalpost != null) {
            logger.info("Oppdaterer behandlingsår ($behandlingsAar) for journalpost: $journalpostId")
            val medBehandlingsAar = journalpost.copy(behandlingsAar = behandlingsAar)
            journalpostRepository.lagre(medBehandlingsAar) {
                medBehandlingsAar
            }
        }
    }

    internal suspend fun hentJournalpostInfo(journalpostId: String): JournalpostInfo? {
        val safJournalpost = safGateway.hentJournalpostInfo(journalpostId)

        return if (safJournalpost == null) {
            null
        } else {
            val parsedJournalpost = safJournalpost.parseJournalpost()
            if (!parsedJournalpost.harTilgang) {
                val maskertJournalpost = safJournalpost.copy(
                    avsenderMottaker = SafDtos.AvsenderMottaker(null, null, null),
                    bruker = SafDtos.Bruker(null, null)
                )
                logger.warn("Saksbehandler har ikke tilgang. Journalpost: [$maskertJournalpost]")
                throw IkkeTilgang("Saksbehandler har ikke tilgang.")
            } else {
                val (norskIdent, aktørId) = when {
                    SafDtos.BrukerType.FNR == parsedJournalpost.brukerType -> safJournalpost.bruker?.id to null
                    SafDtos.BrukerType.AKTOERID == parsedJournalpost.brukerType -> null to safJournalpost.bruker?.id
                    SafDtos.AvsenderMottakertype.FNR == parsedJournalpost.avsenderMottakertype -> safJournalpost.avsenderMottaker?.id to null
                    else -> null to null
                }

                val mottattDato = utledMottattDato(parsedJournalpost)

                JournalpostInfo(
                    journalpostId = journalpostId,
                    dokumenter = safJournalpost.dokumenter.map { DokumentInfo(it.dokumentInfoId) },
                    norskIdent = norskIdent,
                    aktørId = aktørId,
                    mottattDato = mottattDato,
                    erInngående = SafDtos.JournalpostType.I == parsedJournalpost.journalpostType,
                    journalpostStatus = safJournalpost.journalstatus!!,
                    journalpostType = safJournalpost.journalposttype
                )
            }
        }
    }

    // TODO: Kan sende in betyr att den har blitt sendt in fra før? FIXME
    internal fun kanSendesInn(søknadEntitet: SøknadEntitet): MutableSet<String> {
        val journalPoster = søknadEntitet.journalposter!!
        val journalposterDto: JournalposterDto = objectMapper.convertValue(journalPoster)
        val journalpostIder = journalposterDto.journalposter.filter { journalpostId ->
            journalpostRepository.kanSendeInn(listOf(journalpostId)).also { kanSendesInn ->
                if (!kanSendesInn) {
                    logger.warn("JournalpostId $journalpostId kan ikke sendes inn. Filtreres bort fra innsendingen.")
                }
            }
        }.toMutableSet()

        return journalpostIder
    }

    internal suspend fun settKildeHvisIkkeFinnesFraFør(journalposter: List<String>?, aktørId: String) {
        journalposter?.forEach {
            if (journalpostRepository.journalpostIkkeEksisterer(it)) {
                val punsjJournalpost = PunsjJournalpost(UUID.randomUUID(), it, aktørId)
                journalpostRepository.lagre(punsjJournalpost, PunsjJournalpostKildeType.SAKSBEHANDLER) {
                    punsjJournalpost
                }
            }
        }
    }

    internal suspend fun settFagsakYtelseType(ytelseType: PunsjFagsakYtelseType, journalpostId: String) {
        val journalpost = journalpostRepository.hentHvis(journalpostId)
        if (journalpost != null) {
            val medType = journalpost.copy(ytelse = ytelseType.kode)
            journalpostRepository.lagre(medType) {
                medType
            }
        }
    }

    internal suspend fun oppdaterOgFerdigstillForMottak(
        dto: JournalpostMottaksHaandteringDto,
        saksnummer: String,
    ): Pair<HttpStatusCode, String> {
        val journalpostDataFraSaf = safGateway.hentDataFraSaf(dto.journalpostId)
        return dokarkivGateway.oppdaterJournalpostDataOgFerdigstill(
            dataFraSaf = journalpostDataFraSaf,
            journalpostId = dto.journalpostId,
            identitetsnummer = dto.brukerIdent.somIdentitetsnummer(),
            enhetKode = "9999",
            sak = Sak(
                sakstype = Sak.SaksType.FAGSAK,
                fagsakId = saksnummer
            )
        )
    }

    internal suspend fun journalførMotGenerellSak(
        journalpostId: String,
        identitetsnummer: Identitetsnummer,
        enhetKode: String,
    ): Pair<HttpStatusCode, String> {
        val hentDataFraSaf = safGateway.hentDataFraSaf(journalpostId)
        return dokarkivGateway.oppdaterJournalpostDataOgFerdigstill(
            dataFraSaf = hentDataFraSaf,
            journalpostId = journalpostId,
            identitetsnummer = identitetsnummer,
            enhetKode = enhetKode,
            sak = Sak(sakstype = Sak.SaksType.GENERELL_SAK)
        )
    }

    internal suspend fun opprettJournalpost(journalPostRequest: JournalPostRequest): JournalPostResponse {
        return dokarkivGateway.opprettOgFerdigstillJournalpost(journalPostRequest)
    }

    private fun utledMottattDato(parsedSafJournalpost: ParsedSafJournalpost): LocalDateTime {
        return if (parsedSafJournalpost.journalpostType == SafDtos.JournalpostType.I) {
            parsedSafJournalpost.relevanteDatoer.firstOrNull { it.datotype == SafDtos.Datotype.DATO_REGISTRERT }?.dato
        } else {
            parsedSafJournalpost.relevanteDatoer.firstOrNull { it.datotype == SafDtos.Datotype.DATO_JOURNALFOERT }?.dato
        } ?: parsedSafJournalpost.relevanteDatoer.firstOrNull { it.datotype == SafDtos.Datotype.DATO_OPPRETTET }?.dato
        ?: logger.warn(
            "Fant ikke relevant dato ved utleding av mottatt dato. Bruker dagens dato. RelevanteDatoer=${parsedSafJournalpost.relevanteDatoer.map { it.datotype.name }}"
        ).let { LocalDateTime.now(ZoneId.of("Europe/Oslo")) }
    }

    internal suspend fun finnJournalposterPåPerson(aktørId: String): List<PunsjJournalpost> {
        return journalpostRepository.finnJournalposterPåPerson(aktørId)
    }

    internal suspend fun finnJournalposterPåPersonBareFraFordel(aktørId: String): List<PunsjJournalpost> {
        return journalpostRepository.finnJournalposterPåPersonBareFordel(aktørId)
    }

    internal suspend fun hentHvisJournalpostMedId(journalpostId: String): PunsjJournalpost? {
        return journalpostRepository.hentHvis(journalpostId)
    }

    internal suspend fun hentHvisJournalpostMedIder(journalpostId: List<String>): Map<PunsjJournalpost, Boolean> {
        return journalpostRepository.hentHvis(journalpostId)
    }

    @Deprecated("Kan sende in betyr att den har blitt sendt in fra før? FIXME")
    internal suspend fun kanSendeInn(journalpostId: List<String>): Boolean {
        return journalpostRepository.kanSendeInn(journalpostId)
    }

    internal suspend fun lagre(
        punsjJournalpost: PunsjJournalpost,
        kilde: PunsjJournalpostKildeType = PunsjJournalpostKildeType.FORDEL,
    ) {
        journalpostRepository.lagre(punsjJournalpost, kilde) {
            punsjJournalpost
        }
    }

    internal suspend fun settTilFerdig(
        journalpostId: String,
        ferdigstillJournalpost: Boolean = false,
        enhet: String? = null,
        sak: Sak? = null,
        søkerIdentitetsnummer: Identitetsnummer? = null,
    ): Pair<HttpStatusCode, String?> {
        if (ferdigstillJournalpost) {
            require(!enhet.isNullOrBlank()) { "Enhet kan ikke være null dersom journalpost skal ferdigstilles." }
            require(sak != null) { "Sak kan ikke være null dersom journalpost skal ferdigstilles." }
            require(søkerIdentitetsnummer != null) { "SøkerIdentitetsnummer kan ikke være null dersom journalpost skal ferdigstilles." }

            val safJournalPost = hentSafJournalPost(journalpostId)!!

            val parseJournalpost = safJournalPost.parseJournalpost()
            if (parseJournalpost.ikkeErFerdigBehandlet()) {
                logger.info("Ferdigstiller journalpost med id=[{}]", journalpostId)
                logger.info("Oppdaterer journalpost med ny sak=[{}], gammel sak=[{}]", sak, parseJournalpost.sak)

                val (status, body) = dokarkivGateway.oppdaterJournalpostDataOgFerdigstill(
                    dataFraSaf = JSONObject(mapOf("journalpost" to safJournalPost)),
                    journalpostId = journalpostId,
                    identitetsnummer = søkerIdentitetsnummer,
                    enhetKode = enhet,
                    sak = sak
                )

                if (!status.is2xxSuccessful) {
                    logger.error("Feilet med å ferdigstille journalpost med id = [{}]", journalpostId)
                    return status to body
                }
            } else {
                logger.info("Journalpost er allerede ferdigstilt.")
            }
        }
        journalpostRepository.ferdig(journalpostId)
        return HttpStatus.OK to "OK"
    }

    suspend fun settTilFerdig(journalpostId: String) {
        journalpostRepository.ferdig(journalpostId)
    }

    internal suspend fun journalpostIkkeEksisterer(journalpostId: String): Boolean {
        return journalpostRepository.journalpostIkkeEksisterer(journalpostId)
    }

    internal suspend fun hent(journalpostId: String): PunsjJournalpost {
        return journalpostRepository.hent(journalpostId)
    }

    internal suspend fun settAlleTilFerdigBehandlet(journalpostIder: List<String>) {
        return journalpostRepository.settAlleTilFerdigBehandlet(journalpostIder)
    }

    internal suspend fun opprettJournalpost(jp: PunsjJournalpost): PunsjJournalpost {
        return journalpostRepository.opprettJournalpost(jp)
    }

    internal suspend fun settInnsendingstype(type: K9FordelType, journalpostId: String) {
        journalpostRepository.settInnsendingstype(type, journalpostId)
    }

    @Deprecated("Skall kun brukes for å hente ut journalposter som skal sendes til k9-los-api for ny oppgavemodell")
    internal suspend fun hentÅpneJournalposter(): List<PunsjJournalpost> {
        return journalpostRepository.hentÅpneJournalposter()
    }
}

private fun SafDtos.Journalpost.parseJournalpost(): ParsedSafJournalpost {
    val arkivDokumenter = dokumenter
        .filter { it.dokumentvarianter != null && it.dokumentvarianter.isNotEmpty() }
        .onEach { it ->
            it.dokumentvarianter!!.removeIf {
                !it.variantformat.equals(SafDtos.VariantFormat.ARKIV.name, ignoreCase = true)
            }
        }

    return ParsedSafJournalpost(
        journalpostType = enumValueOfOrNull<SafDtos.JournalpostType>(journalposttype),
        brukerType = enumValueOfOrNull<SafDtos.BrukerType>(bruker?.type),
        avsenderType = enumValueOfOrNull<SafDtos.AvsenderType>(avsender?.type),
        tema = enumValueOfOrNull<SafDtos.Tema>(tema),
        journalstatus = enumValueOfOrNull<SafDtos.Journalstatus>(journalstatus),
        arkivDokumenter = arkivDokumenter,
        sak = sak,
        tittel = tittel,
        harTilgang = arkivDokumenter.none { it ->
            it.dokumentvarianter!!.any {
                !it.saksbehandlerHarTilgang
            }
        },
        avsenderMottakertype = enumValueOfOrNull<SafDtos.AvsenderMottakertype>(avsenderMottaker?.type),
        relevanteDatoer = relevanteDatoer
    )
}

private data class ParsedSafJournalpost(
    val journalpostType: SafDtos.JournalpostType?,
    val brukerType: SafDtos.BrukerType?,
    val avsenderType: SafDtos.AvsenderType?,
    val tema: SafDtos.Tema?,
    val sak: SafDtos.Sak?,
    val journalstatus: SafDtos.Journalstatus?,
    val arkivDokumenter: List<SafDtos.Dokument>,
    val harTilgang: Boolean,
    val avsenderMottakertype: SafDtos.AvsenderMottakertype?,
    val relevanteDatoer: List<SafDtos.RelevantDato>,
    val tittel: String?,
) {
    fun ikkeErFerdigBehandlet(): Boolean {
        return !listOf(
            SafDtos.Journalstatus.JOURNALFOERT,
            SafDtos.Journalstatus.FERDIGSTILT
        )
            .contains(journalstatus)
    }
}

private inline fun <reified T : Enum<T>> enumValueOfOrNull(name: String?) =
    enumValues<T>().find { it.name.equals(name, ignoreCase = true) }

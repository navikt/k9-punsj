package no.nav.k9punsj.journalpost

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.Identitetsnummer
import no.nav.k9punsj.felles.IkkeTilgang
import no.nav.k9punsj.felles.PunsjJournalpostKildeType
import no.nav.k9punsj.felles.dto.JournalposterDto
import no.nav.k9punsj.felles.dto.SøknadEntitet
import no.nav.k9punsj.fordel.PunsjInnsendingType
import no.nav.k9punsj.integrasjoner.dokarkiv.DokarkivGateway
import no.nav.k9punsj.integrasjoner.dokarkiv.Dokument
import no.nav.k9punsj.integrasjoner.dokarkiv.JournalPostRequest
import no.nav.k9punsj.integrasjoner.dokarkiv.JournalPostResponse
import no.nav.k9punsj.integrasjoner.dokarkiv.OppdaterJournalpostRequest
import no.nav.k9punsj.integrasjoner.dokarkiv.SafDtos
import no.nav.k9punsj.integrasjoner.dokarkiv.SafGateway
import no.nav.k9punsj.integrasjoner.dokarkiv.Sak
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
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
    private val objectMapper: ObjectMapper,
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(JournalpostService::class.java)
    }

    internal suspend fun hentDokument(journalpostId: String, dokumentId: String): Dokument? =
        safGateway.hentDokument(journalpostId, dokumentId)

    internal suspend fun hentSafJournalPost(journalpostId: String): SafDtos.Journalpost? =
        safGateway.hentJournalpostInfo(journalpostId)

    internal suspend fun hentJournalpostInfo(journalpostId: String): JournalpostInfo? {
        val safJournalpost = safGateway.hentJournalpostInfo(journalpostId)

        return if (safJournalpost == null) {
            null
        } else {
            val parsedJournalpost = safJournalpost.parseJournalpost()
            if (!parsedJournalpost.harTilgang) {
                val maskertJournalpost = safJournalpost.copy(
                    avsenderMottaker = SafDtos.AvsenderMottaker(null, null),
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
                    kanOpprettesJournalføringsoppgave = (SafDtos.JournalpostType.I == parsedJournalpost.journalpostType && SafDtos.Journalstatus.MOTTATT == parsedJournalpost.journalstatus).also {
                        if (!it) {
                            logger.info(
                                "Kan ikke opprettes journalføringsoppgave. Journalposttype=${safJournalpost.journalposttype}, Journalstatus=${safJournalpost.journalstatus}",
                                keyValue("journalpost_id", journalpostId)
                            )
                        }
                    },
                    journalpostStatus = safJournalpost.journalstatus!!
                )
            }
        }
    }

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

    internal suspend fun settFagsakYtelseType(ytelseType: FagsakYtelseType, journalpostId: String) {
        val journalpost = journalpostRepository.hentHvis(journalpostId)
        if (journalpost != null) {
            val medType = journalpost.copy(ytelse = ytelseType.kode)
            journalpostRepository.lagre(medType) {
                medType
            }
        }
    }

    internal suspend fun journalførMotGenerellSak(
        journalpostId: String,
        identitetsnummer: Identitetsnummer,
        enhetKode: String,
    ): Int {
        val hentDataFraSaf = safGateway.hentDataFraSaf(journalpostId)
        return dokarkivGateway.oppdaterJournalpostData(hentDataFraSaf, journalpostId, identitetsnummer, enhetKode)
    }

    internal suspend fun opprettJournalpost(journalPostRequest: JournalPostRequest): JournalPostResponse {
        return dokarkivGateway.opprettJournalpost(journalPostRequest)
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

    internal suspend fun hentPunsjInnsendingType(journalpostId: String): PunsjInnsendingType? {
        return hentHvisJournalpostMedId(journalpostId)?.type
            .let { if (it != null) PunsjInnsendingType.fraKode(it) else null }
    }

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
    ): Pair<HttpStatus, String?> {
        if (ferdigstillJournalpost) {
            require(!enhet.isNullOrBlank()) { "Enhet kan ikke være null dersom journalpost skal ferdigstilles." }
            require(sak != null) { "Sak kan ikke være null dersom journalpost skal ferdigstilles." }
            logger.info("Enhet = [{}]", enhet) // TODO: Fjern før prodsetting
            logger.info("Sakrelasjon = [{}]", sak) // TODO: Fjern før prodsetting

            val parseJournalpost = hentSafJournalPost(journalpostId)!!.parseJournalpost()
            if (parseJournalpost.journalstatus != SafDtos.Journalstatus.FERDIGSTILT) {
                logger.info("Ferdigstiller journalpost med id=[{}]", journalpostId)

                if (parseJournalpost.sak == null) {
                    logger.info("Journalpost har ingen sakrelasjon. Oppdaterer journalpost med sak = [$sak]")
                    dokarkivGateway.oppdaterJournalpost(
                        journalpostId = journalpostId,
                        oppdaterJournalpostRequest = OppdaterJournalpostRequest(sak = sak)
                    )
                }

                val ferdigstillJournalpostRespons = dokarkivGateway.ferdigstillJournalpost(journalpostId, enhet)
                if (!ferdigstillJournalpostRespons.statusCode.is2xxSuccessful) {
                    logger.error("Feilet med å ferdigstille journalpost med id = [{}]", journalpostId)
                    return ferdigstillJournalpostRespons.statusCode to ferdigstillJournalpostRespons.body
                }

                return ferdigstillJournalpostRespons.statusCode to ferdigstillJournalpostRespons.body
            } else {
                logger.info("Journalpost er allerede ferdigstilt.")
            }
        }
        journalpostRepository.ferdig(journalpostId)
        return HttpStatus.OK to "OK"
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

    internal suspend fun settInnsendingstype(type: PunsjInnsendingType, journalpostId: String) {
        journalpostRepository.settInnsendingstype(type, journalpostId)
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
)

internal data class JournalpostInfo(
    val journalpostId: String,
    val norskIdent: String?,
    val aktørId: String?,
    val dokumenter: List<DokumentInfo>,
    val mottattDato: LocalDateTime,
    val erInngående: Boolean,
    val kanOpprettesJournalføringsoppgave: Boolean,
    val journalpostStatus: String,
)

data class JournalpostInfoDto(
    val journalpostId: String,
    val norskIdent: String?,
    val dokumenter: List<DokumentInfo>,
    val venter: VentDto?,
    val punsjInnsendingType: PunsjInnsendingType?,
    @JsonIgnore
    val erInngående: Boolean,
    val kanSendeInn: Boolean,
    val erSaksbehandler: Boolean? = null,
    val journalpostStatus: String,
    val kanOpprettesJournalføringsoppgave: Boolean,
    val kanKopieres: Boolean = punsjInnsendingType != PunsjInnsendingType.KOPI && erInngående, // Brukes av frontend,
    val gosysoppgaveId: String?,
)

data class VentDto(
    val venteÅrsak: String,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val venterTil: LocalDate,
)

data class DokumentInfo(
    val dokumentId: String,
)

inline fun <reified T : Enum<T>> enumValueOfOrNull(name: String?) =
    enumValues<T>().find { it.name.equals(name, ignoreCase = true) }

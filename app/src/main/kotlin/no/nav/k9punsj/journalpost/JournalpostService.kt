package no.nav.k9punsj.journalpost

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.k9punsj.db.datamodell.AktørId
import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import no.nav.k9punsj.db.datamodell.NorskIdent
import no.nav.k9punsj.fordel.PunsjInnsendingType
import no.nav.k9punsj.rest.web.JournalpostId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class JournalpostService(
    private val safGateway: SafGateway,
    private val journalpostRepository: JournalpostRepository,
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(JournalpostService::class.java)
    }

    internal suspend fun hentDokument(journalpostId: JournalpostId, dokumentId: DokumentId): Dokument? =
        safGateway.hentDokument(journalpostId, dokumentId)

    internal suspend fun hentJournalpostInfo(journalpostId: JournalpostId): JournalpostInfo? {
        val safJournalpost = safGateway.hentJournalpostInfo(journalpostId)

        return if (safJournalpost == null) {
            null
        } else {
            val parsedJournalpost = safJournalpost.parseJournalpost()
            if (!parsedJournalpost.harTilgang) {
                logger.warn("Saksbehandler har ikke tilgang. ${
                    safJournalpost.copy(avsenderMottaker = SafDtos.AvsenderMottaker(null, null),
                        bruker = SafDtos.Bruker(null, null))
                }")
                throw IkkeTilgang()
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
                    erInngående = SafDtos.JournalpostType.I == parsedJournalpost.journalpostType
                )
            }
        }
    }

    private fun utledMottattDato(parsedJournalpost: ParsedJournalpost) : LocalDateTime {
        return if (parsedJournalpost.journalpostType == SafDtos.JournalpostType.I) {
            hentRelevantDato(parsedJournalpost, SafDtos.Datotype.DATO_REGISTRERT)
        } else {
            hentRelevantDato(parsedJournalpost, SafDtos.Datotype.DATO_JOURNALFOERT)
        }
    }

    private fun hentRelevantDato(parsedJournalpost: ParsedJournalpost, datotype: SafDtos.Datotype): LocalDateTime {
        return parsedJournalpost.relevanteDatoer.first { d -> d.datotype == datotype }.dato
    }

    internal suspend fun finnJournalposterPåPerson(aktørId: AktørId): List<Journalpost> {
        return journalpostRepository.finnJournalposterPåPerson(aktørId)
    }

    internal suspend fun hentHvisJournalpostMedId(journalpostId: JournalpostId): Journalpost? {
        return journalpostRepository.hentHvis(journalpostId)
    }

    internal suspend fun lagre(journalpost: Journalpost, kilde: KildeType = KildeType.FORDEL) {
        journalpostRepository.lagre(journalpost, kilde) {
            journalpost
        }
    }

    internal suspend fun omfordelJournalpost(journalpostId: JournalpostId, ytelse: FagsakYtelseType) {
        // TODO: Legge på en kafka-topic k9-fordel håndterer.
    }

    internal suspend fun settTilFerdig(journalpostId: JournalpostId) {
        journalpostRepository.ferdig(journalpostId)
    }
}

private fun SafDtos.Journalpost.parseJournalpost(): ParsedJournalpost {
    val arkivDokumenter = dokumenter
        .filter { it.dokumentvarianter != null && it.dokumentvarianter.isNotEmpty() }
        .onEach { it ->
            it.dokumentvarianter!!.removeIf {
                !it.variantformat.equals(SafDtos.VariantFormat.ARKIV.name, ignoreCase = true)
            }
        }

    return ParsedJournalpost(
        journalpostType = enumValueOfOrNull<SafDtos.JournalpostType>(journalposttype),
        brukerType = enumValueOfOrNull<SafDtos.BrukerType>(bruker?.type),
        avsenderType = enumValueOfOrNull<SafDtos.AvsenderType>(avsender?.type),
        tema = enumValueOfOrNull<SafDtos.Tema>(tema),
        journalstatus = enumValueOfOrNull<SafDtos.Journalstatus>(journalstatus),
        arkivDokumenter = arkivDokumenter,
        harTilgang = arkivDokumenter.none { it ->
            it.dokumentvarianter!!.any {
                !it.saksbehandlerHarTilgang
            }
        },
        avsenderMottakertype = enumValueOfOrNull<SafDtos.AvsenderMottakertype>(avsenderMottaker?.type),
        relevanteDatoer = relevanteDatoer
    )
}

private data class ParsedJournalpost(
    val journalpostType: SafDtos.JournalpostType?,
    val brukerType: SafDtos.BrukerType?,
    val avsenderType: SafDtos.AvsenderType?,
    val tema: SafDtos.Tema?,
    val journalstatus: SafDtos.Journalstatus?,
    val arkivDokumenter: List<SafDtos.Dokument>,
    val harTilgang: Boolean,
    val avsenderMottakertype: SafDtos.AvsenderMottakertype?,
    val relevanteDatoer: List<SafDtos.RelevantDato>,
)

data class JournalpostInfo(
    val journalpostId: JournalpostId,
    val norskIdent: NorskIdent?,
    val aktørId: AktørId?,
    val dokumenter: List<DokumentInfo>,
    val mottattDato: LocalDateTime,
    val erInngående: Boolean
)

data class JournalpostInfoDto(
    val journalpostId: JournalpostId,
    val norskIdent: NorskIdent?,
    val dokumenter: List<DokumentInfo>,
    val venter: VentDto?,
    val punsjInnsendingType: PunsjInnsendingType?
)

data class VentDto(
    val venteÅrsak: String,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val venterTil: LocalDate,
)

data class DokumentInfo(
    val dokumentId: DokumentId,
)

internal class IkkeStøttetJournalpost : Throwable("Punsj støtter ikke denne journalposten.")
internal class IkkeTilgang : Throwable("Saksbehandler har ikke tilgang på alle dokumeter i journalposten.")

inline fun <reified T : Enum<T>> enumValueOfOrNull(name: String?) =
    enumValues<T>().find { it.name.equals(name, ignoreCase = true) }

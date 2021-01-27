package no.nav.k9punsj.journalpost

import no.nav.k9punsj.JournalpostId
import no.nav.k9punsj.NorskIdent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class JournalpostService(
        private val safGateway: SafGateway
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
            if (!parsedJournalpost.støttetJournalpost) {
                logger.warn("Oppslag på journalpost som ikke støttes. $safJournalpost")
                throw IkkeStøttetJournalpost()
            } else if (!parsedJournalpost.harTilgang) {
                logger.warn("Saksbehandler har ikke tilgang. ${
                    safJournalpost.copy(avsenderMottaker = SafDtos.AvsenderMottaker(null, null), bruker = SafDtos.Bruker(null, null))
                }")
                throw IkkeTilgang()
            } else {
                val norskIdent: NorskIdent? = if (parsedJournalpost.brukerType == SafDtos.BrukerType.FNR) {
                    safJournalpost.bruker?.id
                } else if (parsedJournalpost.avsenderMottakertype == SafDtos.AvsenderMottakertype.FNR) {
                    safJournalpost.avsenderMottaker?.id
                } else null

                JournalpostInfo(
                        journalpostId = journalpostId,
                        dokumenter = safJournalpost.dokumenter.map { DokumentInfo(it.dokumentInfoId) },
                        norskIdent = norskIdent
                )
            }
        }
    }

    internal suspend fun omfordelJournalpost(journalpostId: JournalpostId, ytelse: Ytelse) {
        // TODO: Legge på en kafka-topic k9-fordel håndterer.
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
            avsenderMottakertype = enumValueOfOrNull<SafDtos.AvsenderMottakertype>(avsenderMottaker?.type)
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
        val avsenderMottakertype: SafDtos.AvsenderMottakertype?
) {
    val støttetJournalpost = listOfNotNull(
            journalpostType, tema, journalpostType
    ).size == 3
}

data class JournalpostInfo(
        val journalpostId: JournalpostId,
        val norskIdent: NorskIdent?,
        val dokumenter: List<DokumentInfo>
)

data class DokumentInfo(
        val dokumentId: DokumentId
)

enum class Ytelse {
    PleiepengerNærstående,
    Omsorgspenger,
    Opplæringspenger,
    Annet
}

internal class IkkeStøttetJournalpost : Throwable("Punsj støtter ikke denne journalposten.")
internal class IkkeTilgang : Throwable("Saksbehandler har ikke tilgang på alle dokumeter i journalposten.")

inline fun <reified T : Enum<T>> enumValueOfOrNull(name: String?) = enumValues<T>().find { it.name.equals(name, ignoreCase = true) }

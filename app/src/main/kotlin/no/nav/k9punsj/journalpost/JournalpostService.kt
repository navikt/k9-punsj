package no.nav.k9punsj.journalpost

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.k9punsj.db.datamodell.AktørId
import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import no.nav.k9punsj.db.datamodell.NorskIdent
import no.nav.k9punsj.integrasjoner.dokarkiv.SafDtos
import no.nav.k9punsj.fordel.PunsjInnsendingType
import no.nav.k9punsj.domenetjenester.dto.JournalpostId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.*

@Service
class JournalpostService(
    private val safGateway: SafGateway,
    private val journalpostRepository: JournalpostRepository,
    private val dokarkivGateway: DokarkivGateway
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(JournalpostService::class.java)
    }

    internal suspend fun hentDokument(journalpostId: JournalpostId, dokumentId: DokumentId): Dokument? =
        safGateway.hentDokument(journalpostId, dokumentId)

    internal suspend fun hentSafJournalPost(journalpostId: JournalpostId): SafDtos.Journalpost? =
        safGateway.hentJournalpostInfo(journalpostId)

    internal suspend fun hentJournalpostInfo(journalpostId: JournalpostId): JournalpostInfo? {
        val safJournalpost = safGateway.hentJournalpostInfo(journalpostId)

        return if (safJournalpost == null) {
            null
        } else {
            val parsedJournalpost = safJournalpost.parseJournalpost()
            if (!parsedJournalpost.harTilgang) {
                logger.warn(
                    "Saksbehandler har ikke tilgang. ${
                        safJournalpost.copy(
                            avsenderMottaker = SafDtos.AvsenderMottaker(null, null),
                            bruker = SafDtos.Bruker(null, null)
                        )
                    }"
                )
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

    internal suspend fun journalførMotGenerellSak(
        journalpostId: JournalpostId, identitetsnummer: Identitetsnummer,
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

    internal suspend fun finnJournalposterPåPerson(aktørId: AktørId): List<PunsjJournalpost> {
        return journalpostRepository.finnJournalposterPåPerson(aktørId)
    }

    internal suspend fun finnJournalposterPåPersonBareFraFordel(aktørId: AktørId): List<PunsjJournalpost> {
        return journalpostRepository.finnJournalposterPåPersonBareFordel(aktørId)
    }

    internal suspend fun hentHvisJournalpostMedId(journalpostId: JournalpostId): PunsjJournalpost? {
        return journalpostRepository.hentHvis(journalpostId)
    }

    internal suspend fun kanSendeInn(journalpostId: JournalpostId): Boolean {
        return journalpostRepository.kanSendeInn(listOf(journalpostId))
    }

    internal suspend fun lagre(punsjJournalpost: PunsjJournalpost, kilde: PunsjJournalpostKildeType = PunsjJournalpostKildeType.FORDEL) {
        journalpostRepository.lagre(punsjJournalpost, kilde) {
            punsjJournalpost
        }
    }

    internal suspend fun omfordelJournalpost(journalpostId: JournalpostId, ytelse: FagsakYtelseType) {
        // TODO: Legge på en kafka-topic k9-fordel håndterer.
    }

    internal suspend fun settTilFerdig(journalpostId: JournalpostId) {
        journalpostRepository.ferdig(journalpostId)
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
    val erInngående: Boolean,
    val kanOpprettesJournalføringsoppgave: Boolean,
    val journalpostStatus: String,
)

data class JournalpostInfoDto(
    val journalpostId: JournalpostId,
    val norskIdent: NorskIdent?,
    val dokumenter: List<DokumentInfo>,
    val venter: VentDto?,
    val punsjInnsendingType: PunsjInnsendingType?,
    @JsonIgnore
    val erInngående: Boolean,
    val kanSendeInn: Boolean,
    val erSaksbehandler: Boolean? = null,
    val journalpostStatus: String,
    val kanOpprettesJournalføringsoppgave: Boolean,
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
internal class NotatUnderArbeidFeil : Throwable("Notatet må ferdigstilles før det kan åpnes i Punsj")
internal class IkkeTilgang(feil: String) : Throwable(feil)
internal class FeilIAksjonslogg(feil: String) : Throwable(feil)
internal class UgyldigToken(feil: String) : Throwable(feil)
internal class IkkeFunnet(message: String) : Throwable(message)
internal class InternalServerErrorDoarkiv(feil: String) : Throwable(feil)

inline fun <reified T : Enum<T>> enumValueOfOrNull(name: String?) =
    enumValues<T>().find { it.name.equals(name, ignoreCase = true) }

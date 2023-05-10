package no.nav.k9punsj.domenetjenester

import no.nav.k9.kodeverk.dokument.Brevkode
import no.nav.k9.søknad.Søknad
import no.nav.k9punsj.domenetjenester.repository.SøknadRepository
import no.nav.k9punsj.felles.dto.SøknadEntitet
import no.nav.k9punsj.hentCorrelationId
import no.nav.k9punsj.innsending.InnsendingClient
import no.nav.k9punsj.integrasjoner.dokarkiv.SafDtos
import no.nav.k9punsj.integrasjoner.dokarkiv.SafGateway
import no.nav.k9punsj.journalpost.JournalpostService
import no.nav.k9punsj.metrikker.SøknadMetrikkService
import no.nav.k9punsj.utils.objectMapper
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import kotlin.coroutines.coroutineContext

@Service
internal class SoknadService(
    private val journalpostService: JournalpostService,
    private val søknadRepository: SøknadRepository,
    private val innsendingClient: InnsendingClient,
    private val søknadMetrikkService: SøknadMetrikkService,
    private val safGateway: SafGateway
) {

    init {
        logger.info("SøknadService init: innsendingClient = ${innsendingClient.toString()}")
    }

    internal suspend fun sendSøknad(
        søknad: Søknad,
        brevkode: Brevkode,
        journalpostIder: MutableSet<String>
    ): Pair<HttpStatus, String>? {
        val journalpostIdListe = journalpostIder.toList()
        val journalposterKanSendesInn = journalpostService.kanSendeInn(journalpostIdListe)
        val punsjetAvSaksbehandler = søknadRepository.hentSøknad(søknad.søknadId.id)?.endret_av!!.replace("\"", "")

        if (!journalposterKanSendesInn) {
            return HttpStatus.CONFLICT to "En eller alle journalpostene $journalpostIder har blitt sendt inn fra før"
        }

        val journalposter = safGateway.hentJournalposter(journalpostIdListe)
        val journalposterMedTypeUtgaaende = journalposter.filterNotNull()
            .filter { it.journalposttype.equals(SafDtos.JournalpostType.U) }
            .map { it.journalpostId }
            .toSet()
        if (journalposterMedTypeUtgaaende.isNotEmpty()) {
            return HttpStatus.CONFLICT to "Journalposter av type utgående ikke støttet: $journalposterMedTypeUtgaaende"
        }

        val journalposterMedStatusFeilregistrert = journalposter.filterNotNull()
            .filter { it.journalstatus != null }
            .filter { it.journalstatus!!.equals(SafDtos.Journalstatus.FEILREGISTRERT.toString()) }
            .map { it.journalpostId }
            .toSet()
        if (journalposterMedStatusFeilregistrert.isNotEmpty()) {
            return HttpStatus.CONFLICT to "Journalposter med status feilregistrert ikke støttet: $journalposterMedStatusFeilregistrert"
        }

        try {
            innsendingClient.sendSøknad(
                søknadId = søknad.søknadId.id,
                søknad = søknad,
                correlationId = coroutineContext.hentCorrelationId(),
                tilleggsOpplysninger = mapOf(
                    PunsjetAvSaksbehandler to punsjetAvSaksbehandler,
                    Søknadtype to brevkode.kode
                )
            )
        } catch (e: Exception) {
            logger.error("Feil vid innsending av søknad for journalpostIder: ${journalpostIder.joinToString(", ")}")
            return Pair(HttpStatus.INTERNAL_SERVER_ERROR, "Fail")
        }

        leggerVedPayload(søknad, journalpostIder)
        journalpostService.settAlleTilFerdigBehandlet(journalpostIdListe)
        logger.info("Punsj har market disse journalpostIdene $journalpostIder som ferdigbehandlet")
        søknadRepository.markerSomSendtInn(søknad.søknadId.id)

        søknadMetrikkService.publiserMetrikker(søknad)
        return null
    }

    suspend fun hentSøknad(søknadId: String): SøknadEntitet? {
        return søknadRepository.hentSøknad(søknadId)
    }

    suspend fun opprettSøknad(søknad: SøknadEntitet): SøknadEntitet {
        return søknadRepository.opprettSøknad(søknad)
    }

    suspend fun oppdaterSøknad(søknad: SøknadEntitet) {
        søknadRepository.oppdaterSøknad(søknad)
    }

    suspend fun hentAlleSøknaderForBunke(bunkerId: String): List<SøknadEntitet> {
        return søknadRepository.hentAlleSøknaderForBunke(bunkerId)
    }

    suspend fun hentSistEndretAvSaksbehandler(søknadId: String): String {
        return søknadRepository.hentSøknad(søknadId)?.endret_av!!.replace("\"", "")
    }

    private suspend fun leggerVedPayload(
        søknad: Søknad,
        journalpostIder: MutableSet<String>
    ) {
        val writeValueAsString = objectMapper().writeValueAsString(søknad)

        journalpostIder.forEach {
            val journalpost = journalpostService.hent(it)
            val medPayload = journalpost.copy(payload = writeValueAsString)
            journalpostService.lagre(medPayload)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SoknadService::class.java)
        private const val PunsjetAvSaksbehandler = "saksbehandler"
        private const val Søknadtype = "søknadtype"
    }
}

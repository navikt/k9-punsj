package no.nav.k9punsj.journalpost

import kotlinx.coroutines.reactive.awaitFirst
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.akjonspunkter.AksjonspunktKode
import no.nav.k9punsj.akjonspunkter.AksjonspunktRepository
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.akjonspunkter.AksjonspunktStatus
import no.nav.k9punsj.felles.IkkeStøttetJournalpost
import no.nav.k9punsj.felles.IkkeTilgang
import no.nav.k9punsj.integrasjoner.dokarkiv.DokarkivGateway
import no.nav.k9punsj.integrasjoner.dokarkiv.SafDtos
import no.nav.k9punsj.journalpost.dto.ResultatDto
import no.nav.k9punsj.tilgangskontroll.AuthenticationHandler
import no.nav.k9punsj.tilgangskontroll.azuregraph.IAzureGraphService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import org.springframework.web.reactive.function.server.json
import kotlin.coroutines.coroutineContext

@Configuration
internal class JournalpostDriftRoutes(
    private val authenticationHandler: AuthenticationHandler,
    private val journalpostService: JournalpostService,
    private val aksjonspunktService: AksjonspunktService,
    private val aksjonspunktRepository: AksjonspunktRepository,
    private val dokarkivGateway: DokarkivGateway,
    private val azureGraphService: IAzureGraphService,
) {

    private companion object {
        private const val JournalpostIdKey = "journalpost_id"
        private val logger: Logger = LoggerFactory.getLogger(JournalpostDriftRoutes::class.java)
    }

    internal object Urls {
        // for drift i prod
        internal const val ResettInfoOmJournalpost = "/journalpost/resett/{$JournalpostIdKey}"
        internal const val HentHvaSomHarBlittSendtInn = "/journalpost/hentForDebugg/{$JournalpostIdKey}"
        internal const val LukkJournalposterDebugg = "/journalpost/lukkDebugg"
        internal const val LukkJournalpostDebugg = "/journalpost/lukkDebugg/{$JournalpostIdKey}"
        internal const val FerdigstillJournalpostForDebugg = "/journalpost/ferdigstillDebugg"
        internal const val OppdaterJournalpostForDebugg = "/journalpost/oppdaterDebugg"
        internal const val LukkLosOppgave = "/journalpost/lukk/losoppgave/{$JournalpostIdKey}"
    }

    @Bean
    fun JournalpostDriftRoutes() = SaksbehandlerRoutes(authenticationHandler) {
        GET("/api${Urls.ResettInfoOmJournalpost}") { request ->
            RequestContext(coroutineContext, request) {
                val journalpostId = request.journalpostId()

                val journalpost = journalpostService.hentHvisJournalpostMedId(journalpostId)
                    ?: return@RequestContext ServerResponse
                        .notFound()
                        .buildAndAwait()
                val kanSendeInn = journalpostService.kanSendeInn(listOf(journalpostId))

                if (kanSendeInn) {
                    val nyVerdi = journalpost.copy(skalTilK9 = null)
                    journalpostService.lagre(nyVerdi)

                    return@RequestContext ServerResponse
                        .ok()
                        .json()
                        .bodyValueAndAwait(ResultatDto("Journalpost med id $journalpostId har blitt resatt!"))
                }
                return@RequestContext ServerResponse
                    .badRequest()
                    .bodyValueAndAwait("Kan ikke endre på en journalpost som har blitt sendt fra punsj")
            }
        }

        GET("/api${Urls.LukkJournalpostDebugg}") { request ->
            RequestContext(coroutineContext, request) {
                val journalpostId = request.journalpostId()
                val enhet = azureGraphService.hentEnhetForInnloggetBruker().trimIndent().take(4)

                journalpostService.hentHvisJournalpostMedId(journalpostId)
                    ?: return@RequestContext ServerResponse
                        .notFound()
                        .buildAndAwait()

                val kanSendeInn = journalpostService.kanSendeInn(listOf(journalpostId))
                if (kanSendeInn) {
                    aksjonspunktService.settUtførtPåAltSendLukkOppgaveTilK9Los(journalpostId, false, null)
                    val (status, body) = journalpostService.settTilFerdig(
                        journalpostId = journalpostId,
                        ferdigstillJournalpost = false,
                        enhet = enhet,
                        sak = null,
                        søkerIdentitetsnummer = null
                    )
                    if (!status.is2xxSuccessful) {
                        return@RequestContext ServerResponse.status(status)
                            .bodyValueAndAwait(body!!)
                    }
                    logger.info("Journalpost lukkes", keyValue("journalpost_id", journalpostId))

                    return@RequestContext ServerResponse
                        .ok()
                        .json()
                        .bodyValueAndAwait(ResultatDto("Journalpost med id $journalpostId har blitt lukket i punsj og i los"))
                } else {
                    return@RequestContext ServerResponse
                        .status(HttpStatus.BAD_REQUEST)
                        .json()
                        .bodyValueAndAwait(ResultatDto("Journalpost med id $journalpostId har blitt lukket fra før! (Ingen endring)"))
                }
            }
        }

        POST("/api${Urls.LukkJournalposterDebugg}") { request ->
            RequestContext(coroutineContext, request) {
                val journalpostIder = request.journalpostIder().journalpostIder.toSet()

                val punsjJper = journalpostService.hentHvisJournalpostMedIder(journalpostIder.toList())
                if (punsjJper.isEmpty()) {
                    return@RequestContext ServerResponse
                        .notFound()
                        .buildAndAwait()
                }

                val punsjJperIder = punsjJper.keys.map { it.journalpostId }.toSet()
                val fantIkkeIPunsjTekst = diffTekst(journalpostIder, punsjJperIder, "Fantes ikke i punsj")

                val uferdigePunsj = punsjJper.filter { !it.value }.map { it.key.journalpostId }.toSet()
                val alleredeLukketIPunsjTekst = diffTekst(punsjJperIder, uferdigePunsj, "Allerede lukket i punsj")
                if (uferdigePunsj.isEmpty()) {
                    return@RequestContext ServerResponse
                        .status(HttpStatus.BAD_REQUEST)
                        .json()
                        .bodyValueAndAwait(
                            ResultatDto(
                                "Alle er ferdig behandlet i punsj eller finnes ikke i punsj: " +
                                        "$alleredeLukketIPunsjTekst $fantIkkeIPunsjTekst"
                            )
                        )
                }

                val medSafStatus =
                    uferdigePunsj.associateWith { journalpostService.hentSafJournalPost(it)!!.journalstatus }
                val ferdigStatuser =
                    arrayOf(SafDtos.Journalstatus.FERDIGSTILT.name, SafDtos.Journalstatus.JOURNALFOERT.name)
                val ferdigeSaf = medSafStatus.filter { it.value in ferdigStatuser }.keys
                val ikkeLukketISafTekst =
                    diffTekst(uferdigePunsj, ferdigeSaf, "Ikke lukket i SAF så disse ble ikke ferdigstilt")
                if (ferdigeSaf.isEmpty()) {
                    return@RequestContext ServerResponse
                        .status(HttpStatus.BAD_REQUEST)
                        .json()
                        .bodyValueAndAwait(
                            ResultatDto(
                                "Alle er ferdig behandlet i punsj, finnes ikke i punsj eller ikke lukket i SAF. " +
                                        "$ikkeLukketISafTekst. $fantIkkeIPunsjTekst $alleredeLukketIPunsjTekst"
                            )
                        )
                }

                aksjonspunktService.settUtførtPåAltSendLukkOppgaveTilK9Los(ferdigeSaf, false, null)
                journalpostService.settAlleTilFerdigBehandlet(ferdigeSaf.toList())


                return@RequestContext ServerResponse.status(HttpStatus.OK)
                    .bodyValueAndAwait(
                        ResultatDto(
                            "Lukket journalposter: $ferdigeSaf. $fantIkkeIPunsjTekst $alleredeLukketIPunsjTekst $ikkeLukketISafTekst"
                        )
                    )
            }
        }

        POST("/api${Urls.FerdigstillJournalpostForDebugg}") { request ->
            RequestContext(coroutineContext, request) {
                val journalpostIder = request.journalpostIder().journalpostIder.toSet()
                logger.info("Forsøker å ferdigstille journalposter: {}", journalpostIder)

                val journalposterIDb = journalpostService.hentHvisJournalpostMedIder(journalpostIder.toList())
                if (journalposterIDb.isEmpty()) {
                    logger.info("Fant ingen journalposter i punsj med id {}", journalpostIder)
                    return@RequestContext ServerResponse.notFound().buildAndAwait()
                }

                val (ferdigstilte, ikkeFerdigstilte) = journalposterIDb
                    .map { journalpostService.hentSafJournalPost(it.key.journalpostId)!! } // Hent saf journalpost
                    .partition { it.journalstatus == SafDtos.Journalstatus.FERDIGSTILT.name } // Del i ferdigstilte og ikke ferdigstilte

                val (medSakstilknytning, utenSakstilknytning) = ikkeFerdigstilte
                    .partition { it.sak?.fagsakId != null } // Del i med og uten sakstilknytning

                val ferdigstillJournalposterResponse = medSakstilknytning
                    .map {
                        dokarkivGateway.ferdigstillJournalpost(
                            it.journalpostId,
                            "9999"
                        )
                    } // Ferdigstill journalposter

                val (vellykkedeFerdigstillinger, feiledeFerdigstillinger) = ferdigstillJournalposterResponse
                    .partition { it.statusCode.is2xxSuccessful } // Del i vellykkede og feilede ferdigstillinger

                val ferdigstillJournalpostResponseDto = FerdigstillJournalpostResponseDto(
                    vellykketFerdigstilteJournalposter = vellykkedeFerdigstillinger.map { it.toString() },
                    feiledeFerdigstillinger = feiledeFerdigstillinger.map { it.toString() },
                    ikkeFerdigstiltUtenSakstilknytning = utenSakstilknytning.map { it.journalpostId },
                    alleredeFerdigstilteJournalposter = ferdigstilte.map { it.journalpostId }
                )
                logger.info("Response: {}", ferdigstillJournalpostResponseDto)
                return@RequestContext ServerResponse
                    .status(HttpStatus.OK)
                    .bodyValueAndAwait(
                        ferdigstillJournalpostResponseDto
                    )
            }
        }

        POST("/api${Urls.OppdaterJournalpostForDebugg}") { request ->
            RequestContext(coroutineContext, request) {
                val oppdaterJournalpostRequest = request.journalposter()
                logger.info("Forsøker å oppdatere journalposter: {}", oppdaterJournalpostRequest)

                val journalposter = oppdaterJournalpostRequest.journalposter
                val journalposterIDb =
                    journalpostService.hentHvisJournalpostMedIder(journalposter.map { it.journalpostId })
                if (journalposterIDb.isEmpty()) {
                    logger.info("Fant ingen journalposter i punsj med id {}", oppdaterJournalpostRequest)
                    return@RequestContext ServerResponse.notFound().buildAndAwait()
                }

                val (ferdigstilte, ikkeFerdigstilte) = journalposterIDb
                    .map { journalpostService.hentSafJournalPost(it.key.journalpostId)!! } // Hent saf journalpost
                    .partition { it.journalstatus == SafDtos.Journalstatus.FERDIGSTILT.name } // Del i ferdigstilte og ikke ferdigstilte


                val (vellykkedeOppdateringer, feiledeOppdateringer) = ikkeFerdigstilte
                    .map { safJournalpost: SafDtos.Journalpost ->
                        val journalpostSomOppdateres =
                            journalposter.first { safJournalpost.journalpostId == it.journalpostId }
                        dokarkivGateway.oppdaterJournalpost(
                            //language=json
                            """
                            {
                              "avsenderMottaker": {
                                "idType": "FNR",
                                "id": "${journalpostSomOppdateres.søkerFnr}"
                              }
                            }
                        """.trimIndent(),
                            journalpostId = safJournalpost.journalpostId
                        )
                    }
                    .partition { it.statusCode.is2xxSuccessful }

                val ferdigstillJournalpostResponseDto = OppdaterJournalpostResponseDto(
                    vellykkedeOppdateringer = vellykkedeOppdateringer.map { it.toString() },
                    feiledeOppdateringer = feiledeOppdateringer.map { it.toString() },
                    alleredeFerdigstilteJournalposter = ferdigstilte.map { it.journalpostId }
                )
                logger.info("Response: {}", ferdigstillJournalpostResponseDto)
                return@RequestContext ServerResponse
                    .status(HttpStatus.OK)
                    .bodyValueAndAwait(
                        ferdigstillJournalpostResponseDto
                    )
            }
        }

        GET("/api${Urls.HentHvaSomHarBlittSendtInn}") { request ->
            RequestContext(coroutineContext, request) {
                val journalpostId = request.journalpostId()
                val journalpost = journalpostService.hentHvisJournalpostMedId(journalpostId)
                    ?: return@RequestContext ServerResponse
                        .notFound()
                        .buildAndAwait()

                if (journalpost.payload != null) {
                    try {
                        journalpostService.hentJournalpostInfo(journalpostId = request.journalpostId())
                            ?: return@RequestContext ServerResponse
                                .notFound()
                                .buildAndAwait()

                        return@RequestContext ServerResponse
                            .status(HttpStatus.OK)
                            .json()
                            .bodyValueAndAwait(journalpost.payload)
                    } catch (cause: IkkeStøttetJournalpost) {
                        return@RequestContext ServerResponse
                            .status(HttpStatus.CONFLICT)
                            .json()
                            .bodyValueAndAwait("""{"type":"punsj://ikke-støttet-journalpost"}""")
                    } catch (case: IkkeTilgang) {
                        return@RequestContext ServerResponse
                            .status(HttpStatus.FORBIDDEN)
                            .buildAndAwait()
                    }
                }
                return@RequestContext ServerResponse
                    .badRequest()
                    .buildAndAwait()
            }
        }

        GET("/api${Urls.LukkLosOppgave}") { request ->
            RequestContext(coroutineContext, request) {
                val journalpostId = request.journalpostId()

                val journalpost = (journalpostService.hentHvisJournalpostMedId(journalpostId)
                    ?: return@RequestContext ServerResponse
                        .notFound()
                        .buildAndAwait())

                val losOppgaverFørEndring = aksjonspunktRepository.hentAlleAksjonspunkter(journalpost.journalpostId)

                val aksjonspunkt = AksjonspunktKode.PUNSJ to AksjonspunktStatus.UTFØRT
                aksjonspunktService.opprettAksjonspunktOgSendTilK9Los(
                    punsjJournalpost = journalpost,
                    aksjonspunkt = aksjonspunkt,
                    type = journalpost.type,
                    ytelse = journalpost.ytelse,
                    pleietrengendeAktørId = null
                )

                val losOppgaverEtterEndring = aksjonspunktRepository.hentAlleAksjonspunkter(journalpost.journalpostId)

                return@RequestContext ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(
                        mapOf(
                            "losOppgaverFørEndring" to losOppgaverFørEndring,
                            "losOppgaverEtterEndring" to losOppgaverEtterEndring,
                            "journalposttilstand" to journalpost
                        )
                    )
            }
        }
    }

    private data class FerdigstillJournalpostResponseDto(
        val vellykketFerdigstilteJournalposter: List<String?>,
        val alleredeFerdigstilteJournalposter: List<String?>,
        val feiledeFerdigstillinger: List<String?>,
        val ikkeFerdigstiltUtenSakstilknytning: List<String?>,
    )

    private data class OppdaterJournalpostResponseDto(
        val vellykkedeOppdateringer: List<String?>,
        val feiledeOppdateringer: List<String?>,
        val alleredeFerdigstilteJournalposter: List<String?>,
    )

    private fun diffTekst(setA: Set<String>, setB: Set<String>, prefix: String): String {
        val diff = setA.minus(setB)
        return if (diff.isNotEmpty()) "$prefix: $diff" else ""
    }

    private fun ServerRequest.journalpostId(): String = pathVariable(JournalpostIdKey)

    internal data class JournalpostIderRequest(val journalpostIder: List<String>)
    internal data class OppdaterJournalpostRequest(val journalposter: List<OppdaterJournalpostRequestDTO>)
    internal data class OppdaterJournalpostRequestDTO(val journalpostId: String, val søkerFnr: String) {
        override fun toString(): String {
            return "OppdaterJournalpostRequestDTO(journalpostId='$journalpostId')"
        }
    }

    private suspend fun ServerRequest.journalposter(): OppdaterJournalpostRequest =
        body(BodyExtractors.toMono(OppdaterJournalpostRequest::class.java)).awaitFirst()

    private suspend fun ServerRequest.journalpostIder(): JournalpostIderRequest =
        body(BodyExtractors.toMono(JournalpostIderRequest::class.java)).awaitFirst()

}

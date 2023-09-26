package no.nav.k9punsj.journalpost

import kotlinx.coroutines.reactive.awaitFirst
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.akjonspunkter.AksjonspunktKode
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.akjonspunkter.AksjonspunktStatus
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.IdentOgJournalpost
import no.nav.k9punsj.felles.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.k9punsj.felles.IkkeFunnet
import no.nav.k9punsj.felles.IkkeStøttetJournalpost
import no.nav.k9punsj.felles.IkkeTilgang
import no.nav.k9punsj.fordel.PunsjInnsendingType
import no.nav.k9punsj.innsending.InnsendingClient
import no.nav.k9punsj.integrasjoner.dokarkiv.SafDtos
import no.nav.k9punsj.integrasjoner.gosys.GosysService
import no.nav.k9punsj.integrasjoner.pdl.PdlService
import no.nav.k9punsj.journalpost.dto.BehandlingsAarDto
import no.nav.k9punsj.journalpost.dto.IdentDto
import no.nav.k9punsj.journalpost.dto.JournalpostInfoDto
import no.nav.k9punsj.journalpost.dto.JournalpostMottaksHaandteringDto
import no.nav.k9punsj.journalpost.dto.KopierJournalpostDto
import no.nav.k9punsj.journalpost.dto.KopierJournalpostInfo
import no.nav.k9punsj.journalpost.dto.LukkJournalpostDto
import no.nav.k9punsj.journalpost.dto.PunsjJournalpost
import no.nav.k9punsj.journalpost.dto.PunsjJournalpostKildeType
import no.nav.k9punsj.journalpost.dto.ResultatDto
import no.nav.k9punsj.journalpost.dto.SettPåVentDto
import no.nav.k9punsj.journalpost.dto.SkalTilInfotrygdSvar
import no.nav.k9punsj.journalpost.dto.utledK9sakFagsakYtelseType
import no.nav.k9punsj.openapi.OasDokumentInfo
import no.nav.k9punsj.openapi.OasFeil
import no.nav.k9punsj.openapi.OasJournalpostDto
import no.nav.k9punsj.openapi.OasJournalpostIder
import no.nav.k9punsj.tilgangskontroll.AuthenticationHandler
import no.nav.k9punsj.tilgangskontroll.InnloggetUtils
import no.nav.k9punsj.tilgangskontroll.abac.IPepClient
import no.nav.k9punsj.tilgangskontroll.azuregraph.IAzureGraphService
import no.nav.k9punsj.utils.ServerRequestUtils.hentNorskIdentHeader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import org.springframework.web.reactive.function.server.json
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.regex.Pattern
import kotlin.coroutines.coroutineContext

@Configuration
internal class JournalpostRoutes(
    private val authenticationHandler: AuthenticationHandler,
    private val journalpostService: JournalpostService,
    private val pdlService: PdlService,
    private val aksjonspunktService: AksjonspunktService,
    private val pepClient: IPepClient,
    private val innsendingClient: InnsendingClient,
    private val gosysService: GosysService,
    private val azureGraphService: IAzureGraphService,
    private val innlogget: InnloggetUtils,
    @Value("\${FERDIGSTILL_GOSYSOPPGAVE_ENABLED:false}") private val ferdigstillGosysoppgaveEnabled: Boolean
) {

    private companion object {
        private const val JournalpostIdKey = "journalpost_id"
        private const val DokumentIdKey = "dokument_id"
        private val logger: Logger = LoggerFactory.getLogger(JournalpostRoutes::class.java)
    }

    internal object Urls {
        internal const val JournalpostInfo = "/journalpost/{$JournalpostIdKey}"
        internal const val Dokument = "/journalpost/{$JournalpostIdKey}/dokument/{$DokumentIdKey}"
        internal const val HentJournalposter = "/journalpost/hent"
        internal const val SettPåVent = "/journalpost/vent/{$JournalpostIdKey}"
        @Deprecated("Skall fjernes")
        internal const val SkalTilK9sak = "/journalpost/skaltilk9sak"
        internal const val SettBehandlingsAar = "/journalpost/settBehandlingsAar/{$JournalpostIdKey}"
        internal const val LukkJournalpost = "/journalpost/lukk/{$JournalpostIdKey}"
        internal const val KopierJournalpost = "/journalpost/kopier/{$JournalpostIdKey}"
        internal const val JournalførPåGenerellSak = "/journalpost/ferdigstill"
        internal const val Mottak = "/journalpost/mottak"

        // for drift i prod
        internal const val ResettInfoOmJournalpost = "/journalpost/resett/{$JournalpostIdKey}"
        internal const val HentHvaSomHarBlittSendtInn = "/journalpost/hentForDebugg/{$JournalpostIdKey}"
        internal const val LukkJournalposterDebugg = "/journalpost/lukkDebugg"
        internal const val LukkJournalpostDebugg = "/journalpost/lukkDebugg/{$JournalpostIdKey}"
    }

    @Bean
    fun JournalpostRoutes() = SaksbehandlerRoutes(authenticationHandler) {
        GET("/api${Urls.JournalpostInfo}") { request ->
            RequestContext(coroutineContext, request) {
                try {
                    val journalpostId = request.journalpostId()
                    val journalpostInfo = journalpostService.hentJournalpostInfo(
                        journalpostId = journalpostId
                    ) ?: throw IkkeFunnet()

                    val norskIdent = if (journalpostInfo.norskIdent == null && journalpostInfo.aktørId != null) {
                        val pdlResponse = pdlService.identifikatorMedAktørId(journalpostInfo.aktørId)
                        pdlResponse?.identPdl?.data?.hentIdenter?.identer?.first()?.ident
                    } else {
                        journalpostInfo.norskIdent
                    }

                    val punsjJournalpost = journalpostService.hentHvisJournalpostMedId(journalpostId = journalpostId)
                    val punsjInnsendingType = punsjJournalpost?.type?.let { PunsjInnsendingType.fraKode(it) }

                    val kanOpprettesJournalforingsOppgave =
                        (journalpostInfo.journalpostType == SafDtos.JournalpostType.I.name &&
                            journalpostInfo.journalpostStatus == SafDtos.Journalstatus.MOTTATT.name)
                    val erFerdigstiltEllerJournalfoert = (
                        journalpostInfo.journalpostStatus == SafDtos.Journalstatus.FERDIGSTILT.name ||
                            journalpostInfo.journalpostStatus == SafDtos.Journalstatus.JOURNALFOERT.name)

                    val journalpostInfoDto = JournalpostInfoDto(
                        journalpostId = journalpostInfo.journalpostId,
                        norskIdent = norskIdent,
                        dokumenter = journalpostInfo.dokumenter,
                        venter = aksjonspunktService.sjekkOmDenErPåVent(journalpostId = journalpostId),
                        punsjInnsendingType = punsjInnsendingType,
                        kanSendeInn = journalpostService.kanSendeInn(listOf(journalpostId)),
                        erSaksbehandler = pepClient.erSaksbehandler(),
                        erInngående = journalpostInfo.erInngående,
                        gosysoppgaveId = punsjJournalpost?.gosysoppgaveId,
                        kanOpprettesJournalføringsoppgave = kanOpprettesJournalforingsOppgave,
                        journalpostStatus = journalpostInfo.journalpostStatus,
                        erFerdigstilt = erFerdigstiltEllerJournalfoert
                    )

                    utvidJournalpostMedMottattDato(
                        journalpostId = journalpostInfo.journalpostId,
                        mottattDato = journalpostInfo.mottattDato,
                        aktørId = journalpostInfo.aktørId
                    )

                    return@RequestContext ServerResponse
                        .ok()
                        .json()
                        .bodyValueAndAwait(journalpostInfoDto)
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
        }

        POST("/api${Urls.HentJournalposter}") { request ->
            RequestContext(coroutineContext, request) {
                val norskIdent = request.ident()
                val pdlResponse = pdlService.identifikator(norskIdent.norskIdent)
                val aktørId = pdlResponse?.identPdl?.data?.hentIdenter?.identer?.first()?.ident
                    ?: throw IllegalStateException("Fant ikke aktørId i PDL")

                val finnJournalposterPåPerson = journalpostService.finnJournalposterPåPerson(aktørId)

                val journalpostMap =
                    finnJournalposterPåPerson.map { journalpostService.hentJournalpostInfo(it.journalpostId) }
                        .filter { it?.journalpostId != null }
                        .groupBy { it?.journalpostId }

                val dto = finnJournalposterPåPerson.map { it: PunsjJournalpost ->
                    val dok = journalpostMap[it.journalpostId]?.flatMap { post -> post!!.dokumenter }
                        ?.map { OasDokumentInfo(it.dokumentId) }?.toSet()

                    // TODO: But why OpenApi DTOer her????
                    OasJournalpostDto(
                        journalpostId = it.journalpostId,
                        gosysoppgaveId = it.gosysoppgaveId,
                        dokumenter = dok,
                        dato = it.mottattDato?.toLocalDate(),
                        klokkeslett = it.mottattDato?.toLocalTime(),
                        punsjInnsendingType = if (it.type != null) PunsjInnsendingType.fraKode(it.type) else null
                    )
                }.toList()

                ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(OasJournalpostIder(dto))
            }
        }

        POST("/api${Urls.SettPåVent}") { request ->
            RequestContext(coroutineContext, request) {
                val journalpost = request.journalpostId()
                val dto = kotlin.runCatching { request.søknadId() }.getOrDefault(SettPåVentDto(null))
                aksjonspunktService.settPåVentOgSendTilLos(journalpost, dto.soeknadId)

                ServerResponse.ok().buildAndAwait()
            }
        }

        POST("/api${Urls.Mottak}") { request ->
            RequestContext(coroutineContext, request) {
                val norskIdent = request.hentNorskIdentHeader()
                innlogget.harInnloggetBrukerTilgangTilOgSendeInn(
                    norskIdent = norskIdent,
                    url = Urls.Mottak
                )?.let { return@RequestContext it }
                val dto = request.body(BodyExtractors.toMono(JournalpostMottaksHaandteringDto::class.java)).awaitFirst()
                val oppdatertJournalpost = journalpostService.hent(dto.journalpostId).copy(
                    ytelse = dto.fagsakYtelseTypeKode,
                    aktørId = pdlService.aktørIdFor(dto.brukerIdent),
                    behandlingsAar = dto.periode?.fom?.year
                )

                val journalpostErFerdigstiltEllerJournalfoert =
                    journalpostService.hentSafJournalPost(oppdatertJournalpost.journalpostId)?.erFerdigstiltEllerJournalfoert

                // Oppdater og ferdigstill journalpost hvis vi har saksnummer
                if (!journalpostErFerdigstiltEllerJournalfoert!! && dto.saksnummer != null) {
                    journalpostService.oppdaterOgFerdigstillForMottak(dto)
                }

                journalpostService.lagre(punsjJournalpost = oppdatertJournalpost)

                aksjonspunktService.opprettAksjonspunktOgSendTilK9Los(
                    punsjJournalpost = oppdatertJournalpost,
                    aksjonspunkt = Pair(AksjonspunktKode.PUNSJ, AksjonspunktStatus.OPPRETTET),
                    type = oppdatertJournalpost.type,
                    ytelse = dto.fagsakYtelseTypeKode
                )

                ServerResponse.noContent().buildAndAwait()
            }
        }

        POST("/api${Urls.SettBehandlingsAar}") { request ->
            RequestContext(coroutineContext, request) {
                val norskIdent = request.hentNorskIdentHeader()
                innlogget.harInnloggetBrukerTilgangTilOgSendeInn(
                    norskIdent = norskIdent,
                    url = Urls.SettBehandlingsAar
                )?.let { return@RequestContext it }

                val journalpostId = request.journalpostId()
                val behandlingsAar = try {
                    request.body(BodyExtractors.toMono(BehandlingsAarDto::class.java)).awaitFirst().behandlingsAar
                } catch (e: Exception) {
                    val nåVærendeÅr = LocalDate.now().year
                    logger.info("Kunne ikke hente behandlingsår fra request. Setter til nåværende år ($nåVærendeÅr). Feil: {}", e)
                    nåVærendeÅr
                }

                journalpostService.lagreBehandlingsAar(
                    journalpostId = journalpostId,
                    behandlingsAar = behandlingsAar
                )

                return@RequestContext ServerResponse.ok()
                    .json()
                    .bodyValueAndAwait(BehandlingsAarDto(behandlingsAar = behandlingsAar))
            }
        }

        /***
         * Denne er deprecated, allt skal til k9-sak.
         * Kobling til riktig sak skjer med hjelp av /journalpost/settBehandlingsAar/ & /journalpost/mottak
         */
        POST("/api${Urls.SkalTilK9sak}") { request ->
            RequestContext(coroutineContext, request) {
                val norskIdent = request.hentNorskIdentHeader()
                innlogget.harInnloggetBrukerTilgangTilOgSendeInn(
                    norskIdent = norskIdent,
                    url = Urls.SkalTilK9sak
                )?.let { return@RequestContext it }

                val skalTilK9Sak = true // Pr. 20.3.23 skal alt rutes til k9sak.

                return@RequestContext ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(SkalTilInfotrygdSvar(k9sak = skalTilK9Sak))
            }
        }

        POST("/api${Urls.LukkJournalpost}") { request ->
            RequestContext(coroutineContext, request) {
                val journalpostId = request.journalpostId()
                val lukkJournalpostRequest = request.lukkJournalpostRequest()
                val enhet = azureGraphService.hentEnhetForInnloggetBruker().trimIndent().take(4)

                val journalpost: PunsjJournalpost = (journalpostService.hentHvisJournalpostMedId(journalpostId)
                    ?: return@RequestContext ServerResponse
                        .notFound()
                        .buildAndAwait())

                if (ferdigstillGosysoppgaveEnabled) {
                    val gosysoppgaveId = journalpost.gosysoppgaveId
                    if (!gosysoppgaveId.isNullOrBlank()) {
                        val (httpStatus, feil) = gosysService.ferdigstillOppgave(gosysoppgaveId)
                        if (!httpStatus.is2xxSuccessful) {
                            logger.error("Feilet med å ferdigstille gosysoppgave. Grunn: {}", feil)
                            return@RequestContext ServerResponse
                                .status(httpStatus.value())
                                .bodyValueAndAwait(feil!!)
                        }
                    }
                }

                val (status, body) = journalpostService.settTilFerdig(
                    journalpostId = journalpostId,
                    ferdigstillJournalpost = true,
                    enhet = enhet,
                    sak = lukkJournalpostRequest.sak,
                    søkerIdentitetsnummer = lukkJournalpostRequest.norskIdent.somIdentitetsnummer()
                )
                if (!status.is2xxSuccessful) {
                    return@RequestContext ServerResponse.status(status).bodyValueAndAwait(body!!)
                }

                aksjonspunktService.settUtførtPåAltSendLukkOppgaveTilK9Los(journalpostId, false, null)

                logger.info("Journalpost lukkes", keyValue("journalpost_id", journalpostId))

                return@RequestContext ServerResponse
                    .ok()
                    .buildAndAwait()
            }
        }

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

        GET("/api${Urls.Dokument}") { request ->
            RequestContext(coroutineContext, request) {
                try {
                    val dokument = journalpostService.hentDokument(
                        journalpostId = request.journalpostId(),
                        dokumentId = request.dokumentId()
                    )

                    if (dokument == null) {
                        return@RequestContext ServerResponse
                            .notFound()
                            .buildAndAwait()
                    } else {
                        return@RequestContext ServerResponse
                            .ok()
                            .contentType(dokument.contentType)
                            .header(
                                HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=${request.dokumentId()}.${dokument.contentType.subtype}"
                            )
                            .header("Content-Security-Policy", "frame-src;")
                            .bodyValueAndAwait(dokument.dataBuffer)
                    }
                } catch (cause: IkkeTilgang) {
                    return@RequestContext ServerResponse
                        .status(HttpStatus.FORBIDDEN)
                        .buildAndAwait()
                }
            }
        }

        /*
        * Brukes for å journalføre inntektsmelding uten søknad som generell sak
         */
        POST("/api${Urls.JournalførPåGenerellSak}") { request ->
            RequestContext(coroutineContext, request) {
                val identOgJournalpost = request.identOgJournalpost()
                val enhet = azureGraphService.hentEnhetForInnloggetBruker().trimIndent().take(4)
                val journalpostId = identOgJournalpost.journalpostId

                journalpostService.hentHvisJournalpostMedId(journalpostId)
                    ?: return@RequestContext ServerResponse
                        .notFound()
                        .buildAndAwait()

                val (status, body) = journalpostService.settTilFerdig(
                    journalpostId = journalpostId,
                    ferdigstillJournalpost = false,
                    sak = null,
                    søkerIdentitetsnummer = null
                )
                if (!status.is2xxSuccessful) {
                    return@RequestContext ServerResponse.status(status).bodyValueAndAwait(body!!)
                }

                val enhetsKode = enhet.trimIndent()
                val bareTall = Pattern.matches("^[0-9]*$", enhetsKode)
                if (!bareTall) {
                    throw IllegalStateException("Klarte ikke hente riktig enhetkode")
                }

                val resultat = kotlin.runCatching {
                    journalpostService.journalførMotGenerellSak(
                        journalpostId = journalpostId,
                        identitetsnummer = identOgJournalpost.norskIdent.somIdentitetsnummer(),
                        enhetKode = enhetsKode
                    )
                }.fold(
                    onSuccess = {
                        ServerResponse
                            .status(it.first)
                            .bodyValueAndAwait(it.second)
                    },
                    onFailure = {
                        ServerResponse
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .json()
                            .bodyValueAndAwait(OasFeil(it.message))
                    }
                )

                aksjonspunktService.settUtførtPåAltSendLukkOppgaveTilK9Los(
                    journalpostId = journalpostId,
                    erSendtInn = false,
                    ansvarligSaksbehandler = azureGraphService.hentIdentTilInnloggetBruker()
                )

                return@RequestContext resultat
            }
        }

        POST("/api${Urls.KopierJournalpost}") { request ->
            RequestContext(coroutineContext, request) {
                val journalpostId = request.pathVariable("journalpost_id")
                val dto = request.body(BodyExtractors.toMono(KopierJournalpostDto::class.java)).awaitFirst()
                val journalpost = journalpostService.hentHvisJournalpostMedId(journalpostId)
                    ?: return@RequestContext kanIkkeKopieres("Finner ikke journalpost.")

                val identListe = mutableListOf(dto.fra, dto.til)
                dto.barn?.let { identListe.add(it) }
                dto.annenPart?.let { identListe.add(it) }

                if (!pepClient.sendeInnTilgang(identListe, Urls.KopierJournalpost)) {
                    return@RequestContext ServerResponse
                        .status(HttpStatus.FORBIDDEN)
                        .bodyValueAndAwait("Har ikke lov til å kopiere journalpost.")
                }

                val safJournalpost = journalpostService.hentSafJournalPost(journalpostId)
                if (safJournalpost != null && safJournalpost.journalposttype == "U") {
                    return@RequestContext kanIkkeKopieres("Ikke støttet journalposttype: ${safJournalpost.journalposttype}")
                }

                val k9FagsakYtelseType = journalpost?.ytelse?.let {
                    journalpost.utledK9sakFagsakYtelseType(
                        k9sakFagsakYtelseType = no.nav.k9.kodeverk.behandling.FagsakYtelseType.fraKode(
                            it
                        )
                    )
                } ?: return@RequestContext kanIkkeKopieres("Finner ikke ytelse for journalpost.")

                val fagsakYtelseType = FagsakYtelseType.fromKode(journalpost.ytelse)

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
                return@RequestContext ServerResponse
                    .status(HttpStatus.ACCEPTED)
                    .bodyValueAndAwait("Journalposten vil bli kopiert.")
            }
        }
    }

    private fun diffTekst(setA: Set<String>, setB: Set<String>, prefix: String): String {
        val diff = setA.minus(setB)
        return if (diff.isNotEmpty()) "$prefix: $diff" else ""
    }

    private suspend fun utvidJournalpostMedMottattDato(
        journalpostId: String,
        mottattDato: LocalDateTime,
        aktørId: String?,
    ) {
        val journalpostFraBasen = journalpostService.hentHvisJournalpostMedId(journalpostId)
        if (journalpostFraBasen?.mottattDato != null || "KOPI" == journalpostFraBasen?.type) {
            return
        }
        if (journalpostFraBasen != null) {
            val justertDato: LocalDateTime =
                VirkedagerUtil.tilbakeStillToVirkedagerHvisDetKommerFraScanning(journalpostFraBasen.type, mottattDato)
            journalpostService.lagre(journalpostFraBasen.copy(mottattDato = justertDato))
        } else {
            val punsjJournalpost = PunsjJournalpost(
                uuid = UUID.randomUUID(),
                journalpostId = journalpostId,
                aktørId = aktørId,
                mottattDato = mottattDato
            )
            journalpostService.lagre(punsjJournalpost, PunsjJournalpostKildeType.SAKSBEHANDLER)
        }
    }

    private suspend fun kanIkkeKopieres(feil: String) = ServerResponse
        .status(HttpStatus.CONFLICT)
        .bodyValueAndAwait(feil)
        .also { logger.warn("Journalpost kan ikke kopieres: $feil") }

    private fun ServerRequest.journalpostId(): String = pathVariable(JournalpostIdKey)
    private fun ServerRequest.dokumentId(): String = pathVariable(DokumentIdKey)

    internal data class JournalpostIderRequest(val journalpostIder: List<String>)

    private suspend fun ServerRequest.journalpostIder(): JournalpostIderRequest =
        body(BodyExtractors.toMono(JournalpostIderRequest::class.java)).awaitFirst()

    private suspend fun ServerRequest.ident() = body(BodyExtractors.toMono(IdentDto::class.java)).awaitFirst()

    private suspend fun ServerRequest.søknadId() = body(BodyExtractors.toMono(SettPåVentDto::class.java)).awaitFirst()
    private suspend fun ServerRequest.lukkJournalpostRequest() =
        body(BodyExtractors.toMono(LukkJournalpostDto::class.java)).awaitFirst()

    private suspend fun ServerRequest.identOgJournalpost() =
        body(BodyExtractors.toMono(IdentOgJournalpost::class.java)).awaitFirst()
}

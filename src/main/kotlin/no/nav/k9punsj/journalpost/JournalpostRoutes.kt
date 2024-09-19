package no.nav.k9punsj.journalpost

import kotlinx.coroutines.reactive.awaitFirst
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9.sak.typer.Saksnummer
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.felles.IdentOgJournalpost
import no.nav.k9punsj.felles.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.k9punsj.felles.IkkeFunnet
import no.nav.k9punsj.felles.IkkeStøttetJournalpost
import no.nav.k9punsj.felles.IkkeTilgang
import no.nav.k9punsj.felles.JournalpostId.Companion.somJournalpostId
import no.nav.k9punsj.felles.PunsjFagsakYtelseType
import no.nav.k9punsj.fordel.K9FordelType
import no.nav.k9punsj.integrasjoner.dokarkiv.SafDtos
import no.nav.k9punsj.integrasjoner.gosys.GosysService
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import no.nav.k9punsj.integrasjoner.pdl.PdlService
import no.nav.k9punsj.journalpost.dto.BehandlingsAarDto
import no.nav.k9punsj.journalpost.dto.IdentDto
import no.nav.k9punsj.journalpost.dto.JournalpostInfoDto
import no.nav.k9punsj.journalpost.dto.KopierJournalpostDto
import no.nav.k9punsj.journalpost.dto.LukkJournalpostDto
import no.nav.k9punsj.journalpost.dto.PunsjJournalpost
import no.nav.k9punsj.journalpost.dto.PunsjJournalpostKildeType
import no.nav.k9punsj.journalpost.dto.Sak
import no.nav.k9punsj.journalpost.dto.SettPåVentDto
import no.nav.k9punsj.journalpost.dto.utledK9sakFagsakYtelseType
import no.nav.k9punsj.openapi.OasDokumentInfo
import no.nav.k9punsj.openapi.OasFeil
import no.nav.k9punsj.openapi.OasJournalpostDto
import no.nav.k9punsj.openapi.OasJournalpostIder
import no.nav.k9punsj.sak.SakService
import no.nav.k9punsj.sak.dto.SakInfoDto
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
    private val journalpostkopieringService: JournalpostkopieringService,
    private val pdlService: PdlService,
    private val personService: PersonService,
    private val aksjonspunktService: AksjonspunktService,
    private val pepClient: IPepClient,
    private val gosysService: GosysService,
    private val sakService: SakService,
    private val k9SakService: K9SakService,
    private val azureGraphService: IAzureGraphService,
    private val innlogget: InnloggetUtils,
    @Value("\${FERDIGSTILL_GOSYSOPPGAVE_ENABLED:false}") private val ferdigstillGosysoppgaveEnabled: Boolean,
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
        internal const val SettBehandlingsAar = "/journalpost/settBehandlingsAar/{$JournalpostIdKey}"
        internal const val LukkJournalpost = "/journalpost/lukk/{$JournalpostIdKey}"
        internal const val KopierJournalpost = "/journalpost/kopier/{$JournalpostIdKey}"
        internal const val JournalførPåGenerellSak = "/journalpost/ferdigstill"
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
                    val k9FordelType = punsjJournalpost?.type?.let { K9FordelType.fraKode(it) }

                    val safJournalPost = journalpostService.hentSafJournalPost(journalpostId = journalpostId)

                    val kanOpprettesJournalforingsOppgave =
                        (journalpostInfo.journalpostType == SafDtos.JournalpostType.INNGAAENDE.kode &&
                                journalpostInfo.journalpostStatus == SafDtos.Journalstatus.MOTTATT.name)
                    val erFerdigstiltEllerJournalfoert = (
                            journalpostInfo.journalpostStatus == SafDtos.Journalstatus.FERDIGSTILT.name ||
                                    journalpostInfo.journalpostStatus == SafDtos.Journalstatus.JOURNALFOERT.name)

                    val k9PunsjFagsakYtelseType = punsjJournalpost?.ytelse?.let {
                        punsjJournalpost.utledK9sakFagsakYtelseType(
                            k9sakFagsakYtelseType = when (it) {
                                PunsjFagsakYtelseType.UKJENT.kode -> FagsakYtelseType.UDEFINERT
                                else -> FagsakYtelseType.fraKode(it)
                            }
                        )
                    }

                    // Hvis journalposten har sakstilhørighet, hent sak fra K9sak
                    val safSak = safJournalPost?.sak
                    val k9Fagsak = safSak?.let { sak: SafDtos.Sak ->
                        norskIdent?.let { ident: String ->
                            sakService.hentSaker(ident)
                                .filterNot { it.reservert }
                                .firstOrNull { it.fagsakId == sak.fagsakId }
                        }
                    }

                    val utledetSak =
                        utledSak(
                            erFerdigstiltEllerJournalfoert,
                            safSak,
                            k9Fagsak,
                            k9PunsjFagsakYtelseType,
                            punsjJournalpost
                        )
                    logger.info("Utledet sak: $utledetSak")

                    val journalpostInfoDto = JournalpostInfoDto(
                        journalpostId = journalpostInfo.journalpostId,
                        norskIdent = norskIdent,
                        dokumenter = journalpostInfo.dokumenter,
                        venter = aksjonspunktService.sjekkOmDenErPåVent(journalpostId = journalpostId),
                        punsjInnsendingType = k9FordelType,
                        kanSendeInn = journalpostService.kanSendeInn(listOf(journalpostId)),
                        erSaksbehandler = pepClient.erSaksbehandler(),
                        erInngående = journalpostInfo.erInngående,
                        gosysoppgaveId = punsjJournalpost?.gosysoppgaveId,
                        kanOpprettesJournalføringsoppgave = kanOpprettesJournalforingsOppgave,
                        journalpostStatus = journalpostInfo.journalpostStatus,
                        erFerdigstilt = erFerdigstiltEllerJournalfoert,
                        sak = utledetSak
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
                        punsjInnsendingType = if (it.type != null) K9FordelType.fraKode(it.type) else null
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
                    logger.info(
                        "Kunne ikke hente behandlingsår fra request. Setter til nåværende år ($nåVærendeÅr). Feil: {}",
                        e
                    )
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

                val identListe = mutableListOf(dto.fra, dto.til)
                dto.barn?.let { identListe.add(it) }
                dto.annenPart?.let { identListe.add(it) }

                if (!pepClient.sendeInnTilgang(identListe, Urls.KopierJournalpost)) {
                    return@RequestContext ServerResponse
                        .status(HttpStatus.FORBIDDEN)
                        .bodyValueAndAwait("Har ikke lov til å kopiere journalpost.")
                }

                val kopierJournalpostInfo = journalpostkopieringService.kopierJournalpost(
                    journalpostId = journalpostId.somJournalpostId(),
                    kopierJournalpostDto = dto
                )

                return@RequestContext ServerResponse
                    .status(HttpStatus.CREATED)
                    .bodyValueAndAwait(kopierJournalpostInfo)
            }
        }
    }

    private suspend fun utledSak(
        erFerdigstiltEllerJournalfoert: Boolean,
        safSak: SafDtos.Sak?,
        k9Fagsak: SakInfoDto?,
        k9FagsakYtelseType: FagsakYtelseType?,
        punsjJournalpost: PunsjJournalpost?,
    ): Sak {
        logger.info("Utleder sak for journalpost")
        val harSafSak = safSak != null
        val safSakHarFagsakId = safSak?.fagsakId != null
        val ikkeHarFagsak = k9Fagsak == null

        val erReservertSaksnummer = harSafSak && safSakHarFagsakId && ikkeHarFagsak
        logger.info("erReservertSaksnummer: $erReservertSaksnummer. Grunnlag -> harSafSak: $harSafSak, safSakHarFagsakId: $safSakHarFagsakId, harFagsak: $ikkeHarFagsak, erReservertSaksnummer: $erReservertSaksnummer")

        return when (erReservertSaksnummer) {
            true -> {
                logger.info("Utleder reservert sak. Henter reservert saksnummer fra k9-sak med fagsakId: ${safSak!!.fagsakId}")
                val reservertSaksnummerDto = k9SakService.hentReservertSaksnummer(
                    Saksnummer(safSak.fagsakId)
                )
                logger.info("Fant reservert saksnummer: $reservertSaksnummerDto")
                val pleietrengendeIdent = reservertSaksnummerDto?.pleietrengendeAktørId?.let {
                    personService.finnEllerOpprettPersonVedAktørId(it).norskIdent
                }
                val relatertPersonIdent = reservertSaksnummerDto?.relatertPersonAktørId?.let {
                    personService.finnEllerOpprettPersonVedAktørId(it).norskIdent
                }
                val barnIdenter = reservertSaksnummerDto?.barnAktørIder?.let { fosterbarnAktørIder: List<String> ->
                    fosterbarnAktørIder.map { personService.finnEllerOpprettPersonVedAktørId(it) }
                }
                Sak(
                    reservertSaksnummer = true,
                    fagsakId = reservertSaksnummerDto?.saksnummer,
                    gyldigPeriode = null,
                    pleietrengendeIdent = pleietrengendeIdent,
                    relatertPersonIdent = relatertPersonIdent,
                    barnIdenter = barnIdenter,
                    sakstype = reservertSaksnummerDto?.ytelseType?.kode,
                    behandlingsÅr = punsjJournalpost?.behandlingsAar
                )
            }

            else -> {
                logger.info("Utleder fagsak fra k9-sak med fagsakId: ${safSak?.fagsakId}")
                Sak(
                    reservertSaksnummer = false,
                    fagsakId = safSak?.fagsakId,
                    gyldigPeriode = k9Fagsak?.gyldigPeriode,
                    pleietrengendeIdent = k9Fagsak?.pleietrengendeIdent,
                    relatertPersonIdent = k9Fagsak?.relatertPersonIdent,
                    barnIdenter = null,
                    sakstype = k9Fagsak?.sakstype ?: k9FagsakYtelseType?.kode,
                    behandlingsÅr = punsjJournalpost?.behandlingsAar
                )
            }
        }
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

    private fun ServerRequest.journalpostId(): String = pathVariable(JournalpostIdKey)
    private fun ServerRequest.dokumentId(): String = pathVariable(DokumentIdKey)

    internal data class JournalpostIderRequest(val journalpostIder: List<String>)

    private suspend fun ServerRequest.ident() = body(BodyExtractors.toMono(IdentDto::class.java)).awaitFirst()

    private suspend fun ServerRequest.søknadId() = body(BodyExtractors.toMono(SettPåVentDto::class.java)).awaitFirst()
    private suspend fun ServerRequest.lukkJournalpostRequest() =
        body(BodyExtractors.toMono(LukkJournalpostDto::class.java)).awaitFirst()

    private suspend fun ServerRequest.identOgJournalpost() =
        body(BodyExtractors.toMono(IdentOgJournalpost::class.java)).awaitFirst()
}

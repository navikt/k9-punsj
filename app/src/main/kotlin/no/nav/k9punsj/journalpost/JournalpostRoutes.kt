package no.nav.k9punsj.journalpost

import kotlinx.coroutines.reactive.awaitFirst
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.akjonspunkter.AksjonspunktKode
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.akjonspunkter.AksjonspunktStatus
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.IdentDto
import no.nav.k9punsj.felles.IdentOgJournalpost
import no.nav.k9punsj.felles.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.k9punsj.felles.IkkeFunnet
import no.nav.k9punsj.felles.IkkeStøttetJournalpost
import no.nav.k9punsj.felles.IkkeTilgang
import no.nav.k9punsj.felles.PunsjJournalpostKildeType
import no.nav.k9punsj.fordel.PunsjInnsendingType
import no.nav.k9punsj.innsending.InnsendingClient
import no.nav.k9punsj.integrasjoner.gosys.GosysService
import no.nav.k9punsj.integrasjoner.pdl.PdlService
import no.nav.k9punsj.journalpost.dto.JournalpostMottaksHaandteringDto
import no.nav.k9punsj.journalpost.dto.LukkJournalpostDto
import no.nav.k9punsj.journalpost.dto.ResultatDto
import no.nav.k9punsj.journalpost.dto.RutingDto
import no.nav.k9punsj.journalpost.dto.SettPåVentDto
import no.nav.k9punsj.journalpost.dto.SkalTilInfotrygdSvar
import no.nav.k9punsj.openapi.OasDokumentInfo
import no.nav.k9punsj.openapi.OasFeil
import no.nav.k9punsj.openapi.OasJournalpostDto
import no.nav.k9punsj.openapi.OasJournalpostIder
import no.nav.k9punsj.ruting.Destinasjon
import no.nav.k9punsj.ruting.RutingService
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
    private val rutingService: RutingService,
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
        internal const val SkalTilK9sak = "/journalpost/skaltilk9sak"
        internal const val LukkJournalpost = "/journalpost/lukk/{$JournalpostIdKey}"
        internal const val KopierJournalpost = "/journalpost/kopier/{$JournalpostIdKey}"
        internal const val JournalførPåGenerellSak = "/journalpost/ferdigstill"
        internal const val Mottak = "/journalpost/mottak"

        // for drift i prod
        internal const val ResettInfoOmJournalpost = "/journalpost/resett/{$JournalpostIdKey}"
        internal const val HentHvaSomHarBlittSendtInn = "/journalpost/hentForDebugg/{$JournalpostIdKey}"
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

                    val punsjInnsendingType = journalpostService.hentPunsjInnsendingType(journalpostId)
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
                        kanOpprettesJournalføringsoppgave = journalpostInfo.kanOpprettesJournalføringsoppgave,
                        journalpostStatus = journalpostInfo.journalpostStatus
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
                val punsjFagsakYtelseType = FagsakYtelseType.fromKode(dto.fagsakYtelseTypeKode)

                journalpostService.settFagsakYtelseType(punsjFagsakYtelseType, dto.journalpostId)
                val punsjJournalpost = journalpostService.hent(dto.journalpostId)

                aksjonspunktService.opprettAksjonspunktOgSendTilK9Los(
                    punsjJournalpost = punsjJournalpost,
                    aksjonspunkt = Pair(AksjonspunktKode.PUNSJ, AksjonspunktStatus.OPPRETTET),
                    type = punsjJournalpost.type,
                    ytelse = dto.fagsakYtelseTypeKode
                )

                ServerResponse.noContent().buildAndAwait()
            }
        }

        POST("/api${Urls.SkalTilK9sak}") { request ->
            RequestContext(coroutineContext, request) {
                val dto = request.rutingDto()

                val norskIdent = request.hentNorskIdentHeader()
                innlogget.harInnloggetBrukerTilgangTilOgSendeInn(
                    norskIdent = norskIdent,
                    url = Urls.SkalTilK9sak
                )?.let { return@RequestContext it }

                val hentHvisJournalpostMedId = journalpostService.hentHvisJournalpostMedId(dto.journalpostId)
                if (hentHvisJournalpostMedId?.skalTilK9 != null) {
                    val ruting = when (hentHvisJournalpostMedId.skalTilK9) {
                        true -> Destinasjon.K9Sak
                        false -> Destinasjon.Infotrygd
                    }

                    val skalTilK9Sak = (ruting == Destinasjon.K9Sak)

                    return@RequestContext ServerResponse
                        .ok()
                        .json()
                        .bodyValueAndAwait(SkalTilInfotrygdSvar(k9sak = skalTilK9Sak))
                }

                val aktørId = pdlService.aktørIdFor(dto.brukerIdent)?.let { setOf(it) } ?: emptySet()
                val fagsakYtelseType = FagsakYtelseType.fromKode(dto.fagsakYtelseType.kode)

                val destinasjon = try {
                    rutingService.destinasjon(
                        søker = dto.brukerIdent,
                        pleietrengende = dto.pleietrengende,
                        annenPart = dto.annenPart,
                        fraOgMed = LocalDate.now(),
                        aktørIder = aktørId,
                        journalpostIds = setOf(dto.journalpostId),
                        fagsakYtelseType = fagsakYtelseType
                    )
                } catch (e: Exception) {
                    return@RequestContext ServerResponse
                        .badRequest()
                        .bodyValueAndAwait("Feil vid ruting-kall: ${e.localizedMessage}")
                }

                val skalTilK9Sak = (destinasjon == Destinasjon.K9Sak)

                lagreHvorJournalpostSkal(
                    hentHvisPunsjJournalpostMedId = hentHvisJournalpostMedId,
                    dto = dto,
                    skalTilK9 = skalTilK9Sak
                )

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

                aksjonspunktService.settUtførtPåAltSendLukkOppgaveTilK9Los(journalpostId, false, null)

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

        POST("/api${Urls.JournalførPåGenerellSak}") { request ->
            RequestContext(coroutineContext, request) {
                val identOgJournalpost = request.identOgJournalpost()
                val enhet = azureGraphService.hentEnhetForInnloggetBruker().trimIndent().take(4)
                val journalpostId = identOgJournalpost.journalpostId

                journalpostService.hentHvisJournalpostMedId(journalpostId)
                    ?: return@RequestContext ServerResponse
                        .notFound()
                        .buildAndAwait()

                aksjonspunktService.settUtførtPåAltSendLukkOppgaveTilK9Los(
                    journalpostId = journalpostId,
                    erSendtInn = false,
                    ansvarligSaksbehandler = azureGraphService.hentIdentTilInnloggetBruker()
                )

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

                return@RequestContext resultat
            }
        }

        kopierJournalpostRoute(
            pepClient = pepClient,
            journalpostService = journalpostService,
            innsendingClient = innsendingClient,
            rutingService = rutingService,
            pdlService = pdlService
        )
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

    private suspend fun lagreHvorJournalpostSkal(
        hentHvisPunsjJournalpostMedId: PunsjJournalpost?,
        dto: RutingDto,
        skalTilK9: Boolean,
    ) {
        if (hentHvisPunsjJournalpostMedId != null) {
            journalpostService.lagre(hentHvisPunsjJournalpostMedId.copy(skalTilK9 = skalTilK9))
        } else {
            val punsjJournalpost = PunsjJournalpost(
                uuid = UUID.randomUUID(),
                journalpostId = dto.journalpostId,
                aktørId = pdlService.aktørIdFor(dto.brukerIdent),
                skalTilK9 = skalTilK9
            )
            journalpostService.lagre(punsjJournalpost, PunsjJournalpostKildeType.SAKSBEHANDLER)
        }
    }

    private fun ServerRequest.journalpostId(): String = pathVariable(JournalpostIdKey)
    private fun ServerRequest.dokumentId(): String = pathVariable(DokumentIdKey)

    private suspend fun ServerRequest.ident() = body(BodyExtractors.toMono(IdentDto::class.java)).awaitFirst()

    private suspend fun ServerRequest.søknadId() = body(BodyExtractors.toMono(SettPåVentDto::class.java)).awaitFirst()
    private suspend fun ServerRequest.lukkJournalpostRequest() = body(BodyExtractors.toMono(LukkJournalpostDto::class.java)).awaitFirst()

    private suspend fun ServerRequest.rutingDto() =
        body(BodyExtractors.toMono(RutingDto::class.java)).awaitFirst()

    private suspend fun ServerRequest.identOgJournalpost() =
        body(BodyExtractors.toMono(IdentOgJournalpost::class.java)).awaitFirst()

}

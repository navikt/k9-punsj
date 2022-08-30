package no.nav.k9punsj.journalpost

import kotlinx.coroutines.reactive.awaitFirst
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.felles.AktørId.Companion.somAktørId
import no.nav.k9punsj.felles.IdentDto
import no.nav.k9punsj.felles.IdentOgJournalpost
import no.nav.k9punsj.felles.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.k9punsj.felles.IkkeFunnet
import no.nav.k9punsj.felles.IkkeStøttetJournalpost
import no.nav.k9punsj.felles.IkkeTilgang
import no.nav.k9punsj.felles.JournalpostId.Companion.somJournalpostId
import no.nav.k9punsj.felles.PunsjJournalpostKildeType
import no.nav.k9punsj.felles.RutingDto
import no.nav.k9punsj.felles.SettPåVentDto
import no.nav.k9punsj.fordel.PunsjInnsendingType
import no.nav.k9punsj.innsending.InnsendingClient
import no.nav.k9punsj.integrasjoner.pdl.PdlService
import no.nav.k9punsj.openapi.OasDokumentInfo
import no.nav.k9punsj.openapi.OasFeil
import no.nav.k9punsj.openapi.OasJournalpostDto
import no.nav.k9punsj.openapi.OasJournalpostIder
import no.nav.k9punsj.openapi.OasSkalTilInfotrygdSvar
import no.nav.k9punsj.ruting.RutingService
import no.nav.k9punsj.tilgangskontroll.AuthenticationHandler
import no.nav.k9punsj.tilgangskontroll.InnloggetUtils
import no.nav.k9punsj.tilgangskontroll.abac.IPepClient
import no.nav.k9punsj.tilgangskontroll.azuregraph.IAzureGraphService
import no.nav.k9punsj.utils.ServerRequestUtils.hentNorskIdentHeader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.status
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import org.springframework.web.reactive.function.server.json
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
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
    private val azureGraphService: IAzureGraphService,
    private val innlogget: InnloggetUtils,
    private val rutingService: RutingService
) {

    internal companion object {
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
                    val journalpostInfo = journalpostService.hentJournalpostInfo(
                        journalpostId = request.journalpostId()
                    ) ?: throw IkkeFunnet()

                    val norskIdent = if (journalpostInfo.norskIdent == null && journalpostInfo.aktørId != null) {
                        val pdlResponse = pdlService.identifikatorMedAktørId(journalpostInfo.aktørId)
                        pdlResponse?.identPdl?.data?.hentIdenter?.identer?.first()?.ident
                    } else {
                        journalpostInfo.norskIdent
                    }

                    val punsjInnsendingType = journalpostService.hentPunsjInnsendingType(request.journalpostId())
                    val journalpostInfoDto = JournalpostInfoDto(
                        journalpostId = journalpostInfo.journalpostId,
                        norskIdent = norskIdent,
                        dokumenter = journalpostInfo.dokumenter,
                        venter = aksjonspunktService.sjekkOmDenErPåVent(journalpostId = request.journalpostId()),
                        punsjInnsendingType = punsjInnsendingType,
                        kanSendeInn = journalpostService.kanSendeInn(listOf(request.journalpostId())),
                        erSaksbehandler = pepClient.erSaksbehandler(),
                        erInngående = journalpostInfo.erInngående,
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
                    return@RequestContext serverResponseConflict()
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

                val dto = finnJournalposterPåPerson.map { it ->
                    val dok = journalpostMap[it.journalpostId]?.flatMap { post -> post!!.dokumenter }
                        ?.map { OasDokumentInfo(it.dokumentId) }?.toSet()

                    // TODO: But why OpenApi DTOer her????
                    OasJournalpostDto(
                        journalpostId = it.journalpostId,
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
                    val punsjbolleRuting = when (hentHvisJournalpostMedId.skalTilK9) {
                        true -> RutingService.Destinasjon.K9Sak
                        false -> RutingService.Destinasjon.Infotrygd
                    }

                    val skalTilK9Sak = (punsjbolleRuting == RutingService.Destinasjon.K9Sak)

                    return@RequestContext ServerResponse
                        .ok()
                        .json()
                        .bodyValueAndAwait(OasSkalTilInfotrygdSvar(k9sak = skalTilK9Sak))
                }

                val aktørId = pdlService.aktørIdFor(dto.brukerIdent)?.let { setOf(it) } ?: emptySet()

                val destinasjon = try {
                    rutingService.destinasjon(
                        søker = dto.brukerIdent,
                        pleietrengende = dto.pleietrengende,
                        annenPart = dto.annenPart,
                        fraOgMed = LocalDate.now(),
                        aktørIder = aktørId,
                        journalpostIds = setOf(dto.journalpostId),
                        fagsakYtelseType = dto.fagsakYtelseType
                    )
                } catch (e: Exception) {
                    return@RequestContext ServerResponse
                        .badRequest()
                        .bodyValueAndAwait("Feil vid ruting-kall: ${e.localizedMessage}")
                }

                lagreHvorJournalpostSkal(
                    hentHvisPunsjJournalpostMedId = hentHvisJournalpostMedId,
                    dto = dto,
                    skalTilK9 = (destinasjon == RutingService.Destinasjon.K9Sak)
                )

                val skalTilK9Sak = (destinasjon == RutingService.Destinasjon.K9Sak)
                return@RequestContext ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(OasSkalTilInfotrygdSvar(k9sak = skalTilK9Sak))
            }
        }

        POST("/api${Urls.LukkJournalpost}") { request ->
            RequestContext(coroutineContext, request) {
                val journalpostId = request.journalpostId()

                journalpostService.hentHvisJournalpostMedId(journalpostId)
                    ?: return@RequestContext ServerResponse
                        .notFound()
                        .buildAndAwait()

                aksjonspunktService.settUtførtPåAltSendLukkOppgaveTilK9Los(journalpostId, false, null)
                journalpostService.settTilFerdig(journalpostId)

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

                journalpostService.hentHvisJournalpostMedId(journalpostId)
                    ?: return@RequestContext ServerResponse
                        .notFound()
                        .buildAndAwait()

                val kanSendeInn = journalpostService.kanSendeInn(listOf(journalpostId))
                if (kanSendeInn) {
                    aksjonspunktService.settUtførtPåAltSendLukkOppgaveTilK9Los(journalpostId, false, null)
                    journalpostService.settTilFerdig(journalpostId)
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
                        return@RequestContext serverResponseConflict()
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
                val enhet = azureGraphService.hentEnhetForInnloggetBruker()
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

                journalpostService.settTilFerdig(journalpostId)

                val enhetsKode = enhet.trimIndent().substring(0, 4)
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
                        ServerResponse.status(it).buildAndAwait()
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
        aktørId: String?
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
        skalTilK9: Boolean
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

    private suspend fun ServerRequest.rutingDto() =
        body(BodyExtractors.toMono(RutingDto::class.java)).awaitFirst()

    private suspend fun ServerRequest.identOgJournalpost() =
        body(BodyExtractors.toMono(IdentOgJournalpost::class.java)).awaitFirst()

    private suspend fun serverResponseConflict() =
        status(HttpStatus.CONFLICT).json().bodyValueAndAwait("""{"type":"punsj://ikke-støttet-journalpost"}""")

    private data class ResultatDto(
        val status: String
    )
}

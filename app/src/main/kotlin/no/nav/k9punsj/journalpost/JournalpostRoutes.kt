package no.nav.k9punsj.journalpost

import com.fasterxml.jackson.module.kotlin.convertValue
import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9.søknad.Søknad
import no.nav.k9punsj.*
import no.nav.k9punsj.abac.IPepClient
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.db.datamodell.AktørId
import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import no.nav.k9punsj.fordel.PunsjInnsendingType
import no.nav.k9punsj.innsending.InnsendingClient
import no.nav.k9punsj.rest.eksternt.pdl.PdlService
import no.nav.k9punsj.rest.eksternt.punsjbollen.PunsjbolleService
import no.nav.k9punsj.rest.web.JournalpostId
import no.nav.k9punsj.rest.web.PunsjBolleDto
import no.nav.k9punsj.rest.web.SettPåVentDto
import no.nav.k9punsj.rest.web.dto.IdentDto
import no.nav.k9punsj.rest.web.dto.NorskIdentDto
import no.nav.k9punsj.rest.web.openapi.OasDokumentInfo
import no.nav.k9punsj.rest.web.openapi.OasJournalpostDto
import no.nav.k9punsj.rest.web.openapi.OasJournalpostIder
import no.nav.k9punsj.rest.web.openapi.OasSkalTilInfotrygdSvar
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.*
import java.time.LocalDateTime
import java.util.UUID
import kotlin.coroutines.coroutineContext

@Configuration
internal class JournalpostRoutes(
    private val authenticationHandler: AuthenticationHandler,
    private val journalpostService: JournalpostService,
    private val pdlService: PdlService,
    private val aksjonspunktService: AksjonspunktService,
    private val pepClient: IPepClient,
    private val punsjbolleService: PunsjbolleService,
    private val innsendingClient: InnsendingClient,
) {

    private companion object {
        private const val JournalpostIdKey = "journalpost_id"
        private const val DokumentIdKey = "dokument_id"
    }

    internal object Urls {
        internal const val JournalpostInfo = "/journalpost/{$JournalpostIdKey}"
        internal const val OmfordelJournalpost = "$JournalpostInfo/omfordel"
        internal const val Dokument = "/journalpost/{$JournalpostIdKey}/dokument/{$DokumentIdKey}"
        internal const val HentJournalposter = "/journalpost/hent"
        internal const val SettPåVent = "/journalpost/vent/{$JournalpostIdKey}"
        internal const val SkalTilK9sak = "/journalpost/skaltilk9sak"
        internal const val LukkJournalpost = "/journalpost/lukk/{$JournalpostIdKey}"
        internal const val KopierJournalpost = "/journalpost/kopier/{$JournalpostIdKey}"

        //for drift i prod
        internal const val ResettInfoOmJournalpost = "/journalpost/resett/{$JournalpostIdKey}"
        internal const val HentHvaSomHarBlittSendtInn = "/journalpost/hentForDebugg/{$JournalpostIdKey}"
    }

    @Bean
    fun JournalpostRoutes() = Routes(authenticationHandler) {

        GET("/api${Urls.JournalpostInfo}") { request ->
            RequestContext(coroutineContext, request) {
                try {
                    val journalpostInfo = journalpostService.hentJournalpostInfo(
                        journalpostId = request.journalpostId()
                    )
                    if (journalpostInfo == null) {
                        return@RequestContext ServerResponse
                            .notFound()
                            .buildAndAwait()
                    } else {
                        if (journalpostInfo.norskIdent == null && journalpostInfo.aktørId != null) {
                            val pdlResponse = pdlService.identifikatorMedAktørId(journalpostInfo.aktørId)
                            val personIdent = pdlResponse?.identPdl?.data?.hentIdenter?.identer?.first()?.ident
                            val punsjInnsendingType =
                                journalpostService.hentHvisJournalpostMedId(journalpostId = request.journalpostId())?.type
                            val journalpostInfoDto = JournalpostInfoDto(
                                journalpostId = journalpostInfo.journalpostId,
                                norskIdent = personIdent,
                                dokumenter = journalpostInfo.dokumenter,
                                kanSendeInn = journalpostService.kanSendeInn(request.journalpostId()),
                                venter = aksjonspunktService.sjekkOmDenErPåVent(journalpostId = request.journalpostId()),
                                punsjInnsendingType = if (punsjInnsendingType != null) PunsjInnsendingType.fraKode(
                                    punsjInnsendingType) else null,
                                erSaksbehandler = pepClient.erSaksbehandler(),
                                erInngående = journalpostInfo.erInngående
                            )
                            utvidJournalpostMedMottattDato(journalpostInfo.journalpostId,
                                journalpostInfo.mottattDato,
                                journalpostInfo.aktørId)
                            return@RequestContext ServerResponse
                                .ok()
                                .json()
                                .bodyValueAndAwait(journalpostInfoDto)
                        } else {
                            val punsjInnsendingType =
                                journalpostService.hentHvisJournalpostMedId(journalpostId = request.journalpostId())?.type

                            utvidJournalpostMedMottattDato(journalpostInfo.journalpostId,
                                journalpostInfo.mottattDato,
                                journalpostInfo.aktørId)
                            return@RequestContext ServerResponse
                                .ok()
                                .json()
                                .bodyValueAndAwait(JournalpostInfoDto(
                                    journalpostId = journalpostInfo.journalpostId,
                                    norskIdent = journalpostInfo.norskIdent,
                                    dokumenter = journalpostInfo.dokumenter,
                                    venter = aksjonspunktService.sjekkOmDenErPåVent(journalpostId = request.journalpostId()),
                                    punsjInnsendingType = if (punsjInnsendingType != null) PunsjInnsendingType.fraKode(
                                        punsjInnsendingType) else null,
                                    kanSendeInn = journalpostService.kanSendeInn(request.journalpostId()),
                                    erSaksbehandler = pepClient.erSaksbehandler(),
                                    erInngående = journalpostInfo.erInngående
                                ))
                        }
                    }

                } catch (cause: IkkeStøttetJournalpost) {
                    return@RequestContext ServerResponse
                        .badRequest()
                        .buildAndAwait()
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

                    OasJournalpostDto(
                        journalpostId = it.journalpostId,
                        dokumenter = dok,
                        it.mottattDato?.toLocalDate(),
                        it.mottattDato?.toLocalTime(),
                        if (it.type != null) PunsjInnsendingType.fraKode(it.type) else null)
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
                val dto = request.søknadId()

                aksjonspunktService.settPåVentOgSendTilLos(journalpost, dto.soeknadId)

                ServerResponse
                    .ok()
                    .buildAndAwait()
            }
        }

        POST("/api${Urls.SkalTilK9sak}") { request ->
            RequestContext(coroutineContext, request) {
                val dto = request.punsjbolleDto()
                harInnloggetBrukerTilgangTilOgOpprettesak(dto.brukerIdent,
                    Urls.SkalTilK9sak)?.let { return@RequestContext it }

                val hentHvisJournalpostMedId = journalpostService.hentHvisJournalpostMedId(dto.journalpostId)

                if (hentHvisJournalpostMedId?.skalTilK9 != null) {
                    return@RequestContext ServerResponse
                        .ok()
                        .json()
                        .bodyValueAndAwait(OasSkalTilInfotrygdSvar(hentHvisJournalpostMedId.skalTilK9))
                }

                val skalTilK9 = punsjbolleService.kanRutesTilK9Sak(
                    søker = dto.brukerIdent,
                    barn = dto.barnIdent,
                    journalpostId = dto.journalpostId,
                    periode = null, // Utledes fra journalposten i Punsjbollen
                    correlationId = coroutineContext.hentCorrelationId()
                )

                lagreHvorJournalpostSkal(hentHvisJournalpostMedId, dto, skalTilK9)

                return@RequestContext ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(OasSkalTilInfotrygdSvar(skalTilK9))
            }
        }

        POST("/api${Urls.LukkJournalpost}") { request ->
            RequestContext(coroutineContext, request) {
                val journalpostId = request.journalpostId()
                val hentHvisJournalpostMedId = journalpostService.hentHvisJournalpostMedId(journalpostId)

                if (hentHvisJournalpostMedId == null) {
                    return@RequestContext ServerResponse
                        .notFound()
                        .buildAndAwait()
                }
                aksjonspunktService.settUtførtPåAltSendLukkOppgaveTilK9Los(journalpostId, false)
                journalpostService.settTilFerdig(journalpostId)

                return@RequestContext ServerResponse
                    .ok()
                    .buildAndAwait()
            }
        }

        POST("/api${Urls.OmfordelJournalpost}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val omfordelingRequest = request.omfordelingRequest()
                try {
                    val journalpostInfo = journalpostService.hentJournalpostInfo(
                        journalpostId = request.journalpostId()
                    )
                    if (journalpostInfo == null) {
                        ServerResponse
                            .notFound()
                            .buildAndAwait()
                    } else {
                        journalpostService.omfordelJournalpost(
                            journalpostId = request.journalpostId(),
                            ytelse = FagsakYtelseType.fromKode(omfordelingRequest.fagsakYtelseTypeKode)
                        )
                        ServerResponse
                            .noContent()
                            .buildAndAwait()
                    }

                } catch (cause: IkkeStøttetJournalpost) {
                    ServerResponse
                        .badRequest()
                        .buildAndAwait()
                } catch (case: IkkeTilgang) {
                    ServerResponse
                        .status(HttpStatus.FORBIDDEN)
                        .buildAndAwait()
                }
            }
        }

        POST("/api${Urls.ResettInfoOmJournalpost}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val journalpostId = request.journalpostId()

                val journalpost = journalpostService.hentHvisJournalpostMedId(journalpostId)
                    ?: return@RequestContext ServerResponse
                        .notFound()
                        .buildAndAwait()
                val kanSendeInn = journalpostService.kanSendeInn(journalpostId)

                if (kanSendeInn) {
                    val nyVerdi = journalpost.copy(skalTilK9 = null)
                    journalpostService.lagre(nyVerdi)

                    return@RequestContext ServerResponse
                        .ok()
                        .buildAndAwait()
                }
                return@RequestContext ServerResponse
                    .badRequest()
                    .bodyValueAndAwait("Kan ikke endre på en journalpost som har blitt sendt fra punsj")
            }
        }

        GET("/api${Urls.HentHvaSomHarBlittSendtInn}") { request ->
            RequestContext(coroutineContext, request) {
                val journalpostId = request.journalpostId()
                val journalpost = journalpostService.hentHvisJournalpostMedId(journalpostId)
                    ?: return@RequestContext ServerResponse
                        .notFound()
                        .buildAndAwait()

                if(journalpost.payload != null) {
                    val body: Søknad = objectMapper().convertValue(journalpost.payload)
                    harBasisTilgang(body.søker.personIdent.verdi, Urls.HentHvaSomHarBlittSendtInn)?.let { return@RequestContext it }
                    return@RequestContext ServerResponse
                        .status(HttpStatus.OK)
                        .json()
                        .bodyValueAndAwait(journalpost.payload)
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
                        ServerResponse
                            .notFound()
                            .buildAndAwait()
                    } else {
                        ServerResponse
                            .ok()
                            .contentType(dokument.contentType)
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=${request.dokumentId()}.${dokument.contentType.subtype}")
                            .header("Content-Security-Policy", "frame-src;")
                            .bodyValueAndAwait(dokument.dataBuffer)
                    }
                } catch (cause: IkkeTilgang) {
                    ServerResponse
                        .status(HttpStatus.FORBIDDEN)
                        .buildAndAwait()
                }

            }
        }

        kopierJournalpostRoute(
            pepClient = pepClient,
            punsjbolleService = punsjbolleService,
            journalpostService = journalpostService,
            innsendingClient = innsendingClient
        )
    }

    private suspend fun utvidJournalpostMedMottattDato(
        jornalpostId: JournalpostId,
        mottattDato: LocalDateTime,
        aktørId: AktørId?,
    ) {
        val journalpostFraBasen = journalpostService.hentHvisJournalpostMedId(jornalpostId)
        if (journalpostFraBasen?.mottattDato != null) {
            return
        }
        if (journalpostFraBasen != null) {
            val justertDato: LocalDateTime =
                VirkedagerUtil.tilbakeStillToVirkedagerHvisDetKommerFraScanning(journalpostFraBasen.type, mottattDato)
            journalpostService.lagre(journalpostFraBasen.copy(mottattDato = justertDato))
        } else {
            val journalpost = Journalpost(
                uuid = UUID.randomUUID(),
                journalpostId = jornalpostId,
                aktørId,
                mottattDato = mottattDato
            )
            journalpostService.lagre(journalpost, KildeType.SAKSBEHANDLER)
        }
    }

    private suspend fun lagreHvorJournalpostSkal(
        hentHvisJournalpostMedId: Journalpost?,
        dto: PunsjBolleDto,
        skalTilK9: Boolean,
    ) {
        if (hentHvisJournalpostMedId != null) {
            journalpostService.lagre(hentHvisJournalpostMedId.copy(skalTilK9 = skalTilK9))
        } else {
            val journalpost = Journalpost(
                uuid = UUID.randomUUID(),
                journalpostId = dto.journalpostId,
                pdlService.aktørIdFor(dto.brukerIdent),
                skalTilK9 = skalTilK9
            )
            journalpostService.lagre(journalpost, KildeType.SAKSBEHANDLER)
        }
    }

    private suspend fun harInnloggetBrukerTilgangTilOgOpprettesak(
        norskIdentDto: NorskIdentDto,
        url: String,
    ): ServerResponse? {
        val saksbehandlerHarTilgang = pepClient.sendeInnTilgang(norskIdentDto, url)
        if (!saksbehandlerHarTilgang) {
            return ServerResponse
                .status(HttpStatus.FORBIDDEN)
                .json()
                .bodyValueAndAwait("Du har ikke lov til og sende på denne personen")
        }
        return null
    }

    private suspend fun harBasisTilgang(
        norskIdentDto: NorskIdentDto,
        url: String,
    ): ServerResponse? {
        val saksbehandlerHarTilgang = pepClient.harBasisTilgang(norskIdentDto, url)
        if (!saksbehandlerHarTilgang) {
            return ServerResponse
                .status(HttpStatus.FORBIDDEN)
                .json()
                .bodyValueAndAwait("Du har ikke lov til og sende på denne personen")
        }
        return null
    }


    private suspend fun ServerRequest.journalpostId(): JournalpostId = pathVariable(JournalpostIdKey)
    private suspend fun ServerRequest.dokumentId(): DokumentId = pathVariable(DokumentIdKey)
    private suspend fun ServerRequest.omfordelingRequest() =
        body(BodyExtractors.toMono(OmfordelingRequest::class.java)).awaitFirst()

    private suspend fun ServerRequest.ident() = body(BodyExtractors.toMono(IdentDto::class.java)).awaitFirst()

    private suspend fun ServerRequest.søknadId() = body(BodyExtractors.toMono(SettPåVentDto::class.java)).awaitFirst()

    private suspend fun ServerRequest.punsjbolleDto() =
        body(BodyExtractors.toMono(PunsjBolleDto::class.java)).awaitFirst()

    data class OmfordelingRequest(
        val fagsakYtelseTypeKode: String,
    )
}

package no.nav.k9punsj.journalpost

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9punsj.AuthenticationHandler
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.Routes
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import no.nav.k9punsj.rest.eksternt.pdl.PdlService
import no.nav.k9punsj.rest.eksternt.punsjbollen.PunsjbolleService
import no.nav.k9punsj.rest.web.JournalpostId
import no.nav.k9punsj.rest.web.PunsjBolleDto
import no.nav.k9punsj.rest.web.dto.IdentDto
import no.nav.k9punsj.rest.web.openapi.OasDokumentInfo
import no.nav.k9punsj.rest.web.openapi.OasJournalpostDto
import no.nav.k9punsj.rest.web.openapi.OasJournalpostIder
import no.nav.k9punsj.rest.web.openapi.OasSkalTilInfotrygdSvar
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.*
import java.util.UUID
import kotlin.coroutines.coroutineContext


@Configuration
internal class JournalpostRoutes(
    private val authenticationHandler: AuthenticationHandler,
    private val journalpostService: JournalpostService,
    private val pdlService: PdlService,
    private val aksjonspunktService: AksjonspunktService,
    private val punsjbolleService: PunsjbolleService
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(JournalpostRoutes::class.java)
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
                        ServerResponse
                            .notFound()
                            .buildAndAwait()
                    } else {
                        if (journalpostInfo.norskIdent == null && journalpostInfo.aktørId != null) {
                            val pdlResponse = pdlService.identifikatorMedAktørId(journalpostInfo.aktørId)
                            val personIdent = pdlResponse?.identPdl?.data?.hentIdenter?.identer?.first()?.ident

                            val journalpostInfoDto = JournalpostInfoDto(
                                journalpostId = journalpostInfo.journalpostId,
                                norskIdent = personIdent,
                                dokumenter = journalpostInfo.dokumenter,
                                aksjonspunktService.sjekkOmDenErPåVent(journalpostId = request.journalpostId())
                            )
                            ServerResponse
                                .ok()
                                .json()
                                .bodyValueAndAwait(journalpostInfoDto)
                        } else {
                            ServerResponse
                                .ok()
                                .json()
                                .bodyValueAndAwait(JournalpostInfoDto(journalpostId = journalpostInfo.journalpostId,
                                    norskIdent = journalpostInfo.norskIdent,
                                    dokumenter = journalpostInfo.dokumenter,
                                    aksjonspunktService.sjekkOmDenErPåVent(journalpostId = request.journalpostId())))
                        }
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

        POST("/api${Urls.HentJournalposter}") { request ->
            RequestContext(coroutineContext, request) {
                val norskIdent = request.ident()
                val pdlResponse = pdlService.identifikator(norskIdent.norskIdent)
                val aktørId = pdlResponse?.identPdl?.data?.hentIdenter?.identer?.first()?.ident
                    ?: throw IllegalStateException("Fant ikke aktørId i PDL")

                val finnJournalposterPåPerson = journalpostService.finnJournalposterPåPerson(aktørId)

                val journalpostMap = finnJournalposterPåPerson.map { journalpostService.hentJournalpostInfo(it.journalpostId) }
                    .filter { it?.journalpostId != null }
                    .groupBy { it?.journalpostId }

                val dto = finnJournalposterPåPerson.map { it ->
                    val dok = journalpostMap[it.journalpostId]?.flatMap { post -> post!!.dokumenter }
                        ?.map { OasDokumentInfo(it.dokumentId) }?.toSet()

                    OasJournalpostDto(it.journalpostId, dok, it.dato)
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
                //TODO burde flagge søknad også? indikere at den også venter?
                aksjonspunktService.settPåVentOgSendTilLos(journalpost)

                ServerResponse
                    .ok()
                    .buildAndAwait()
            }
        }

        POST("/api${Urls.SkalTilK9sak}") { request ->
            RequestContext(coroutineContext, request) {
                val dto = request.punsjbolleDto()
                val hentHvisJournalpostMedId = journalpostService.hentHvisJournalpostMedId(dto.journalpostId)

                if (hentHvisJournalpostMedId?.skalTilK9 != null) {
                   return@RequestContext ServerResponse
                        .ok()
                        .buildAndAwait()
                }

                val saksnummerDto =
                    punsjbolleService.opprettEllerHentFagsaksnummer(søker = dto.brukerIdent,
                        barn = dto.barnIdent,
                        journalpostIdDto = dto.journalpostId)

                // skal til infotrygd
                }

                lagreHvorJournalpostSkal(hentHvisJournalpostMedId, dto, tsaksnummerDto == null)

                return@RequestContext ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(OasSkalTilInfotrygdSvar(saksnummerDto != null))
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

    private suspend fun ServerRequest.journalpostId(): JournalpostId = pathVariable(JournalpostIdKey)
    private suspend fun ServerRequest.dokumentId(): DokumentId = pathVariable(DokumentIdKey)
    private suspend fun ServerRequest.omfordelingRequest() =
        body(BodyExtractors.toMono(OmfordelingRequest::class.java)).awaitFirst()

    private suspend fun ServerRequest.ident() = body(BodyExtractors.toMono(IdentDto::class.java)).awaitFirst()

    private suspend fun ServerRequest.punsjbolleDto() = body(BodyExtractors.toMono(PunsjBolleDto::class.java)).awaitFirst()

    data class OmfordelingRequest(
        val fagsakYtelseTypeKode: String,
    )
}

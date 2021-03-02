package no.nav.k9punsj.rest.web.ruter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9punsj.AuthenticationHandler
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.Routes
import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import no.nav.k9punsj.db.datamodell.FagsakYtelseTypeUri
import no.nav.k9punsj.db.datamodell.Periode
import no.nav.k9punsj.domenetjenester.MappeService
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.domenetjenester.PleiepengerSyktBarnSoknadService
import no.nav.k9punsj.domenetjenester.mappers.SøknadMapper
import no.nav.k9punsj.hentAuthentication
import no.nav.k9punsj.rest.eksternt.k9sak.K9SakService
import no.nav.k9punsj.rest.info.ITokenService
import no.nav.k9punsj.rest.web.HentSøknad
import no.nav.k9punsj.rest.web.Innsending
import no.nav.k9punsj.rest.web.OpprettNySøknad
import no.nav.k9punsj.rest.web.SendSøknad
import no.nav.k9punsj.rest.web.dto.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.*
import kotlin.coroutines.coroutineContext

@Configuration
internal class PleiepengerSyktBarnRoutes(
    private val objectMapper: ObjectMapper,
    private val mappeService: MappeService,
    private val pleiepengerSyktBarnSoknadService: PleiepengerSyktBarnSoknadService,
    private val personService: PersonService,
    private val k9SakService: K9SakService,
    private val authenticationHandler: AuthenticationHandler,
    private val tokenService: ITokenService

    ) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(PleiepengerSyktBarnRoutes::class.java)

        private const val søknadType = FagsakYtelseTypeUri.PLEIEPENGER_SYKT_BARN
        private const val SøknadIdKey = "soeknad_id"
    }

    internal object Urls {
        internal const val HenteMappe = "/$søknadType/mappe" //get
        internal const val HenteSøknad = "/$søknadType/mappe/{$SøknadIdKey}" //get
        internal const val NySøknad = "/$søknadType" //post
        internal const val OppdaterEksisterendeSøknad = "/$søknadType/oppdater" //put
        internal const val SendEksisterendeSøknad = "/$søknadType/send" //post
        internal const val HentSøknadFraK9Sak = "/k9-sak/$søknadType" //post
    }

    @Bean
    fun pleiepengerSyktBarnSøknadRoutes() = Routes(authenticationHandler) {
        GET("/api${Urls.HenteMappe}") { request ->
            RequestContext(coroutineContext, request) {
                val norskIdent = request.norskeIdent()
                val person = personService.finnPersonVedNorskIdent(norskIdent)

                if (person != null) {
                    val svarDto = mappeService.hentMappe(
                        person = person,
                        søknadType = FagsakYtelseType.PLEIEPENGER_SYKT_BARN
                    ).tilDto<PleiepengerSøknadVisningDto>(FagsakYtelseType.PLEIEPENGER_SYKT_BARN) {
                        norskIdent
                    }
                    return@RequestContext ServerResponse
                        .ok()
                        .json()
                        .bodyValueAndAwait(svarDto)
                }
                return@RequestContext ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(SvarDto<PleiepengerSøknadVisningDto>(norskIdent, FagsakYtelseType.PLEIEPENGER_SYKT_BARN.kode, listOf()))
            }
        }

        GET("/api${Urls.HenteSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                val søknadId = request.søknadId()
                val søknad = mappeService.hentSøknad(søknadId)

                if (søknad != null) {
                    val person = personService.finnPerson(søknad.søkerId)
                    return@RequestContext ServerResponse
                        .ok()
                        .json()
                        .bodyValueAndAwait(søknad.tilDto<PleiepengerSøknadVisningDto> { person.norskIdent })
                }
                return@RequestContext ServerResponse
                    .notFound()
                    .buildAndAwait()
            }
        }

        PUT("/api${Urls.OppdaterEksisterendeSøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val innsending = request.innsending()

                val accessToken = coroutineContext.hentAuthentication().accessToken
                val saksbehandler = tokenService.decodeToken(accessToken).getUsername()

                val søknadEntitet = mappeService.utfyllendeInnsending(
                    innsending = innsending,
                    saksbehandler = saksbehandler
                )

                if (søknadEntitet == null) {
                    ServerResponse
                        .badRequest()
                        .buildAndAwait()
                } else {
                    val søknadOppdaterDto = SøknadOppdaterDto(
                        innsending.norskIdent,
                        søknadEntitet.first.søknadId,
                        søknad = søknadEntitet.second
                    )
                    ServerResponse
                        .ok()
                        .json()
                        .bodyValueAndAwait(søknadOppdaterDto)
                }
            }
        }

        POST("/api${Urls.SendEksisterendeSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                val sendSøknad = request.sendSøknad()
                val søknadEntitet = mappeService.hentSøknad(sendSøknad.soeknadId)

                if (søknadEntitet == null) {
                    return@RequestContext ServerResponse
                        .badRequest()
                        .buildAndAwait()
                } else {
                    try {
                        val søknad: PleiepengerSøknadVisningDto = objectMapper.convertValue(søknadEntitet.søknad!!)
                        val format = SøknadMapper.mapTilMapFormat(søknad)
                        val søknadK9Format = SøknadMapper.mapTilEksternFormat(format)
                        if (søknadK9Format.second.isNotEmpty()) {
                            val feil = søknadK9Format.second.map { feil ->
                                SøknadFeil.SøknadFeilDto(feil.felt,
                                    feil.feilkode,
                                    feil.feilmelding)
                            }.toList()

                            return@RequestContext ServerResponse
                                .status(HttpStatus.BAD_REQUEST)
                                .json()
                                .bodyValueAndAwait(SøknadFeil(sendSøknad.soeknadId, feil))
                        }

                        val journalposterDto: JournalposterDto =
                            objectMapper.convertValue(søknadEntitet.journalposter!!)
                        pleiepengerSyktBarnSoknadService.sendSøknad(søknadK9Format.first,
                            journalposterDto.journalposter)

                        //TODO(OJR) marker søknad som sendt_inn = TRUE og journalposter som behandlet?
//                        mappeService.fjern(
//                            mappeId = mappeId,
//                            norskIdent = norskIdent
//                        )
                        return@RequestContext ServerResponse
                            .accepted()
                            .buildAndAwait()
                    } catch (e: Exception) {
                        logger.error("", e)
                        return@RequestContext ServerResponse
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .buildAndAwait()
                    }
                }
            }
        }

        POST("/api${Urls.NySøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val opprettNySøknad = request.opprettNy()
                val søknadEntitet = mappeService.førsteInnsending(
                    nySøknad = opprettNySøknad!!,
                    søknadType = FagsakYtelseType.PLEIEPENGER_SYKT_BARN
                )
                val søknadDto = søknadEntitet.tilDto<PleiepengerSøknadVisningDto> {
                    opprettNySøknad.norskIdent
                }
                return@RequestContext ServerResponse
                    .created(request.søknadLocation(søknadDto.søknadId))
                    .json()
                    .bodyValueAndAwait(søknadDto)
            }
        }

        POST("/api${Urls.HentSøknadFraK9Sak}") { request ->
            RequestContext(coroutineContext, request) {
                val hentSøknad = request.hentSøknad()
                val psbUtfyltFraK9 = k9SakService.hentSisteMottattePsbSøknad(hentSøknad.norskIdent,
                    Periode(hentSøknad.periode.fom, hentSøknad.periode.tom))
                    ?: return@RequestContext ServerResponse.notFound().buildAndAwait()

                val søknadIdDto =
                    mappeService.opprettTomSøknad(hentSøknad.norskIdent, FagsakYtelseType.PLEIEPENGER_SYKT_BARN)

                val mottatDto = objectMapper.convertValue<PleiepengerSøknadMottakDto>(psbUtfyltFraK9)

                val mapTilVisningFormat = SøknadMapper.mapTilVisningFormat(mottatDto)

                val søknadDto = SøknadDto(
                    søknadId = søknadIdDto,
                    søkerId = hentSøknad.norskIdent,
                    journalposter = null,
                    erFraK9 = true,
                    søknad = mapTilVisningFormat
                )

                val svarDto =
                    SvarDto(hentSøknad.norskIdent, FagsakYtelseType.PLEIEPENGER_SYKT_BARN.kode, listOf(søknadDto))

                return@RequestContext ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(svarDto)
            }
        }
    }

    private suspend fun ServerRequest.norskeIdent(): String {
        return headers().header("X-Nav-NorskIdent").first()!!
    }

    private suspend fun ServerRequest.innsending() = body(BodyExtractors.toMono(Innsending::class.java)).awaitFirst()
    private suspend fun ServerRequest.opprettNy() = body(BodyExtractors.toMono(OpprettNySøknad::class.java)).awaitFirst()
    private suspend fun ServerRequest.hentSøknad() = body(BodyExtractors.toMono(HentSøknad::class.java)).awaitFirst()
    private suspend fun ServerRequest.sendSøknad() = body(BodyExtractors.toMono(SendSøknad::class.java)).awaitFirst()

    private fun ServerRequest.søknadLocation(søknadId: SøknadIdDto) = uriBuilder().pathSegment("mappe", søknadId).build()

    private suspend fun ServerRequest.søknadId(): SøknadIdDto = pathVariable(SøknadIdKey)
}

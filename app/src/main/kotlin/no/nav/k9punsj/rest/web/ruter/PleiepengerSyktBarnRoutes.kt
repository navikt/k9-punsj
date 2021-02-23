package no.nav.k9punsj.rest.web.ruter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9.søknad.ValideringsFeil
import no.nav.k9punsj.AuthenticationHandler
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.Routes
import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import no.nav.k9punsj.db.datamodell.FagsakYtelseTypeUri
import no.nav.k9punsj.db.datamodell.MappeId
import no.nav.k9punsj.db.datamodell.NorskIdent
import no.nav.k9punsj.domenetjenester.MappeService
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.domenetjenester.PleiepengerSyktBarnSoknadService
import no.nav.k9punsj.domenetjenester.mappers.SøknadMapper
import no.nav.k9punsj.rest.eksternt.k9sak.K9SakService
import no.nav.k9punsj.rest.web.HentSøknad
import no.nav.k9punsj.rest.web.Innsending
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

    ) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(PleiepengerSyktBarnRoutes::class.java)
        private const val NorskIdentKey = "norsk_ident"
        private const val MappeIdKey = "mappe_id"
        private const val søknadType = FagsakYtelseTypeUri.PLEIEPENGER_SYKT_BARN
    }

    internal object Urls {
        internal const val HenteMapper = "/$søknadType/mapper"
        internal const val NySøknad = "/$søknadType"
        internal const val EksisterendeSøknad = "/$søknadType/mappe/{$MappeIdKey}"
        internal const val HentSøknadFraK9Sak = "/k9-sak/$søknadType"
        internal const val NySøknad2 = "/$søknadType/v2"
    }

    @Bean
    fun pleiepengerSyktBarnSøknadRoutes() = Routes {

        GET("/api${Urls.HenteMapper}") { request ->
            RequestContext(coroutineContext, request) {
                val personer = personService.finnPersoner(request.norskeIdenter())
                val mapperDto = mappeService.hentMapper(
                    personer = personer,
                    søknadType = FagsakYtelseType.PLEIEPENGER_SYKT_BARN
                ).map { mappe ->
                    mappe.tilDto { personId ->
                        personer.first { p -> p.personId == personId }.norskIdent
                    }
                }
                ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(MapperSvarDTO(mapperDto))
            }
        }

        GET("/api${Urls.EksisterendeSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                val mappe = mappeService.hent(request.mappeId())
                if (mappe == null) {
                    ServerResponse
                        .notFound()
                        .buildAndAwait()
                } else {
                    val mappeDTO = mappe.tilDto {
                        mappe.søker.personId
                    }
                    ServerResponse
                        .ok()
                        .json()
                        .bodyValueAndAwait(mappeDTO)
                }
            }
        }

        PUT("/api${Urls.EksisterendeSøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val innsending = request.innsending()

                val mappe = mappeService.utfyllendeInnsending(
                    mappeId = request.mappeId(),
                    innsending = innsending
                )

                if (mappe == null) {
                    ServerResponse
                        .notFound()
                        .buildAndAwait()
                } else {
                    val mappeDTO = mappe.tilDto {
                        mappe.søker.personId
                    }
                    println(mappeDTO)

                    ServerResponse
                        .ok()
                        .json()
                        .bodyValueAndAwait(mappeDTO)
                }
            }
        }

        POST("/api${Urls.EksisterendeSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                val innsending = request.innsending()
                val norskIdent = innsending.personer.keys.first()

                val søknadIdDto = innsending.personer[norskIdent]?.søknadIdDto

                val mappeId = request.mappeId()
                val mappe = mappeService.hent(mappeId)

                val søknadEntitet =
                    mappe?.bunke?.flatMap { b -> b.søknader!! }?.toList()?.first { s -> s.søknadId == søknadIdDto }

                if (søknadEntitet == null) {
                    return@RequestContext ServerResponse
                        .notFound()
                        .buildAndAwait()
                } else {
                    try {
                        val søknad: PleiepengerSøknadDto = objectMapper.convertValue(søknadEntitet.søknad!!)
                        val søknadK9Format = SøknadMapper.mapTilEksternFormat(søknad)
                        if (søknadK9Format.second.isNotEmpty()) {
                            val feil = søknadK9Format.second.map { feil ->
                                MappeFeil.SøknadFeilDto(feil.felt,
                                    feil.feilkode,
                                    feil.feilmelding)
                            }.toList()

                            return@RequestContext ServerResponse
                                .status(HttpStatus.BAD_REQUEST)
                                .json()
                                .bodyValueAndAwait(MappeFeil(mappeId, feil))
                        }

                        val convertValue: JournalposterDto = objectMapper.convertValue(søknadEntitet.journalposter!!)
                        pleiepengerSyktBarnSoknadService.sendSøknad(søknadK9Format.first, convertValue.journalposter)

                        //TODO(OJR) marker søknad som sendt_inn = TRUE
//                        mappeService.fjern(
//                            mappeId = mappeId,
//                            norskIdent = norskIdent
//                        )
                        return@RequestContext ServerResponse
                            .accepted()
                            .buildAndAwait()
                    } catch (e: ValideringsFeil) {
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
                val innsending = request.innsending()
                val mappe = mappeService.førsteInnsending(
                    innsending = innsending,
                    søknadType = FagsakYtelseType.PLEIEPENGER_SYKT_BARN
                )
                val mappeDTO = mappe.tilDto {
                    mappe.søker.personId
                }
                return@RequestContext ServerResponse
                    .created(request.mappeLocation(mappe.mappeId))
                    .json()
                    .bodyValueAndAwait(mappeDTO)
            }
        }

        DELETE("/api${Urls.EksisterendeSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                try {
//                    mappeService.slett(mappeid = request.mappeId())
                    throw IllegalStateException("støtter ikke lengre sletting av mapper")
                    ServerResponse.noContent().buildAndAwait()
                } catch (e: Exception) {
                    ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).buildAndAwait()
                }
            }
        }

        POST("/api${Urls.HentSøknadFraK9Sak}") { request ->
            RequestContext(coroutineContext, request) {
                val hentSøknad = request.hentSøknad()
                val psbUtfyltFraK9 = k9SakService.hentSisteMottattePsbSøknad(hentSøknad.norskIdent, hentSøknad.periode)
                    ?: return@RequestContext ServerResponse.notFound().buildAndAwait()

                val søknadIdDto =
                    mappeService.opprettTomSøknad(hentSøknad.norskIdent, FagsakYtelseType.PLEIEPENGER_SYKT_BARN)

                val søknadDto = SøknadDto(
                    søknadId = søknadIdDto,
                    søkerId = hentSøknad.norskIdent,
                    søknad = psbUtfyltFraK9,
                    journalposter = null,
                    erFraK9 = true
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


    private suspend fun ServerRequest.mappeId(): MappeId = pathVariable(MappeIdKey)

    private suspend fun ServerRequest.norskIdent(): NorskIdent? =
        if (norskeIdenter().size != 1) null else norskeIdenter().first()

    private suspend fun ServerRequest.norskeIdenter(): Set<NorskIdent> {
        val identer = mutableSetOf<NorskIdent>()
        headers().header("X-Nav-NorskIdent").forEach { it -> identer.addAll(it.split(",").onEach { it.trim() }) }
        return identer.toSet()
    }

    private suspend fun ServerRequest.innsending() = body(BodyExtractors.toMono(Innsending::class.java)).awaitFirst()
    private fun ServerRequest.mappeLocation(mappeId: MappeId) = uriBuilder().pathSegment("mappe", mappeId).build()
    private suspend fun ServerRequest.hentSøknad() = body(BodyExtractors.toMono(HentSøknad::class.java)).awaitFirst()
}

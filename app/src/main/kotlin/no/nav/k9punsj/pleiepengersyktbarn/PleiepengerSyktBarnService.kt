package no.nav.k9punsj.pleiepengersyktbarn

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Feil
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.domenetjenester.MappeService
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.domenetjenester.SoknadService
import no.nav.k9punsj.felles.dto.JournalposterDto
import no.nav.k9punsj.felles.dto.Matchfagsak
import no.nav.k9punsj.felles.dto.OpprettNySøknad
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.felles.dto.SendSøknad
import no.nav.k9punsj.domenetjenester.dto.SøknadFeil
import no.nav.k9punsj.felles.dto.hentUtJournalposter
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import no.nav.k9punsj.integrasjoner.punsjbollen.PunsjbolleService
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.openapi.OasFeil
import no.nav.k9punsj.tilgangskontroll.azuregraph.IAzureGraphService
import no.nav.k9punsj.utils.ServerRequestUtils.søknadLocationUri
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import org.springframework.web.reactive.function.server.json
import java.net.URI

@Service
internal class PleiepengerSyktBarnService(
    private val personService: PersonService,
    private val mappeService: MappeService,
    private val journalpostRepository: JournalpostRepository,
    private val azureGraphService: IAzureGraphService,
    private val objectMapper: ObjectMapper,
    private val soknadService: SoknadService,
    private val k9SakService: K9SakService,
    private val punsjbolleService: PunsjbolleService,
    @Value("\${no.nav.k9sak.frontend}") private val k9SakFrontend: URI
) {

    private companion object {
        val logger = LoggerFactory.getLogger(PleiepengerSyktBarnService::class.java)
    }

    internal suspend fun henteMappe(norskIdent: String): ServerResponse {
        val person = personService.finnPersonVedNorskIdent(norskIdent)
        if (person != null) {
            val svarDto = mappeService.hentMappe(
                person = person
            ).tilPsbVisning(norskIdent)
            return ServerResponse
                .ok()
                .json()
                .bodyValueAndAwait(svarDto)
        }
        return ServerResponse
            .ok()
            .json()
            .bodyValueAndAwait(SvarPsbDto(norskIdent, FagsakYtelseType.PLEIEPENGER_SYKT_BARN.kode, listOf()))
    }

    internal suspend fun henteSøknad(søknadId: String): ServerResponse {
        val søknad = mappeService.hentSøknad(søknadId)
            ?: return ServerResponse
                .notFound()
                .buildAndAwait()

        return ServerResponse
            .ok()
            .json()
            .bodyValueAndAwait(søknad.tilPsbvisning())
    }

    internal suspend fun oppdaterEksisterendeSøknad(
        request: ServerRequest, søknad: PleiepengerSyktBarnSøknadDto
    ): ServerResponse {
        val saksbehandler = azureGraphService.hentIdentTilInnloggetBruker()

        val søknadEntitet = mappeService.utfyllendeInnsendingPsb(
            pleiepengerSøknadDto = søknad,
            saksbehandler = saksbehandler
        ) ?: return ServerResponse.badRequest().buildAndAwait()

        val (entitet, _) = søknadEntitet
        val søker = personService.finnPerson(entitet.søkerId)
        journalpostRepository.settKildeHvisIkkeFinnesFraFør(
            hentUtJournalposter(entitet),
            søker.aktørId
        )

        return ServerResponse
            .ok()
            .json()
            .bodyValueAndAwait(søknad)
    }

    internal suspend fun sendEksisterendeSøknad(sendSøknad: SendSøknad): ServerResponse {
        val søknadEntitet = mappeService.hentSøknad(sendSøknad.soeknadId)
            ?: return ServerResponse.badRequest().buildAndAwait()

        try {
            val søknad: PleiepengerSyktBarnSøknadDto = objectMapper.convertValue(søknadEntitet.søknad!!)
            val hentPerioderSomFinnesIK9 = henterPerioderSomFinnesIK9sak(søknad)?.first ?: emptyList()

            val journalPoster = søknadEntitet.journalposter!!
            val journalposterDto: JournalposterDto = objectMapper.convertValue(journalPoster)
            val journalpostIder = journalposterDto.journalposter.filter { journalpostId ->
                journalpostRepository.kanSendeInn(listOf(journalpostId)).also { kanSendesInn ->
                    if (!kanSendesInn) {
                        logger.warn("JournalpostId $journalpostId kan ikke sendes inn. Filtreres bort fra innsendingen.")
                    }
                }
            }.toMutableSet()

            if (journalpostIder.isEmpty()) {
                logger.error("Innsendingen må inneholde minst en journalpost som kan sendes inn.")
                return ServerResponse
                    .status(HttpStatus.CONFLICT)
                    .bodyValueAndAwait(OasFeil("Innsendingen må inneholde minst en journalpost som kan sendes inn."))
            }

            val (søknadK9Format, feilISøknaden) = MapPsbTilK9Format(
                søknadId = søknad.soeknadId,
                journalpostIder = journalpostIder,
                perioderSomFinnesIK9 = hentPerioderSomFinnesIK9,
                dto = søknad
            ).søknadOgFeil()

            if (feilISøknaden.isNotEmpty()) {
                val feil = feilISøknaden.map { feil ->
                    SøknadFeil.SøknadFeilDto(
                        feil.felt,
                        feil.feilkode,
                        feil.feilmelding
                    )
                }.toList()

                return ServerResponse
                    .status(HttpStatus.BAD_REQUEST)
                    .json()
                    .bodyValueAndAwait(SøknadFeil(søknad.soeknadId, feil))
            }

            val feil = soknadService.sendSøknad(
                søknadK9Format,
                journalpostIder
            )
            if (feil != null) {
                val (httpStatus, feilen) = feil

                return ServerResponse
                    .status(httpStatus)
                    .json()
                    .bodyValueAndAwait(OasFeil(feilen))
            }

            return ServerResponse
                .accepted()
                .location(k9SakFrontendUrl(søknadK9Format))
                .json()
                .bodyValueAndAwait(søknadK9Format)

        } catch (e: Exception) {
            logger.error(e.localizedMessage, e)
            return ServerResponse
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .json()
                .bodyValueAndAwait(e.localizedMessage)
        }
    }

    internal suspend fun nySøknad(request: ServerRequest, søknad: OpprettNySøknad): ServerResponse {
        //oppretter sak i k9-sak hvis det ikke finnes fra før
        if (søknad.barnIdent != null) {
            punsjbolleService.opprettEllerHentFagsaksnummer(
                søker = søknad.norskIdent,
                pleietrengende = søknad.barnIdent,
                journalpostId = søknad.journalpostId,
                periode = null,
                fagsakYtelseType = no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN
            )
        }

        //setter riktig type der man jobber på en ukjent i utgangspunktet
        journalpostRepository.settFagsakYtelseType(
            FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
            søknad.journalpostId
        )

        val søknadEntitet = mappeService.førsteInnsendingPsb(
            nySøknad = søknad
        )
        return ServerResponse
            .created(request.søknadLocationUri(søknadEntitet.søknadId))
            .json()
            .bodyValueAndAwait(søknadEntitet.tilPsbvisning())
    }

    internal suspend fun validerSøknad(
        soknadTilValidering: PleiepengerSyktBarnSøknadDto
    ): ServerResponse {
        val søknadEntitet = mappeService.hentSøknad(soknadTilValidering.soeknadId)
            ?: return ServerResponse
                .badRequest()
                .buildAndAwait()

        val hentPerioderSomFinnesIK9 = henterPerioderSomFinnesIK9sak(soknadTilValidering)?.first ?: emptyList()
        val journalPoster = søknadEntitet.journalposter!!
        val journalposterDto: JournalposterDto = objectMapper.convertValue(journalPoster)

        val mapTilEksternFormat: Pair<Søknad, List<Feil>>?

        try {
            mapTilEksternFormat = MapPsbTilK9Format(
                søknadId = soknadTilValidering.soeknadId,
                journalpostIder = journalposterDto.journalposter,
                perioderSomFinnesIK9 = hentPerioderSomFinnesIK9,
                dto = soknadTilValidering
            ).søknadOgFeil()
        } catch (e: Exception) {
            return ServerResponse
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .json()
                .bodyValueAndAwait(e.localizedMessage)
        }

        val (søknad, feilListe) = mapTilEksternFormat
        if (feilListe.isNotEmpty()) {
            val feil = feilListe.map { feil ->
                SøknadFeil.SøknadFeilDto(
                    feil.felt,
                    feil.feilkode,
                    feil.feilmelding
                )
            }.toList()

            return ServerResponse
                .status(HttpStatus.BAD_REQUEST)
                .json()
                .bodyValueAndAwait(SøknadFeil(soknadTilValidering.soeknadId, feil))
        }
        val saksbehandler = azureGraphService.hentIdentTilInnloggetBruker()
        mappeService.utfyllendeInnsendingPsb(
            pleiepengerSøknadDto = soknadTilValidering,
            saksbehandler = saksbehandler
        )
        return ServerResponse
            .status(HttpStatus.ACCEPTED)
            .json()
            .bodyValueAndAwait(søknad)
    }

    internal suspend fun hentInfoFraK9Sak(matchfagsak: Matchfagsak): ServerResponse {
        val (perioder, _) = k9SakService.hentPerioderSomFinnesIK9(
            matchfagsak.brukerIdent,
            matchfagsak.barnIdent,
            FagsakYtelseType.PLEIEPENGER_SYKT_BARN
        ).first ?: return ServerResponse.ok().json().bodyValueAndAwait(listOf<PeriodeDto>())

        return ServerResponse
            .ok()
            .json()
            .bodyValueAndAwait(perioder)
    }


    private suspend fun k9SakFrontendUrl(søknad: Søknad) = punsjbolleService.opprettEllerHentFagsaksnummer(
        søker = søknad.søker.personIdent.verdi,
        pleietrengende = søknad.getYtelse<PleiepengerSyktBarn>().barn.personIdent.verdi,
        søknad = søknad
    ).let { saksnummer -> URI("$k9SakFrontend/fagsak/${saksnummer.saksnummer}/behandling/") }

    private suspend fun henterPerioderSomFinnesIK9sak(dto: PleiepengerSyktBarnSøknadDto): Pair<List<PeriodeDto>?, String?>? {
        if (dto.soekerId.isNullOrBlank() || dto.barn == null || dto.barn.norskIdent.isNullOrBlank()) {
            return null
        }
        return k9SakService.hentPerioderSomFinnesIK9(
            søker = dto.soekerId,
            barn = dto.barn.norskIdent,
            fagsakYtelseType = FagsakYtelseType.PLEIEPENGER_SYKT_BARN
        )
    }

}
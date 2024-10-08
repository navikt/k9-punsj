package no.nav.k9punsj.omsorgspengerutbetaling

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9.kodeverk.dokument.Brevkode
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Feil
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.domenetjenester.MappeService
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.domenetjenester.SoknadService
import no.nav.k9punsj.felles.PunsjFagsakYtelseType
import no.nav.k9punsj.felles.dto.ArbeidsgiverMedArbeidsforholdId
import no.nav.k9punsj.felles.dto.JournalposterDto
import no.nav.k9punsj.felles.dto.MatchFagsakMedPeriode
import no.nav.k9punsj.felles.dto.Matchfagsak
import no.nav.k9punsj.felles.dto.OpprettNySøknad
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.felles.dto.SendSøknad
import no.nav.k9punsj.felles.dto.SøknadFeil
import no.nav.k9punsj.felles.dto.hentUtJournalposter
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import no.nav.k9punsj.journalpost.JournalpostService
import no.nav.k9punsj.tilgangskontroll.azuregraph.IAzureGraphService
import no.nav.k9punsj.utils.ServerRequestUtils.søknadLocationUri
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import org.springframework.web.reactive.function.server.json

@Service
internal class OmsorgspengerutbetalingService(
    private val objectMapper: ObjectMapper,
    private val personService: PersonService,
    private val mappeService: MappeService,
    private val journalpostService: JournalpostService,
    private val azureGraphService: IAzureGraphService,
    private val soknadService: SoknadService,
    private val k9SakService: K9SakService,
    private val aksjonspunktService: AksjonspunktService,
) {

    private val logger: Logger = LoggerFactory.getLogger(OmsorgspengerutbetalingService::class.java)

    internal suspend fun henteMappe(norskIdent: String): ServerResponse {
        val person = personService.finnPersonVedNorskIdent(norskIdent = norskIdent)
        if (person != null) {
            val svarDto = mappeService.hentMappe(person = person)
                .tilOmsUtVisning(norskIdent)
            return ServerResponse
                .ok()
                .json()
                .bodyValueAndAwait(svarDto)
        }
        return ServerResponse
            .ok()
            .json()
            .bodyValueAndAwait(SvarOmsUtDto(norskIdent, PunsjFagsakYtelseType.OMSORGSPENGER.kode, listOf()))
    }

    internal suspend fun henteSøknad(søknadId: String): ServerResponse {
        val søknad = soknadService.hentSøknad(søknadId)

        return if (søknad == null) {
            ServerResponse
                .notFound()
                .buildAndAwait()
        } else {
            ServerResponse
                .ok()
                .json()
                .bodyValueAndAwait(søknad.tilOmsUtvisning())
        }
    }

    internal suspend fun nySøknad(request: ServerRequest, opprettNySøknad: OpprettNySøknad): ServerResponse {
        // setter riktig type der man jobber på en ukjent i utgangspunktet
        journalpostService.settFagsakYtelseType(PunsjFagsakYtelseType.OMSORGSPENGER, opprettNySøknad.journalpostId)

        val søknadEntitet = mappeService.førsteInnsendingOmsorgspengerutbetaling(
            nySøknad = opprettNySøknad
        )
        return ServerResponse
            .created(request.søknadLocationUri(søknadEntitet.søknadId))
            .json()
            .bodyValueAndAwait(søknadEntitet.tilOmsUtvisning())
    }

    internal suspend fun oppdaterEksisterendeSøknad(søknad: OmsorgspengerutbetalingSøknadDto): ServerResponse {
        val saksbehandler = azureGraphService.hentIdentTilInnloggetBruker()

        val søknadEntitet = mappeService.utfyllendeInnsendingOmsUt(
            omsorgspengerutbetalingSøknadDto = søknad,
            saksbehandler = saksbehandler
        ) ?: return ServerResponse.badRequest().buildAndAwait()

        val (entitet, _) = søknadEntitet
        val søker = personService.finnPerson(personId = entitet.søkerId)
        journalpostService.settKildeHvisIkkeFinnesFraFør(
            journalposter = hentUtJournalposter(entitet),
            aktørId = søker.aktørId
        )
        return ServerResponse
            .ok()
            .json()
            .bodyValueAndAwait(søknad)
    }

    internal suspend fun sendEksisterendeSøknad(sendSøknad: SendSøknad): ServerResponse {
        val søknadEntitet = soknadService.hentSøknad(sendSøknad.soeknadId)
            ?: return ServerResponse.badRequest().buildAndAwait()

        try {
            val søknad: OmsorgspengerutbetalingSøknadDto = objectMapper.convertValue(søknadEntitet.søknad!!)

            val journalPoster = søknadEntitet.journalposter!!
            val journalposterDto: JournalposterDto = objectMapper.convertValue(journalPoster)
            val journalpostIder = journalposterDto.journalposter.filter { journalpostId ->
                journalpostService.kanSendeInn(listOf(journalpostId)).also { kanSendesInn ->
                    if (!kanSendesInn) {
                        logger.warn("JournalpostId $journalpostId kan ikke sendes inn. Filtreres bort fra innsendingen.")
                    }
                }
            }.toMutableSet()

            if (journalpostIder.isEmpty()) {
                logger.error("Innsendingen må inneholde minst en journalpost som kan sendes inn.")
                return ServerResponse
                    .status(HttpStatus.CONFLICT)
                    .bodyValueAndAwait("Innsendingen må inneholde minst en journalpost som kan sendes inn.")
            }

            val eksisterendePerioder = if (søknad.erKorrigering!!) {
                val fravaersPeriode = søknad.fravaersperioder?.let {
                    søknad.periodeForHeleAretMedFravaer()
                }
                logger.info("Korrigering av søknad. Henter eksisterende perioder for soknadsperiode: [$fravaersPeriode]")
                hentInfoFraK9Sak(
                    Matchfagsak(
                        brukerIdent = sendSøknad.norskIdent,
                        periode = fravaersPeriode // Er periode null henter vi fraværsperioder fra i år.
                    )
                )
            } else listOf()

            val (søknadK9Format, feilListe) = MapOmsUtTilK9Format(
                søknadId = søknad.soeknadId,
                journalpostIder = journalpostIder,
                dto = søknad,
                eksisterendePerioder = eksisterendePerioder
            ).søknadOgFeil()

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
                    .bodyValueAndAwait(SøknadFeil(sendSøknad.soeknadId, feil))
            }

            val feil = soknadService.opprettSakOgSendInnSøknad(
                søknad = søknadK9Format,
                brevkode = Brevkode.PAPIRSØKNAD_UTBETALING_OMS_AT, // TODO: Finn en måte å spesifisere hvilken brevkode det gjelder.
                journalpostIder = journalpostIder
            )

            if (feil != null) {
                val (httpStatus, feilen) = feil
                return ServerResponse
                    .status(httpStatus)
                    .json()
                    .bodyValueAndAwait(feilen)
            }

            val ansvarligSaksbehandler = soknadService.hentSistEndretAvSaksbehandler(søknad.soeknadId)
            aksjonspunktService.settUtførtPåAltSendLukkOppgaveTilK9Los(
                journalpostId = journalpostIder,
                erSendtInn = true,
                ansvarligSaksbehandler = ansvarligSaksbehandler
            )

            return ServerResponse
                .accepted()
                .json()
                .bodyValueAndAwait(søknadK9Format)
        } catch (e: Exception) {
            return ServerResponse
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .json()
                .bodyValueAndAwait(e.localizedMessage)
        }
    }

    internal suspend fun validerSøknad(soknadTilValidering: OmsorgspengerutbetalingSøknadDto): ServerResponse {
        val søknadEntitet = soknadService.hentSøknad(soknadTilValidering.soeknadId)
            ?: return ServerResponse
                .badRequest()
                .buildAndAwait()

        val journalPoster = søknadEntitet.journalposter!!
        val journalposterDto: JournalposterDto = objectMapper.convertValue(journalPoster)

        val mapTilEksternFormat: Pair<Søknad, List<Feil>>?
        val eksisterendePerioder = if (soknadTilValidering.erKorrigering!!) {
            val fravaersPeriode = soknadTilValidering.fravaersperioder?.let {
                soknadTilValidering.periodeForHeleAretMedFravaer()
            }
            logger.info("Korrigering av søknad. Henter eksisterende perioder for soknadsperiode: [$fravaersPeriode]")
            hentInfoFraK9Sak(
                Matchfagsak(
                    brukerIdent = soknadTilValidering.soekerId!!,
                    periode = fravaersPeriode // Er periode null henter vi fraværsperioder fra i år.
                )
            )
        } else listOf()

        try {
            mapTilEksternFormat = MapOmsUtTilK9Format(
                søknadId = soknadTilValidering.soeknadId,
                journalpostIder = journalposterDto.journalposter,
                dto = soknadTilValidering,
                eksisterendePerioder = eksisterendePerioder
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

        return ServerResponse
            .status(HttpStatus.ACCEPTED)
            .json()
            .bodyValueAndAwait(søknad)
    }

    internal suspend fun hentArbeidsforholdIderFraK9Sak(matchFagsakMedPeriode: MatchFagsakMedPeriode): ServerResponse {
        val (arbeidsgiverMedArbeidsforholdId, feil) = k9SakService.hentArbeidsforholdIdFraInntektsmeldinger(
            søker = matchFagsakMedPeriode.brukerIdent,
            punsjFagsakYtelseType = PunsjFagsakYtelseType.OMSORGSPENGER,
            periodeDto = matchFagsakMedPeriode.periodeDto
        )

        return if (arbeidsgiverMedArbeidsforholdId != null) {
            ServerResponse
                .ok()
                .json()
                .bodyValueAndAwait(arbeidsgiverMedArbeidsforholdId)
        } else if (feil != null) {
            ServerResponse
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .json()
                .bodyValueAndAwait(feil)
        } else {
            ServerResponse
                .ok()
                .json()
                .bodyValueAndAwait(listOf<ArbeidsgiverMedArbeidsforholdId>())
        }
    }

    internal suspend fun hentInfoFraK9Sak(matchfagsak: Matchfagsak): List<PeriodeDto> {
        val (perioder, _) = if (matchfagsak.periode == null) {
            k9SakService.hentPerioderSomFinnesIK9(
                søker = matchfagsak.brukerIdent,
                punsjFagsakYtelseType = PunsjFagsakYtelseType.OMSORGSPENGER
            )
        } else {
            k9SakService.hentPerioderSomFinnesIK9ForPeriode(
                søker = matchfagsak.brukerIdent,
                punsjFagsakYtelseType = PunsjFagsakYtelseType.OMSORGSPENGER,
                periode = matchfagsak.periode
            )
        }

        return perioder ?: listOf()
    }
}

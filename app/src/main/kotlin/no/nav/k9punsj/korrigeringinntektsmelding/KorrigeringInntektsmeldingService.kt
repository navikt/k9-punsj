package no.nav.k9punsj.korrigeringinntektsmelding

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Feil
import no.nav.k9punsj.domenetjenester.MappeService
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.domenetjenester.SoknadService
import no.nav.k9punsj.domenetjenester.dto.SøknadFeil
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.SøknadFinnsIkke
import no.nav.k9punsj.felles.UventetFeil
import no.nav.k9punsj.felles.ValideringsFeil
import no.nav.k9punsj.felles.dto.ArbeidsgiverMedArbeidsforholdId
import no.nav.k9punsj.felles.dto.JournalposterDto
import no.nav.k9punsj.felles.dto.MatchFagsakMedPeriode
import no.nav.k9punsj.felles.dto.OpprettNySøknad
import no.nav.k9punsj.felles.dto.SendSøknad
import no.nav.k9punsj.felles.dto.hentUtJournalposter
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import no.nav.k9punsj.integrasjoner.punsjbollen.PunsjbolleService
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.tilgangskontroll.azuregraph.IAzureGraphService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
internal class KorrigeringInntektsmeldingService(
    private val objectMapper: ObjectMapper,
    private val personService: PersonService,
    private val mappeService: MappeService,
    private val punsjbolleService: PunsjbolleService,
    private val journalpostRepository: JournalpostRepository,
    private val azureGraphService: IAzureGraphService,
    private val soknadService: SoknadService,
    private val k9SakService: K9SakService
) {

    private val logger: Logger = LoggerFactory.getLogger(KorrigeringInntektsmeldingService::class.java)


    internal suspend fun henteMappe(norskIdent: String): SvarOmsDto {
        val person = personService.finnPersonVedNorskIdent(norskIdent = norskIdent)
        if (person != null) {
            val svarDto = mappeService.hentMappe(person = person)
                .tilOmsVisning(norskIdent)
            return svarDto
        }
        return SvarOmsDto(norskIdent, FagsakYtelseType.OMSORGSPENGER.kode, listOf())

    }

    internal suspend fun henteSøknad(søknadId: String): KorrigeringInntektsmeldingDto {
        val søknad = mappeService.hentSøknad(søknadId)

        return if (søknad == null) {
            throw SøknadFinnsIkke(søknadId)
        } else {
            return søknad.tilOmsvisning()
        }
    }

    internal suspend fun nySøknad(opprettNySøknad: OpprettNySøknad): KorrigeringInntektsmeldingDto {
        //oppretter sak i k9-sak hvis det ikke finnes fra før
        if (opprettNySøknad.pleietrengendeIdent != null) {
            punsjbolleService.opprettEllerHentFagsaksnummer(
                søker = opprettNySøknad.norskIdent,
                pleietrengende = opprettNySøknad.pleietrengendeIdent,
                journalpostId = opprettNySøknad.journalpostId,
                periode = null,
                fagsakYtelseType = no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER
            )
        }

        //setter riktig type der man jobber på en ukjent i utgangspunktet
        journalpostRepository.settFagsakYtelseType(FagsakYtelseType.OMSORGSPENGER, opprettNySøknad.journalpostId)

        val søknadEntitet = mappeService.førsteInnsendingKorrigeringIm(
            nySøknad = opprettNySøknad
        )
        return søknadEntitet.tilOmsvisning()
    }

    internal suspend fun oppdaterEksisterendeSøknad(søknad: KorrigeringInntektsmeldingDto): KorrigeringInntektsmeldingDto {
        val saksbehandler = azureGraphService.hentIdentTilInnloggetBruker()

        val søknadEntitet = mappeService.utfyllendeInnsendingOms(
            korrigeringInntektsmeldingDto = søknad,
            saksbehandler = saksbehandler
        ) ?: throw SøknadFinnsIkke(søknad.soeknadId)

        val (entitet, _) = søknadEntitet
        val søker = personService.finnPerson(personId = entitet.søkerId)
        journalpostRepository.settKildeHvisIkkeFinnesFraFør(
            journalposter = hentUtJournalposter(entitet),
            aktørId = søker.aktørId
        )
        return søknad
    }

    internal suspend fun sendEksisterendeSøknad(sendSøknad: SendSøknad): Søknad {
        val søknadEntitet = mappeService.hentSøknad(sendSøknad.soeknadId)
            ?: throw SøknadFinnsIkke(sendSøknad.soeknadId)

        try {
            val søknad: KorrigeringInntektsmeldingDto = objectMapper.convertValue(søknadEntitet.søknad!!)

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
                throw ValideringsFeil("Innsendingen må inneholde minst en journalpost som kan sendes inn.")
            }

            val (søknadK9Format, feilListe) = MapOmsTilK9Format(
                søknadId = søknad.soeknadId,
                journalpostIder = journalpostIder,
                dto = søknad
            ).søknadOgFeil()

            if (feilListe.isNotEmpty()) {
                val feil = feilListe.map { feil ->
                    SøknadFeil.SøknadFeilDto(
                        feil.felt,
                        feil.feilkode,
                        feil.feilmelding
                    )
                }.toList()

                throw ValideringsFeil(feil.joinToString(separator = " - "))
            }

            val feil = soknadService.sendSøknad(
                søknadK9Format,
                journalpostIder
            )

            if (feil != null) {
                val (httpStatus, feilen) = feil
                throw ValideringsFeil(feilen)
            }

            return søknadK9Format

        } catch (e: Exception) {
            throw UventetFeil(e.localizedMessage)
        }

    }

    internal suspend fun validerSøknad(soknadTilValidering: KorrigeringInntektsmeldingDto): Søknad {
        val søknadEntitet = mappeService.hentSøknad(soknadTilValidering.soeknadId)
            ?: throw SøknadFinnsIkke(soknadTilValidering.soeknadId)

        val journalPoster = søknadEntitet.journalposter!!
        val journalposterDto: JournalposterDto = objectMapper.convertValue(journalPoster)

        val mapTilEksternFormat: Pair<Søknad, List<Feil>>?

        try {
            mapTilEksternFormat = MapOmsTilK9Format(
                søknadId = soknadTilValidering.soeknadId,
                journalpostIder = journalposterDto.journalposter,
                dto = soknadTilValidering
            ).søknadOgFeil()
        } catch (e: Exception) {
            throw UventetFeil(e.localizedMessage)
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

            throw ValideringsFeil(feil.joinToString(separator = " - "))
        }

        return søknad
    }

    internal suspend fun hentArbeidsforholdIderFraK9Sak(matchFagsakMedPeriode: MatchFagsakMedPeriode): List<ArbeidsgiverMedArbeidsforholdId> {
        val (arbeidsgiverMedArbeidsforholdId, feil) = k9SakService.hentArbeidsforholdIdFraInntektsmeldinger(
            søker = matchFagsakMedPeriode.brukerIdent,
            fagsakYtelseType = FagsakYtelseType.OMSORGSPENGER,
            periodeDto = matchFagsakMedPeriode.periodeDto
        )

        return if (arbeidsgiverMedArbeidsforholdId != null) {
            arbeidsgiverMedArbeidsforholdId
        } else if (feil != null) {
            throw UventetFeil(feil)
        } else {
            emptyList()
        }

    }
}
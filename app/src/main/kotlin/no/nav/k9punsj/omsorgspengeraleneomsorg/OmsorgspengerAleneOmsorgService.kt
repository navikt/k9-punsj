package no.nav.k9punsj.omsorgspengeraleneomsorg

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9.kodeverk.dokument.Brevkode
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Feil
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.domenetjenester.MappeService
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.domenetjenester.SoknadService
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.dto.JournalposterDto
import no.nav.k9punsj.felles.dto.OpprettNySøknad
import no.nav.k9punsj.felles.dto.SendK9SoknadDto
import no.nav.k9punsj.felles.dto.SendSøknad
import no.nav.k9punsj.felles.dto.SøknadFeil
import no.nav.k9punsj.felles.dto.hentUtJournalposter
import no.nav.k9punsj.integrasjoner.punsjbollen.PunsjbolleService
import no.nav.k9punsj.journalpost.JournalpostService
import no.nav.k9punsj.tilgangskontroll.azuregraph.IAzureGraphService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
internal class OmsorgspengerAleneOmsorgService(
    private val objectMapper: ObjectMapper,
    private val mappeService: MappeService,
    private val personService: PersonService,
    private val punsjbolleService: PunsjbolleService,
    private val azureGraphService: IAzureGraphService,
    private val journalpostService: JournalpostService,
    private val soknadService: SoknadService,
    private val aksjonspunktService: AksjonspunktService
) {

    private val logger = LoggerFactory.getLogger(OmsorgspengerAleneOmsorgService::class.java)

    suspend fun henteMappe(norskIdent: String): SvarOmsAODto {
        personService.finnPersonVedNorskIdent(norskIdent)?.let { person ->
            return mappeService.hentMappe(person = person)
                .tilOmsAOVisning(norskIdent)
        }

        return SvarOmsAODto(
            søker = norskIdent,
            fagsakTypeKode = FagsakYtelseType.OMSORGSPENGER_ALENE_OMSORGEN.kode,
            søknader = listOf()
        )
    }

    suspend fun henteSøknad(søknadId: String): OmsorgspengerAleneOmsorgSøknadDto? {
        val søknad = mappeService.hentSøknad(søknad = søknadId)
            ?: return null

        return søknad.tilOmsAOvisning()
    }

    suspend fun nySøknad(nySøknad: OpprettNySøknad)
        : Pair<String, OmsorgspengerAleneOmsorgSøknadDto> {
        // oppretter sak i k9-sak hvis det ikke finnes fra før
        if (nySøknad.pleietrengendeIdent != null) {
            punsjbolleService.opprettEllerHentFagsaksnummer(
                søker = nySøknad.norskIdent,
                pleietrengende = nySøknad.pleietrengendeIdent,
                journalpostId = nySøknad.journalpostId,
                periode = null,
                fagsakYtelseType = no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_AO
            )
        }

        // setter riktig type der man jobber på en ukjent i utgangspunktet
        journalpostService.settFagsakYtelseType(
            ytelseType = FagsakYtelseType.OMSORGSPENGER_ALENE_OMSORGEN,
            journalpostId = nySøknad.journalpostId
        )

        val søknadEntitet = mappeService.førsteInnsendingOmsAO(
            nySøknad = nySøknad
        )

        return Pair(søknadEntitet.søknadId, søknadEntitet.tilOmsAOvisning())
    }

    suspend fun oppdaterEksisterendeSøknad(søknad: OmsorgspengerAleneOmsorgSøknadDto)
        : OmsorgspengerAleneOmsorgSøknadDto? {
        val saksbehandler = azureGraphService.hentIdentTilInnloggetBruker()
        val søknadEntitet = mappeService.utfyllendeInnsendingOmsAO(
            dto = søknad,
            saksbehandler = saksbehandler
        ) ?: return null

        val (entitet, _) = søknadEntitet
        val søker = personService.finnPerson(personId = entitet.søkerId)
        journalpostService.settKildeHvisIkkeFinnesFraFør(
            journalposter = hentUtJournalposter(entitet),
            aktørId = søker.aktørId
        )
        return søknad
    }

    suspend fun sendEksisterendeSøknad(sendSøknad: SendSøknad): SendK9SoknadDto? {
        val søknadEntitet = mappeService.hentSøknad(sendSøknad.soeknadId)
            ?: return null

        val søknad: OmsorgspengerAleneOmsorgSøknadDto =
            objectMapper.convertValue(søknadEntitet.søknad!!)

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
            throw IllegalStateException("Innsendingen må inneholde minst en journalpost som kan sendes inn.")
        }

        val (søknadK9Format, feilListe) = MapOmsAOTilK9Format(
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

            return SendK9SoknadDto(SøknadFeil(sendSøknad.soeknadId, feil), null)
        }

        val feil = soknadService.sendSøknad(
            søknad = søknadK9Format,
            brevkode = Brevkode.SØKNAD_OMS_UTVIDETRETT_AO,
            journalpostIder = journalpostIder
        )

        if (feil != null) {
            val (_, feilen) = feil
            throw IllegalStateException(feilen)
        }

        val ansvarligSaksbehandler = soknadService.hentSistEndretAvSaksbehandler(søknad.soeknadId)
        aksjonspunktService.settUtførtPåAltSendLukkOppgaveTilK9Los(
            journalpostId = journalpostIder,
            erSendtInn = true,
            ansvarligSaksbehandler = ansvarligSaksbehandler
        )

        return SendK9SoknadDto(null, søknadK9Format)
    }

    suspend fun validerSøknad(soknadTilValidering: OmsorgspengerAleneOmsorgSøknadDto): SendK9SoknadDto? {
        val søknadEntitet = mappeService.hentSøknad(soknadTilValidering.soeknadId)
            ?: return null

        val journalPoster = søknadEntitet.journalposter!!
        val journalposterDto: JournalposterDto = objectMapper.convertValue(journalPoster)

        val mapTilEksternFormat: Pair<Søknad, List<Feil>>?

        try {
            mapTilEksternFormat = MapOmsAOTilK9Format(
                søknadId = soknadTilValidering.soeknadId,
                journalpostIder = journalposterDto.journalposter,
                dto = soknadTilValidering
            ).søknadOgFeil()
        } catch (e: Exception) {
            throw IllegalStateException(e.localizedMessage)
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

            return SendK9SoknadDto(SøknadFeil(soknadTilValidering.soeknadId, feil), null)
        }

        return SendK9SoknadDto(null, søknad)
    }
}

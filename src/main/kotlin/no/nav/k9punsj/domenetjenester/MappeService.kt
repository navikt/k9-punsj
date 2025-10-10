package no.nav.k9punsj.domenetjenester

import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9punsj.domenetjenester.repository.BunkeRepository
import no.nav.k9punsj.domenetjenester.repository.MappeRepository
import no.nav.k9punsj.felles.PunsjFagsakYtelseType
import no.nav.k9punsj.felles.JsonB
import no.nav.k9punsj.felles.dto.*
import no.nav.k9punsj.journalpost.JournalpostService
import no.nav.k9punsj.korrigeringinntektsmelding.KorrigeringInntektsmeldingDto
import no.nav.k9punsj.utils.objectMapper
import no.nav.k9punsj.omsorgspengeraleneomsorg.OmsorgspengerAleneOmsorgSøknadDto
import no.nav.k9punsj.omsorgspengerkronisksyktbarn.OmsorgspengerKroniskSyktBarnSøknadDto
import no.nav.k9punsj.omsorgspengermidlertidigalene.NyOmsMASøknad
import no.nav.k9punsj.omsorgspengermidlertidigalene.OmsorgspengerMidlertidigAleneSøknadDto
import no.nav.k9punsj.omsorgspengerutbetaling.OmsorgspengerutbetalingSøknadDto
import no.nav.k9punsj.opplaeringspenger.OpplaeringspengerSøknadDto
import no.nav.k9punsj.pleiepengerlivetssluttfase.PleiepengerLivetsSluttfaseSøknadDto
import no.nav.k9punsj.pleiepengersyktbarn.PleiepengerSyktBarnSøknadDto
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

@Service
internal class MappeService(
    private val mappeRepository: MappeRepository,
    private val soknadService: SoknadService,
    private val bunkeRepository: BunkeRepository,
    private val personService: PersonService,
    private val journalpostService: JournalpostService,
) {

    suspend fun hentMappe(person: Person): Mappe {
        return henterMappeMedAlleKoblinger(mappeRepository.opprettEllerHentMappeForPerson(person.personId), person)
    }

    suspend fun slettMappeMedAlleKoblinger() {
        soknadService.slettAlleSøknader() // Sletter først alle søknader
        bunkeRepository.slettAlleBunker() // Sletter deretter alle bunker
        mappeRepository.slettAlleMapper() // Til slutt, alle mapper
    }

    suspend fun førsteInnsendingOlp(nySøknad: OpprettNySøknad): SøknadEntitet {
        val norskIdent = nySøknad.norskIdent
        val barnIdent = nySøknad.pleietrengendeIdent
        val søker = personService.finnEllerOpprettPersonVedNorskIdent(norskIdent)

        val mappeId = mappeRepository.opprettEllerHentMappeForPerson(søker.personId)
        val bunkeId = bunkeRepository.opprettEllerHentBunkeForFagsakType(mappeId, PunsjFagsakYtelseType.OPPLÆRINGSPENGER)
        val søknadfelles = felles(nySøknad.journalpostId)
        val opplaeringspengerSøknadDto =
            OpplaeringspengerSøknadDto(
                soeknadId = søknadfelles.søknadsId.toString(),
                soekerId = norskIdent,
                barn = OpplaeringspengerSøknadDto.BarnDto(barnIdent, null),
                journalposter = listOf(nySøknad.journalpostId),
                mottattDato = søknadfelles.mottattDato?.toLocalDate(),
                klokkeslett = søknadfelles.klokkeslett,
                harInfoSomIkkeKanPunsjes = false,
                k9saksnummer = nySøknad.k9saksnummer
            )

        return opprettSøknadEntitet(
            søknadfelles.søknadsId,
            bunkeId,
            søker,
            søknadfelles.journalposter,
            opplaeringspengerSøknadDto,
            nySøknad.k9saksnummer
        )
    }

    suspend fun førsteInnsendingPsb(nySøknad: OpprettNySøknad): SøknadEntitet {
        val norskIdent = nySøknad.norskIdent
        val barnIdent = nySøknad.pleietrengendeIdent
        val søker = personService.finnEllerOpprettPersonVedNorskIdent(norskIdent)

        val mappeId = mappeRepository.opprettEllerHentMappeForPerson(søker.personId)
        val bunkeId =
            bunkeRepository.opprettEllerHentBunkeForFagsakType(mappeId, PunsjFagsakYtelseType.PLEIEPENGER_SYKT_BARN)
        val søknadfelles = felles(nySøknad.journalpostId)
        val pleiepengerSøknadDto =
            PleiepengerSyktBarnSøknadDto(
                soeknadId = søknadfelles.søknadsId.toString(),
                soekerId = norskIdent,
                barn = PleiepengerSyktBarnSøknadDto.BarnDto(barnIdent, null),
                journalposter = listOf(nySøknad.journalpostId),
                mottattDato = søknadfelles.mottattDato?.toLocalDate(),
                klokkeslett = søknadfelles.klokkeslett,
                harInfoSomIkkeKanPunsjes = false,
                harMedisinskeOpplysninger = false,
                k9saksnummer = nySøknad.k9saksnummer
            )

        return opprettSøknadEntitet(
            søknadfelles.søknadsId,
            bunkeId,
            søker,
            søknadfelles.journalposter,
            pleiepengerSøknadDto,
            nySøknad.k9saksnummer
        )
    }

    suspend fun forsteInnsendingPls(nySøknad: OpprettNySøknad): SøknadEntitet {
        val norskIdent = nySøknad.norskIdent
        val pleietrengendeIdent = nySøknad.pleietrengendeIdent
        val soker = personService.finnEllerOpprettPersonVedNorskIdent(norskIdent)

        val mappeId = mappeRepository.opprettEllerHentMappeForPerson(soker.personId)
        val bunkeId =
            bunkeRepository.opprettEllerHentBunkeForFagsakType(mappeId, PunsjFagsakYtelseType.PLEIEPENGER_LIVETS_SLUTTFASE)
        val soknadfelles = felles(nySøknad.journalpostId)
        val pleiepengerLivetsSluttfaseSoknadDto = PleiepengerLivetsSluttfaseSøknadDto(
            soeknadId = soknadfelles.søknadsId.toString(),
            soekerId = norskIdent,
            pleietrengende = PleietrengendeDto(pleietrengendeIdent),
            journalposter = listOf(nySøknad.journalpostId),
            mottattDato = soknadfelles.mottattDato?.toLocalDate(),
            klokkeslett = soknadfelles.klokkeslett,
            harInfoSomIkkeKanPunsjes = false,
            harMedisinskeOpplysninger = false
        )

        return opprettSøknadEntitet(
            soknadfelles.søknadsId,
            bunkeId,
            soker,
            soknadfelles.journalposter,
            pleiepengerLivetsSluttfaseSoknadDto,
            nySøknad.k9saksnummer
        )
    }

    private suspend fun felles(journalpostId: String): Søknadfelles {
        val søknadId = UUID.randomUUID()

        val journalposter: MutableMap<String, Any?> = mutableMapOf()
        journalposter["journalposter"] = listOf(journalpostId)

        val mottattDato: LocalDateTime? = hentTidligsteMottattDatoFraJournalposter(journalpostId)
        val klokkeslett: LocalTime? =
            if (mottattDato?.toLocalTime() != null) LocalTime.of(
                mottattDato.toLocalTime().hour,
                mottattDato.toLocalTime().minute
            ) else null

        return Søknadfelles(søknadId, journalposter, mottattDato, klokkeslett)
    }

    suspend fun førsteInnsendingKorrigeringIm(nySøknad: OpprettNySøknad): SøknadEntitet {
        val norskIdent = nySøknad.norskIdent
        val søker = personService.finnEllerOpprettPersonVedNorskIdent(norskIdent)
        val mappeId = mappeRepository.opprettEllerHentMappeForPerson(søker.personId)
        val bunkeId = bunkeRepository.opprettEllerHentBunkeForFagsakType(mappeId, PunsjFagsakYtelseType.OMSORGSPENGER)

        val søknadfelles = felles(nySøknad.journalpostId)
        val dto = KorrigeringInntektsmeldingDto(
            soeknadId = søknadfelles.søknadsId.toString(),
            journalposter = listOf(nySøknad.journalpostId),
            mottattDato = søknadfelles.mottattDato?.toLocalDate(),
            klokkeslett = søknadfelles.klokkeslett,
            soekerId = norskIdent,
            k9saksnummer = nySøknad.k9saksnummer
        )
        return opprettSøknadEntitet(
            søknadfelles.søknadsId,
            bunkeId,
            søker,
            søknadfelles.journalposter,
            dto,
            nySøknad.k9saksnummer
        )
    }

    suspend fun førsteInnsendingOmsorgspengerutbetaling(nySøknad: OpprettNySøknad): SøknadEntitet {
        val norskIdent = nySøknad.norskIdent
        val søker = personService.finnEllerOpprettPersonVedNorskIdent(norskIdent)
        val mappeId = mappeRepository.opprettEllerHentMappeForPerson(søker.personId)
        val bunkeId = bunkeRepository.opprettEllerHentBunkeForFagsakType(mappeId, PunsjFagsakYtelseType.OMSORGSPENGER)

        val søknadfelles = felles(nySøknad.journalpostId)
        val dto = OmsorgspengerutbetalingSøknadDto(
            soeknadId = søknadfelles.søknadsId.toString(),
            journalposter = listOf(nySøknad.journalpostId),
            mottattDato = søknadfelles.mottattDato?.toLocalDate(),
            klokkeslett = søknadfelles.klokkeslett,
            soekerId = norskIdent,
            k9saksnummer = nySøknad.k9saksnummer
        )
        return opprettSøknadEntitet(
            søknadfelles.søknadsId,
            bunkeId,
            søker,
            søknadfelles.journalposter,
            dto,
            nySøknad.k9saksnummer
        )
    }

    suspend fun førsteInnsendingOmsKSB(nySøknad: OpprettNySøknad): SøknadEntitet {
        val norskIdent = nySøknad.norskIdent
        val søker = personService.finnEllerOpprettPersonVedNorskIdent(norskIdent)
        val pleietrengendeIdent = requireNotNull(nySøknad.pleietrengendeIdent) { "Mangler pleietrengendeIdent" }
        val mappeId = mappeRepository.opprettEllerHentMappeForPerson(søker.personId)
        val bunkeId = bunkeRepository.opprettEllerHentBunkeForFagsakType(
            mappeId,
            PunsjFagsakYtelseType.OMSORGSPENGER_KRONISK_SYKT_BARN
        )

        val søknadfelles = felles(nySøknad.journalpostId)
        val dto = OmsorgspengerKroniskSyktBarnSøknadDto(
            soeknadId = søknadfelles.søknadsId.toString(),
            journalposter = listOf(nySøknad.journalpostId),
            mottattDato = søknadfelles.mottattDato?.toLocalDate(),
            klokkeslett = søknadfelles.klokkeslett,
            soekerId = norskIdent,
            barn = OmsorgspengerKroniskSyktBarnSøknadDto.BarnDto(pleietrengendeIdent, null),
            harInfoSomIkkeKanPunsjes = false,
            harMedisinskeOpplysninger = false,
            k9saksnummer = nySøknad.k9saksnummer
        )
        return opprettSøknadEntitet(
            søknadfelles.søknadsId,
            bunkeId,
            søker,
            søknadfelles.journalposter,
            dto,
            nySøknad.k9saksnummer
        )
    }

    suspend fun førsteInnsendingOmsAO(nySøknad: OpprettNySøknad): SøknadEntitet {
        val norskIdent = nySøknad.norskIdent
        val søker = personService.finnEllerOpprettPersonVedNorskIdent(norskIdent)
        val mappeId = mappeRepository.opprettEllerHentMappeForPerson(søker.personId)
        val bunkeId =
            bunkeRepository.opprettEllerHentBunkeForFagsakType(mappeId, PunsjFagsakYtelseType.OMSORGSPENGER_ALENE_OMSORGEN)

        val søknadfelles = felles(nySøknad.journalpostId)
        val dto = OmsorgspengerAleneOmsorgSøknadDto(
            soeknadId = søknadfelles.søknadsId.toString(),
            journalposter = listOf(nySøknad.journalpostId),
            mottattDato = søknadfelles.mottattDato?.toLocalDate(),
            klokkeslett = søknadfelles.klokkeslett,
            soekerId = norskIdent,
            barn = nySøknad.pleietrengendeIdent?.let {
                OmsorgspengerAleneOmsorgSøknadDto.BarnDto(norskIdent = it, foedselsdato = null)
            },
            k9saksnummer = nySøknad.k9saksnummer
        )
        return opprettSøknadEntitet(
            søknadfelles.søknadsId,
            bunkeId,
            søker,
            søknadfelles.journalposter,
            dto,
            nySøknad.k9saksnummer
        )
    }

    suspend fun førsteInnsendingOmsMA(nySøknad: NyOmsMASøknad): SøknadEntitet {
        val norskIdent = nySøknad.norskIdent
        val søker = personService.finnEllerOpprettPersonVedNorskIdent(norskIdent)
        val mappeId = mappeRepository.opprettEllerHentMappeForPerson(søker.personId)
        val bunkeId = bunkeRepository.opprettEllerHentBunkeForFagsakType(
            mappeId,
            PunsjFagsakYtelseType.OMSORGSPENGER_MIDLERTIDIG_ALENE
        )

        val søknadfelles = felles(nySøknad.journalpostId)
        val dto = OmsorgspengerMidlertidigAleneSøknadDto(
            soeknadId = søknadfelles.søknadsId.toString(),
            journalposter = listOf(nySøknad.journalpostId),
            mottattDato = søknadfelles.mottattDato?.toLocalDate(),
            klokkeslett = søknadfelles.klokkeslett,
            soekerId = norskIdent,
            barn = nySøknad.barn,
            annenForelder = OmsorgspengerMidlertidigAleneSøknadDto.AnnenForelder(
                norskIdent = nySøknad.annenPart,
                situasjonType = null,
                situasjonBeskrivelse = null,
                periode = null
            ),
            k9saksnummer = nySøknad.k9saksnummer
        )
        return opprettSøknadEntitet(
            søknadfelles.søknadsId,
            bunkeId,
            søker,
            søknadfelles.journalposter,
            dto,
            nySøknad.k9saksnummer
        )
    }

    private suspend fun opprettSøknadEntitet(
        søknadId: UUID,
        bunkeId: String,
        søker: Person,
        journalposter: MutableMap<String, Any?>,
        dto: Any,
        k9Saksnummer: String?,
    ): SøknadEntitet {
        val søknadEntitet = SøknadEntitet(
            søknadId = søknadId.toString(),
            bunkeId = bunkeId,
            søkerId = søker.personId,
            journalposter = journalposter,
            søknad = objectMapper().convertValue<JsonB>(dto),
            k9saksnummer = k9Saksnummer
        )
        return soknadService.opprettSøknad(søknadEntitet)
    }

    private suspend fun hentTidligsteMottattDatoFraJournalposter(journalpostIdDto: String): LocalDateTime? {
        return journalpostService.hentHvisJournalpostMedId(journalpostIdDto)?.mottattDato
    }

    suspend fun utfyllendeInnsendingOlp(
        opplaeringspengerSøknadDto: OpplaeringspengerSøknadDto,
        saksbehandler: String,
    ): Pair<SøknadEntitet, OpplaeringspengerSøknadDto>? {
        val søknadEntitet = soknadService.hentSøknad(opplaeringspengerSøknadDto.soeknadId)!!
        return if (søknadEntitet.sendtInn.not()) {
            val journalposter =
                leggTilJournalpost(opplaeringspengerSøknadDto.journalposter, søknadEntitet.journalposter)
            val søknadJson = objectMapper().convertValue<JsonB>(opplaeringspengerSøknadDto)
            val oppdatertSøknad =
                søknadEntitet.copy(
                    søknad = søknadJson,
                    journalposter = journalposter,
                    endret_av = saksbehandler,
                    k9saksnummer = søknadEntitet.k9saksnummer
                )
            soknadService.oppdaterSøknad(oppdatertSøknad)

            val nySøknad = opplaeringspengerSøknadDto.copy(
                journalposter = journalposter.values.toList()
                    .filterIsInstance<String>(),
                k9saksnummer = søknadEntitet.k9saksnummer
            )
            Pair(oppdatertSøknad, nySøknad)
        } else {
            null
        }
    }

    suspend fun utfyllendeInnsendingPsb(
        pleiepengerSøknadDto: PleiepengerSyktBarnSøknadDto,
        saksbehandler: String,
    ): Pair<SøknadEntitet, PleiepengerSyktBarnSøknadDto>? {
        val søknadEntitet = soknadService.hentSøknad(pleiepengerSøknadDto.soeknadId)!!
        return if (søknadEntitet.sendtInn.not()) {
            val journalposter = leggTilJournalpost(pleiepengerSøknadDto.journalposter, søknadEntitet.journalposter)
            val søknadJson = objectMapper().convertValue<JsonB>(pleiepengerSøknadDto)
            val oppdatertSøknad = søknadEntitet.copy(søknad = søknadJson, journalposter = journalposter, endret_av = saksbehandler)
            soknadService.oppdaterSøknad(oppdatertSøknad)

            val nySøknad = pleiepengerSøknadDto.copy(
                journalposter = journalposter.values.toList()
                    .filterIsInstance<String>(),
                k9saksnummer = søknadEntitet.k9saksnummer
            )
            Pair(oppdatertSøknad, nySøknad)
        } else {
            null
        }
    }

    suspend fun utfyllendeInnsendingPls(
        dto: PleiepengerLivetsSluttfaseSøknadDto,
        saksbehandler: String,
    ): Pair<SøknadEntitet, PleiepengerLivetsSluttfaseSøknadDto>? {
        val søknadEntitet = soknadService.hentSøknad(dto.soeknadId)!!
        return if (søknadEntitet.sendtInn.not()) {
            val journalposter = leggTilJournalpost(dto.journalposter, søknadEntitet.journalposter)
            val søknadJson = objectMapper().convertValue<JsonB>(dto)
            val oppdatertSøknad =
                søknadEntitet.copy(søknad = søknadJson, journalposter = journalposter, endret_av = saksbehandler)
            soknadService.oppdaterSøknad(oppdatertSøknad)

            val nySøknad = dto.copy(
                journalposter = journalposter.values.toList()
                    .filterIsInstance<String>(),
                k9saksnummer = søknadEntitet.k9saksnummer
            )
            Pair(oppdatertSøknad, nySøknad)
        } else {
            null
        }
    }

    suspend fun utfyllendeInnsendingOms(
        korrigeringInntektsmeldingDto: KorrigeringInntektsmeldingDto,
        saksbehandler: String,
    ): Pair<SøknadEntitet, KorrigeringInntektsmeldingDto>? {
        val søknadEntitet = soknadService.hentSøknad(korrigeringInntektsmeldingDto.soeknadId)!!
        return if (søknadEntitet.sendtInn.not()) {
            val journalposter =
                leggTilJournalpost(korrigeringInntektsmeldingDto.journalposter, søknadEntitet.journalposter)
            val søknadJson = objectMapper().convertValue<JsonB>(korrigeringInntektsmeldingDto)
            val oppdatertSøknad =
                søknadEntitet.copy(søknad = søknadJson, journalposter = journalposter, endret_av = saksbehandler)
            soknadService.oppdaterSøknad(oppdatertSøknad)
            val nySøknad = korrigeringInntektsmeldingDto.copy(
                journalposter = journalposter.values.toList().filterIsInstance<String>(),
                k9saksnummer = søknadEntitet.k9saksnummer
            )
            Pair(oppdatertSøknad, nySøknad)
        } else {
            null
        }
    }

    suspend fun utfyllendeInnsendingOmsUt(
        omsorgspengerutbetalingSøknadDto: OmsorgspengerutbetalingSøknadDto,
        saksbehandler: String,
    ): Pair<SøknadEntitet, OmsorgspengerutbetalingSøknadDto>? {
        val søknadEntitet = soknadService.hentSøknad(omsorgspengerutbetalingSøknadDto.soeknadId)!!
        return if (søknadEntitet.sendtInn.not()) {
            val journalposter =
                leggTilJournalpost(omsorgspengerutbetalingSøknadDto.journalposter, søknadEntitet.journalposter)
            val søknadJson = objectMapper().convertValue<JsonB>(omsorgspengerutbetalingSøknadDto)
            val oppdatertSøknad =
                søknadEntitet.copy(søknad = søknadJson, journalposter = journalposter, endret_av = saksbehandler)
            soknadService.oppdaterSøknad(oppdatertSøknad)
            val nySøknad = omsorgspengerutbetalingSøknadDto.copy(
                journalposter = journalposter.values.toList().filterIsInstance<String>(),
                k9saksnummer = søknadEntitet.k9saksnummer
            )
            Pair(oppdatertSøknad, nySøknad)
        } else {
            null
        }
    }

    suspend fun utfyllendeInnsendingOmsKSB(
        omsorgspengerKroniskSyktBarnSøknadDto: OmsorgspengerKroniskSyktBarnSøknadDto,
        saksbehandler: String,
    ): Pair<SøknadEntitet, OmsorgspengerKroniskSyktBarnSøknadDto>? {
        val søknadEntitet = soknadService.hentSøknad(omsorgspengerKroniskSyktBarnSøknadDto.soeknadId)!!
        return if (søknadEntitet.sendtInn.not()) {
            val journalposter =
                leggTilJournalpost(omsorgspengerKroniskSyktBarnSøknadDto.journalposter, søknadEntitet.journalposter)
            val søknadJson = objectMapper().convertValue<JsonB>(omsorgspengerKroniskSyktBarnSøknadDto)
            val oppdatertSøknad =
                søknadEntitet.copy(søknad = søknadJson, journalposter = journalposter, endret_av = saksbehandler)
            soknadService.oppdaterSøknad(oppdatertSøknad)
            val nySøknad = omsorgspengerKroniskSyktBarnSøknadDto.copy(
                journalposter = journalposter.values.toList().filterIsInstance<String>(),
                k9saksnummer = søknadEntitet.k9saksnummer
            )
            Pair(oppdatertSøknad, nySøknad)
        } else {
            null
        }
    }

    suspend fun utfyllendeInnsendingOmsMA(
        omsorgspengerMidlertidigAleneSøknadDto: OmsorgspengerMidlertidigAleneSøknadDto,
        saksbehandler: String,
    ): Pair<SøknadEntitet, OmsorgspengerMidlertidigAleneSøknadDto>? {
        val søknadEntitet = soknadService.hentSøknad(omsorgspengerMidlertidigAleneSøknadDto.soeknadId)!!
        return if (søknadEntitet.sendtInn.not()) {
            val journalposter =
                leggTilJournalpost(omsorgspengerMidlertidigAleneSøknadDto.journalposter, søknadEntitet.journalposter)
            val søknadJson = objectMapper().convertValue<JsonB>(omsorgspengerMidlertidigAleneSøknadDto)
            val oppdatertSøknad =
                søknadEntitet.copy(søknad = søknadJson, journalposter = journalposter, endret_av = saksbehandler)
            soknadService.oppdaterSøknad(oppdatertSøknad)
            val nySøknad = omsorgspengerMidlertidigAleneSøknadDto.copy(
                journalposter = journalposter.values.toList().filterIsInstance<String>(),
                k9saksnummer = søknadEntitet.k9saksnummer
            )
            Pair(oppdatertSøknad, nySøknad)
        } else {
            null
        }
    }

    suspend fun utfyllendeInnsendingOmsAO(
        dto: OmsorgspengerAleneOmsorgSøknadDto,
        saksbehandler: String,
    ): Pair<SøknadEntitet, OmsorgspengerAleneOmsorgSøknadDto>? {
        val søknadEntitet = soknadService.hentSøknad(dto.soeknadId)!!
        return if (søknadEntitet.sendtInn.not()) {
            val journalposter = leggTilJournalpost(dto.journalposter, søknadEntitet.journalposter)
            val søknadJson = objectMapper().convertValue<JsonB>(dto)
            val oppdatertSøknad =
                søknadEntitet.copy(søknad = søknadJson, journalposter = journalposter, endret_av = saksbehandler)
            soknadService.oppdaterSøknad(oppdatertSøknad)
            val nySøknad = dto.copy(
                journalposter = journalposter.values.toList().filterIsInstance<String>(),
                k9saksnummer = søknadEntitet.k9saksnummer
            )
            Pair(oppdatertSøknad, nySøknad)
        } else {
            null
        }
    }

    private fun leggTilJournalpost(
        nyeJournalposter: List<String>?,
        fraDatabasen: JsonB?,
    ): MutableMap<String, Any?> {
        if (fraDatabasen != null) {
            val list = fraDatabasen["journalposter"] as List<*>
            val set = list.toSet()
            val toSet = nyeJournalposter?.flatMap { set.plus(it) }?.toSet()

            fraDatabasen.replace("journalposter", toSet)
            return fraDatabasen
        }
        val jPoster = mutableMapOf<String, Any?>()
        jPoster["journalposter"] = listOf(nyeJournalposter)
        return jPoster
    }

    private suspend fun henterMappeMedAlleKoblinger(
        mappeId: String?,
        søker: Person,
        søknadId: String? = null,
    ): Mappe {
        val alleBunker = bunkeRepository.hentAlleBunkerForMappe(mappeId!!)
        val bunkerId = alleBunker.map { b -> b.bunkeId }.toList()
        val hentAlleSøknaderForBunker = bunkerId.flatMap { id -> soknadService.hentAlleSøknaderForBunke(id) }

        if (søknadId != null) {
            val harFeilet = hentAlleSøknaderForBunker.map { s -> s.søknadId }.contains(søknadId).not()
            if (harFeilet) {
                throw IllegalStateException("klarte ikke hente opp nettopp berørt søknad")
            }
        }
        val bunkerMedSøknader = alleBunker.map { b ->
            BunkeEntitet(
                b.bunkeId,
                b.punsjFagsakYtelseType,
                hentAlleSøknaderForBunker.filter { s -> s.bunkeId == b.bunkeId }.toList()
            )
        }
        return Mappe(mappeId, søker, bunkerMedSøknader)
    }
}

private data class Søknadfelles(
    val søknadsId: UUID,
    val journalposter: MutableMap<String, Any?>,
    val mottattDato: LocalDateTime?,
    val klokkeslett: LocalTime?,
)

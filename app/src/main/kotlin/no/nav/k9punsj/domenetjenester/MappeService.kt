package no.nav.k9punsj.domenetjenester

import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9punsj.db.datamodell.*
import no.nav.k9punsj.db.repository.BunkeRepository
import no.nav.k9punsj.db.repository.MappeRepository
import no.nav.k9punsj.db.repository.SøknadRepository
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.OpprettNySøknad
import no.nav.k9punsj.rest.web.dto.*
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

@Service
class MappeService(
    val mappeRepository: MappeRepository,
    val søknadRepository: SøknadRepository,
    val bunkeRepository: BunkeRepository,
    val personService: PersonService,
    val journalpostRepository: JournalpostRepository,
) {

    suspend fun hent(mappeId: MappeId): Mappe? {
        val hentEierAvMappe = mappeRepository.hentEierAvMappe(mappeId)
        if (hentEierAvMappe != null) {
            return henterMappeMedAlleKoblinger(mappeId,
                personService.finnPerson(hentEierAvMappe))

        }
        return null
    }

    suspend fun hentMappe(person: Person): Mappe {
        return henterMappeMedAlleKoblinger(mappeRepository.opprettEllerHentMappeForPerson(person.personId), person)
    }

    suspend fun førsteInnsendingPsb(nySøknad: OpprettNySøknad): SøknadEntitet {
        val norskIdent = nySøknad.norskIdent
        val barnIdent = nySøknad.barnIdent
        val søker = personService.finnEllerOpprettPersonVedNorskIdent(norskIdent)

        val mappeId = mappeRepository.opprettEllerHentMappeForPerson(søker.personId)
        val bunkeId = bunkeRepository.opprettEllerHentBunkeForFagsakType(mappeId, FagsakYtelseType.PLEIEPENGER_SYKT_BARN)
        val søknadfelles = felles(nySøknad)
        val pleiepengerSøknadDto =
            PleiepengerSyktBarnSøknadDto(
                soeknadId = søknadfelles.søknadsId.toString(),
                soekerId = norskIdent,
                barn = PleiepengerSyktBarnSøknadDto.BarnDto(barnIdent, null),
                journalposter = listOf(nySøknad.journalpostId),
                mottattDato = søknadfelles.mottattDato?.toLocalDate(),
                klokkeslett = søknadfelles.klokkeslett,
                harInfoSomIkkeKanPunsjes = false,
                harMedisinskeOpplysninger = false
            )

        return opprettSøknadEntitet(søknadfelles.søknadsId, bunkeId, søker, søknadfelles.journalposter, pleiepengerSøknadDto)
    }

    suspend fun forsteInnsendingPls(nySøknad: OpprettNySøknad): SøknadEntitet {
        val norskIdent = nySøknad.norskIdent
        val barnIdent = nySøknad.pleietrengendeIdent
        val soker = personService.finnEllerOpprettPersonVedNorskIdent(norskIdent)

        val mappeId = mappeRepository.opprettEllerHentMappeForPerson(soker.personId)
        val bunkeId =
            bunkeRepository.opprettEllerHentBunkeForFagsakType(mappeId, FagsakYtelseType.PLEIEPENGER_LIVETS_SLUTTFASE)
        val soknadfelles = felles(nySøknad)
        val pleiepengerLivetsSluttfaseSoknadDto = PleiepengerLivetsSluttfaseSøknadDto(
            soeknadId = soknadfelles.søknadsId.toString(),
            soekerId = norskIdent,
            pleietrengende = PleiepengerLivetsSluttfaseSøknadDto.PleietrengendeDto(barnIdent),
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
            pleiepengerLivetsSluttfaseSoknadDto
        )
    }

    private suspend fun felles(nySøknad: OpprettNySøknad): Søknadfelles {
        val søknadId = UUID.randomUUID()

        val journalposter: MutableMap<String, Any?> = mutableMapOf()
        journalposter["journalposter"] = listOf(nySøknad.journalpostId)

        val mottattDato: LocalDateTime? = hentTidligsteMottattDatoFraJournalposter(nySøknad.journalpostId)
        val klokkeslett: LocalTime? =
            if (mottattDato?.toLocalTime() != null) LocalTime.of(mottattDato.toLocalTime().hour,
                mottattDato.toLocalTime().minute) else null

        return Søknadfelles(søknadId, journalposter, mottattDato, klokkeslett)
    }

    suspend fun førsteInnsendingOms(nySøknad: OpprettNySøknad): SøknadEntitet {
        val norskIdent = nySøknad.norskIdent
        val søker = personService.finnEllerOpprettPersonVedNorskIdent(norskIdent)
        val mappeId = mappeRepository.opprettEllerHentMappeForPerson(søker.personId)
        val bunkeId = bunkeRepository.opprettEllerHentBunkeForFagsakType(mappeId, FagsakYtelseType.OMSORGSPENGER)

        val søknadfelles = felles(nySøknad)
        val dto = OmsorgspengerSøknadDto(
                soeknadId = søknadfelles.søknadsId.toString(),
                journalposter = listOf(nySøknad.journalpostId),
                mottattDato = søknadfelles.mottattDato?.toLocalDate(),
                klokkeslett = søknadfelles.klokkeslett,
                soekerId = norskIdent
            )
        return opprettSøknadEntitet(søknadfelles.søknadsId, bunkeId, søker, søknadfelles.journalposter, dto)
    }

    suspend fun førsteInnsendingOmsKSB(nySøknad: OpprettNySøknad): SøknadEntitet {
        val norskIdent = nySøknad.norskIdent
        val søker = personService.finnEllerOpprettPersonVedNorskIdent(norskIdent)
        val mappeId = mappeRepository.opprettEllerHentMappeForPerson(søker.personId)
        val bunkeId = bunkeRepository.opprettEllerHentBunkeForFagsakType(mappeId, FagsakYtelseType.OMSORGSPENGER_KRONISK_SYKT_BARN)

        val søknadfelles = felles(nySøknad)
        val dto = OmsorgspengerKroniskSyktBarnSøknadDto(
                soeknadId = søknadfelles.søknadsId.toString(),
                journalposter = listOf(nySøknad.journalpostId),
                mottattDato = søknadfelles.mottattDato?.toLocalDate(),
                klokkeslett = søknadfelles.klokkeslett,
                soekerId = norskIdent,
                harInfoSomIkkeKanPunsjes = false,
                harMedisinskeOpplysninger = false
            )
        return opprettSøknadEntitet(søknadfelles.søknadsId, bunkeId, søker, søknadfelles.journalposter, dto)
    }

    suspend fun førsteInnsendingOmsAO(nySøknad: OpprettNySøknad): SøknadEntitet {
        val norskIdent = nySøknad.norskIdent
        val søker = personService.finnEllerOpprettPersonVedNorskIdent(norskIdent)
        val mappeId = mappeRepository.opprettEllerHentMappeForPerson(søker.personId)
        val bunkeId = bunkeRepository.opprettEllerHentBunkeForFagsakType(mappeId, FagsakYtelseType.OMSORGSPENGER_ALENE_OMSORGEN)

        val søknadfelles = felles(nySøknad)
        val dto = OmsorgspengerAleneOmsorgSøknadDto(
            soeknadId = søknadfelles.søknadsId.toString(),
            journalposter = listOf(nySøknad.journalpostId),
            mottattDato = søknadfelles.mottattDato?.toLocalDate(),
            klokkeslett = søknadfelles.klokkeslett,
            soekerId = norskIdent
        )
        return opprettSøknadEntitet(søknadfelles.søknadsId, bunkeId, søker, søknadfelles.journalposter, dto)
    }

    private suspend fun opprettSøknadEntitet(
        søknadId: UUID,
        bunkeId: BunkeId,
        søker: Person,
        journalposter: MutableMap<String, Any?>,
        dto: Any,
    ): SøknadEntitet {
        val søknadEntitet = SøknadEntitet(
            søknadId = søknadId.toString(),
            bunkeId = bunkeId,
            søkerId = søker.personId,
            journalposter = journalposter,
            søknad = objectMapper().convertValue<JsonB>(dto)
        )
        return søknadRepository.opprettSøknad(søknadEntitet)
    }

    private suspend fun hentTidligsteMottattDatoFraJournalposter(journalpostIdDto: JournalpostIdDto): LocalDateTime? {
        return journalpostRepository.hentHvis(journalpostIdDto)?.mottattDato
    }

    suspend fun utfyllendeInnsendingPsb(
        pleiepengerSøknadDto: PleiepengerSyktBarnSøknadDto,
        saksbehandler: String,
    ): Pair<SøknadEntitet, PleiepengerSyktBarnSøknadDto>? {
        val hentSøknad = søknadRepository.hentSøknad(pleiepengerSøknadDto.soeknadId)!!
        return if (hentSøknad.sendtInn.not()) {
            val journalposter = leggTilJournalpost(pleiepengerSøknadDto.journalposter, hentSøknad.journalposter)
            val søknadJson = objectMapper().convertValue<JsonB>(pleiepengerSøknadDto)
            val oppdatertSøknad =
                hentSøknad.copy(søknad = søknadJson, journalposter = journalposter, endret_av = saksbehandler)
            søknadRepository.oppdaterSøknad(oppdatertSøknad)

            val nySøknad = pleiepengerSøknadDto.copy(journalposter = journalposter.values.toList()
                .filterIsInstance<JournalpostIdDto>())
            Pair(oppdatertSøknad, nySøknad)
        } else {
            null
        }
    }

    suspend fun utfyllendeInnsendingPls(
        dto: PleiepengerLivetsSluttfaseSøknadDto,
        saksbehandler: String,
    ): Pair<SøknadEntitet, PleiepengerLivetsSluttfaseSøknadDto>? {
        val hentSøknad = søknadRepository.hentSøknad(dto.soeknadId)!!
        return if (hentSøknad.sendtInn.not()) {
            val journalposter = leggTilJournalpost(dto.journalposter, hentSøknad.journalposter)
            val søknadJson = objectMapper().convertValue<JsonB>(dto)
            val oppdatertSøknad =
                hentSøknad.copy(søknad = søknadJson, journalposter = journalposter, endret_av = saksbehandler)
            søknadRepository.oppdaterSøknad(oppdatertSøknad)

            val nySøknad = dto.copy(journalposter = journalposter.values.toList()
                .filterIsInstance<JournalpostIdDto>())
            Pair(oppdatertSøknad, nySøknad)
        } else {
            null
        }
    }

    suspend fun utfyllendeInnsendingOms(
        omsorgspengerSøknadDto: OmsorgspengerSøknadDto,
        saksbehandler: String,
    ): Pair<SøknadEntitet, OmsorgspengerSøknadDto>? {
        val hentSøknad = søknadRepository.hentSøknad(omsorgspengerSøknadDto.soeknadId)!!
        return if (hentSøknad.sendtInn.not()) {
            val journalposter = leggTilJournalpost(omsorgspengerSøknadDto.journalposter, hentSøknad.journalposter)
            val søknadJson = objectMapper().convertValue<JsonB>(omsorgspengerSøknadDto)
            val oppdatertSøknad =
                hentSøknad.copy(søknad = søknadJson, journalposter = journalposter, endret_av = saksbehandler)
            søknadRepository.oppdaterSøknad(oppdatertSøknad)
            val nySøknad = omsorgspengerSøknadDto.copy(journalposter = journalposter.values.toList().filterIsInstance<JournalpostIdDto>())
            Pair(oppdatertSøknad, nySøknad)
        } else {
            null
        }
    }

    suspend fun utfyllendeInnsendingOmsKSB(
        omsorgspengerKroniskSyktBarnSøknadDto: OmsorgspengerKroniskSyktBarnSøknadDto,
        saksbehandler: String,
    ): Pair<SøknadEntitet, OmsorgspengerKroniskSyktBarnSøknadDto>? {
        val hentSøknad = søknadRepository.hentSøknad(omsorgspengerKroniskSyktBarnSøknadDto.soeknadId)!!
        return if (hentSøknad.sendtInn.not()) {
            val journalposter = leggTilJournalpost(omsorgspengerKroniskSyktBarnSøknadDto.journalposter, hentSøknad.journalposter)
            val søknadJson = objectMapper().convertValue<JsonB>(omsorgspengerKroniskSyktBarnSøknadDto)
            val oppdatertSøknad =
                hentSøknad.copy(søknad = søknadJson, journalposter = journalposter, endret_av = saksbehandler)
            søknadRepository.oppdaterSøknad(oppdatertSøknad)
            val nySøknad = omsorgspengerKroniskSyktBarnSøknadDto.copy(journalposter = journalposter.values.toList().filterIsInstance<JournalpostIdDto>())
            Pair(oppdatertSøknad, nySøknad)
        } else {
            null
        }
    }

    suspend fun utfyllendeInnsendingOmsMA(
        omsorgspengerMidlertidigAleneSøknadDto: OmsorgspengerMidlertidigAleneSøknadDto,
        saksbehandler: String,
    ): Pair<SøknadEntitet, OmsorgspengerMidlertidigAleneSøknadDto>? {
        val hentSøknad = søknadRepository.hentSøknad(omsorgspengerMidlertidigAleneSøknadDto.soeknadId)!!
        return if (hentSøknad.sendtInn.not()) {
            val journalposter = leggTilJournalpost(omsorgspengerMidlertidigAleneSøknadDto.journalposter, hentSøknad.journalposter)
            val søknadJson = objectMapper().convertValue<JsonB>(omsorgspengerMidlertidigAleneSøknadDto)
            val oppdatertSøknad =
                hentSøknad.copy(søknad = søknadJson, journalposter = journalposter, endret_av = saksbehandler)
            søknadRepository.oppdaterSøknad(oppdatertSøknad)
            val nySøknad = omsorgspengerMidlertidigAleneSøknadDto.copy(journalposter = journalposter.values.toList().filterIsInstance<JournalpostIdDto>())
            Pair(oppdatertSøknad, nySøknad)
        } else {
            null
        }
    }

    suspend fun utfyllendeInnsendingOmsAO(
        dto: OmsorgspengerAleneOmsorgSøknadDto,
        saksbehandler: String,
    ): Pair<SøknadEntitet, OmsorgspengerAleneOmsorgSøknadDto>? {
        val hentSøknad = søknadRepository.hentSøknad(dto.soeknadId)!!
        return if (hentSøknad.sendtInn.not()) {
            val journalposter = leggTilJournalpost(dto.journalposter, hentSøknad.journalposter)
            val søknadJson = objectMapper().convertValue<JsonB>(dto)
            val oppdatertSøknad =
                hentSøknad.copy(søknad = søknadJson, journalposter = journalposter, endret_av = saksbehandler)
            søknadRepository.oppdaterSøknad(oppdatertSøknad)
            val nySøknad = dto.copy(journalposter = journalposter.values.toList().filterIsInstance<JournalpostIdDto>())
            Pair(oppdatertSøknad, nySøknad)
        } else {
            null
        }
    }

    private fun leggTilJournalpost(
        nyeJournalposter: List<JournalpostIdDto>?,
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
        mappeId: MappeId?,
        søker: Person,
        søknadId: SøknadId? = null,
    ): Mappe {
        val alleBunker = bunkeRepository.hentAlleBunkerForMappe(mappeId!!)
        val bunkerId = alleBunker.map { b -> b.bunkeId }.toList()
        val hentAlleSøknaderForBunker = bunkerId.flatMap { id -> søknadRepository.hentAlleSøknaderForBunke(id) }

        if (søknadId != null) {
            val harFeilet = hentAlleSøknaderForBunker.map { s -> s.søknadId }.contains(søknadId).not()
            if (harFeilet) {
                throw IllegalStateException("klarte ikke hente opp nettopp berørt søknad")
            }
        }
        val bunkerMedSøknader = alleBunker.map { b ->
            BunkeEntitet(b.bunkeId,
                b.fagsakYtelseType,
                hentAlleSøknaderForBunker.filter { s -> s.bunkeId == b.bunkeId }.toList())
        }
        return Mappe(mappeId, søker, bunkerMedSøknader)
    }

    suspend fun hentSøknad(søknad: SøknadIdDto): SøknadEntitet? {
        return søknadRepository.hentSøknad(søknad)
    }
}

data class Søknadfelles(
    val søknadsId: UUID,
    val journalposter: MutableMap<String, Any?>,
    val mottattDato: LocalDateTime?,
    val klokkeslett: LocalTime?,
)

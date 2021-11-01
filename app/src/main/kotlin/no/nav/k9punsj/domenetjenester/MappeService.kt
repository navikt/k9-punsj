package no.nav.k9punsj.domenetjenester

import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9punsj.db.datamodell.*
import no.nav.k9punsj.db.repository.BunkeRepository
import no.nav.k9punsj.db.repository.MappeRepository
import no.nav.k9punsj.db.repository.SøknadRepository
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.OpprettNySøknad
import no.nav.k9punsj.rest.web.dto.JournalpostIdDto
import no.nav.k9punsj.rest.web.dto.OmsorgspengerSøknadDto
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadDto
import no.nav.k9punsj.rest.web.dto.SøknadIdDto
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

    suspend fun hentMappe(person: Person, søknadType: FagsakYtelseType): Mappe {
        return henterMappeMedAlleKoblinger(mappeRepository.opprettEllerHentMappeForPerson(person.personId), person)
    }

    suspend fun førsteInnsendingPsb(nySøknad: OpprettNySøknad): SøknadEntitet {
        val norskIdent = nySøknad.norskIdent
        val barnIdent = nySøknad.barnIdent
        val søker = personService.finnEllerOpprettPersonVedNorskIdent(norskIdent)

        val mappeId = mappeRepository.opprettEllerHentMappeForPerson(søker.personId)
        val bunkeId = bunkeRepository.opprettEllerHentBunkeForFagsakType(mappeId, FagsakYtelseType.PLEIEPENGER_SYKT_BARN)
        val søknadId = UUID.randomUUID()

        val journalposter = mutableMapOf<String, Any?>()
        journalposter["journalposter"] = listOf(nySøknad.journalpostId)

        val mottattDato = hentTidligsteMottattDatoFraJournalposter(nySøknad.journalpostId)
        val klokkeslett = if (mottattDato?.toLocalTime() != null) LocalTime.of(mottattDato.toLocalTime().hour, mottattDato.toLocalTime().minute) else null
        val pleiepengerSøknadDto =
            PleiepengerSøknadDto(
                soeknadId = søknadId.toString(),
                soekerId = norskIdent,
                barn = PleiepengerSøknadDto.BarnDto(barnIdent, null),
                journalposter = listOf(nySøknad.journalpostId),
                mottattDato = mottattDato?.toLocalDate(),
                klokkeslett = klokkeslett,
                harInfoSomIkkeKanPunsjes = false,
                harMedisinskeOpplysninger = false
            )

        return opprettSøknadEntitet(søknadId, bunkeId, søker, journalposter, pleiepengerSøknadDto)
    }

    suspend fun førsteInnsendingOms(nySøknad: OpprettNySøknad): SøknadEntitet {
        val norskIdent = nySøknad.norskIdent
        val barnIdent = nySøknad.barnIdent
        val søker = personService.finnEllerOpprettPersonVedNorskIdent(norskIdent)

        val mappeId = mappeRepository.opprettEllerHentMappeForPerson(søker.personId)
        val bunkeId = bunkeRepository.opprettEllerHentBunkeForFagsakType(mappeId, FagsakYtelseType.OMSORGSPENGER)
        val søknadId = UUID.randomUUID()

        val journalposter = mutableMapOf<String, Any?>()
        journalposter["journalposter"] = listOf(nySøknad.journalpostId)

        val mottattDato = hentTidligsteMottattDatoFraJournalposter(nySøknad.journalpostId)
        val klokkeslett = if (mottattDato?.toLocalTime() != null) LocalTime.of(mottattDato.toLocalTime().hour, mottattDato.toLocalTime().minute) else null
        val dto =
            OmsorgspengerSøknadDto(
                soeknadId = søknadId.toString(),
                journalposter = listOf(nySøknad.journalpostId),
            )

        return opprettSøknadEntitet(søknadId, bunkeId, søker, journalposter, dto)
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
        søknad: PleiepengerSøknadDto,
        saksbehandler: String,
    ): Pair<SøknadEntitet, PleiepengerSøknadDto>? {
        val hentSøknad = søknadRepository.hentSøknad(søknad.soeknadId)!!
        return if (hentSøknad.sendtInn.not()) {
            val journalposter = leggTilJournalpost(søknad.journalposter, hentSøknad.journalposter)
            val søknadJson = objectMapper().convertValue<JsonB>(søknad)
            val oppdatertSøknad =
                hentSøknad.copy(søknad = søknadJson, journalposter = journalposter, endret_av = saksbehandler)
            søknadRepository.oppdaterSøknad(oppdatertSøknad)
            val nySøknad = søknad.copy(journalposter = journalposter.values.toList() as List<JournalpostIdDto>)
            Pair(oppdatertSøknad, nySøknad)
        } else {
            null
        }
    }

    suspend fun utfyllendeInnsendingOms(
        søknad: OmsorgspengerSøknadDto,
        saksbehandler: String,
    ): Pair<SøknadEntitet, OmsorgspengerSøknadDto>? {
        val hentSøknad = søknadRepository.hentSøknad(søknad.soeknadId)!!
        return if (hentSøknad.sendtInn.not()) {
            val journalposter = leggTilJournalpost(søknad.journalposter, hentSøknad.journalposter)
            val søknadJson = objectMapper().convertValue<JsonB>(søknad)
            val oppdatertSøknad =
                hentSøknad.copy(søknad = søknadJson, journalposter = journalposter, endret_av = saksbehandler)
            søknadRepository.oppdaterSøknad(oppdatertSøknad)
            val nySøknad = søknad.copy(journalposter = journalposter.values.toList() as List<JournalpostIdDto>)
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

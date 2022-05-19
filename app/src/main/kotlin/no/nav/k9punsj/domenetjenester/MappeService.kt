package no.nav.k9punsj.domenetjenester

import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9punsj.domenetjenester.repository.BunkeRepository
import no.nav.k9punsj.domenetjenester.repository.MappeRepository
import no.nav.k9punsj.omsorgspengeraleneomsorg.OmsorgspengerAleneOmsorgSøknadDto
import no.nav.k9punsj.omsorgspengerkronisksyktbarn.OmsorgspengerKroniskSyktBarnSøknadDto
import no.nav.k9punsj.omsorgspengermidlertidigalene.OmsorgspengerMidlertidigAleneSøknadDto
import no.nav.k9punsj.korrigeringinntektsmelding.KorrigeringInntektsmeldingDto
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.JsonB
import no.nav.k9punsj.felles.dto.*
import no.nav.k9punsj.pleiepengerlivetssluttfase.PleiepengerLivetsSluttfaseSøknadDto
import no.nav.k9punsj.pleiepengersyktbarn.PleiepengerSyktBarnSøknadDto
import no.nav.k9punsj.journalpost.JournalpostService
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.omsorgspengermidlertidigalene.NyOmsMASøknad
import no.nav.k9punsj.omsorgspengerutbetaling.OmsorgspengerutbetalingSøknadDto
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

@Service
class MappeService(
    private val mappeRepository: MappeRepository,
    private val soknadService: SoknadService,
    private val bunkeRepository: BunkeRepository,
    private val personService: PersonService,
    private val journalpostService: JournalpostService
) {

    suspend fun hentMappe(person: Person): Mappe {
        return henterMappeMedAlleKoblinger(mappeRepository.opprettEllerHentMappeForPerson(person.personId), person)
    }

    suspend fun førsteInnsendingPsb(nySøknad: OpprettNySøknad): SøknadEntitet {
        val norskIdent = nySøknad.norskIdent
        val barnIdent = nySøknad.barnIdent
        val søker = personService.finnEllerOpprettPersonVedNorskIdent(norskIdent)

        val mappeId = mappeRepository.opprettEllerHentMappeForPerson(søker.personId)
        val bunkeId = bunkeRepository.opprettEllerHentBunkeForFagsakType(mappeId, FagsakYtelseType.PLEIEPENGER_SYKT_BARN)
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
                harMedisinskeOpplysninger = false
            )

        return opprettSøknadEntitet(søknadfelles.søknadsId, bunkeId, søker, søknadfelles.journalposter, pleiepengerSøknadDto)
    }

    suspend fun forsteInnsendingPls(nySøknad: OpprettNySøknad): SøknadEntitet {
        val norskIdent = nySøknad.norskIdent
        val pleietrengendeIdent = nySøknad.pleietrengendeIdent
        val soker = personService.finnEllerOpprettPersonVedNorskIdent(norskIdent)

        val mappeId = mappeRepository.opprettEllerHentMappeForPerson(soker.personId)
        val bunkeId =
            bunkeRepository.opprettEllerHentBunkeForFagsakType(mappeId, FagsakYtelseType.PLEIEPENGER_LIVETS_SLUTTFASE)
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
            pleiepengerLivetsSluttfaseSoknadDto
        )
    }

    private suspend fun felles(journalpostId: String): Søknadfelles {
        val søknadId = UUID.randomUUID()

        val journalposter: MutableMap<String, Any?> = mutableMapOf()
        journalposter["journalposter"] = listOf(journalpostId)

        val mottattDato: LocalDateTime? = hentTidligsteMottattDatoFraJournalposter(journalpostId)
        val klokkeslett: LocalTime? =
            if (mottattDato?.toLocalTime() != null) LocalTime.of(mottattDato.toLocalTime().hour,
                mottattDato.toLocalTime().minute) else null

        return Søknadfelles(søknadId, journalposter, mottattDato, klokkeslett)
    }

    suspend fun førsteInnsendingKorrigeringIm(nySøknad: OpprettNySøknad): SøknadEntitet {
        val norskIdent = nySøknad.norskIdent
        val søker = personService.finnEllerOpprettPersonVedNorskIdent(norskIdent)
        val mappeId = mappeRepository.opprettEllerHentMappeForPerson(søker.personId)
        val bunkeId = bunkeRepository.opprettEllerHentBunkeForFagsakType(mappeId, FagsakYtelseType.OMSORGSPENGER)

        val søknadfelles = felles(nySøknad.journalpostId)
        val dto = KorrigeringInntektsmeldingDto(
                soeknadId = søknadfelles.søknadsId.toString(),
                journalposter = listOf(nySøknad.journalpostId),
                mottattDato = søknadfelles.mottattDato?.toLocalDate(),
                klokkeslett = søknadfelles.klokkeslett,
                soekerId = norskIdent
            )
        return opprettSøknadEntitet(søknadfelles.søknadsId, bunkeId, søker, søknadfelles.journalposter, dto)
    }

    suspend fun førsteInnsendingOmsorgspengerutbetaling(nySøknad: OpprettNySøknad): SøknadEntitet {
        val norskIdent = nySøknad.norskIdent
        val søker = personService.finnEllerOpprettPersonVedNorskIdent(norskIdent)
        val mappeId = mappeRepository.opprettEllerHentMappeForPerson(søker.personId)
        val bunkeId = bunkeRepository.opprettEllerHentBunkeForFagsakType(mappeId, FagsakYtelseType.OMSORGSPENGERUTBETALING)

        val søknadfelles = felles(nySøknad.journalpostId)
        val dto = OmsorgspengerutbetalingSøknadDto(
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

        val søknadfelles = felles(nySøknad.journalpostId)
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

        val søknadfelles = felles(nySøknad.journalpostId)
        val dto = OmsorgspengerAleneOmsorgSøknadDto(
            soeknadId = søknadfelles.søknadsId.toString(),
            journalposter = listOf(nySøknad.journalpostId),
            mottattDato = søknadfelles.mottattDato?.toLocalDate(),
            klokkeslett = søknadfelles.klokkeslett,
            soekerId = norskIdent
        )
        return opprettSøknadEntitet(søknadfelles.søknadsId, bunkeId, søker, søknadfelles.journalposter, dto)
    }

    suspend fun førsteInnsendingOmsMA(nySøknad: NyOmsMASøknad): SøknadEntitet {
        val norskIdent = nySøknad.norskIdent
        val søker = personService.finnEllerOpprettPersonVedNorskIdent(norskIdent)
        val mappeId = mappeRepository.opprettEllerHentMappeForPerson(søker.personId)
        val bunkeId = bunkeRepository.opprettEllerHentBunkeForFagsakType(mappeId, FagsakYtelseType.OMSORGSPENGER_MIDLERTIDIG_ALENE)

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
            )
        )
        return opprettSøknadEntitet(søknadfelles.søknadsId, bunkeId, søker, søknadfelles.journalposter, dto)
    }

    private suspend fun opprettSøknadEntitet(
        søknadId: UUID,
        bunkeId: String,
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
        return soknadService.opprettSøknad(søknadEntitet)
    }

    private suspend fun hentTidligsteMottattDatoFraJournalposter(journalpostIdDto: String): LocalDateTime? {
        return journalpostService.hentHvisJournalpostMedId(journalpostIdDto)?.mottattDato
    }

    suspend fun utfyllendeInnsendingPsb(
        pleiepengerSøknadDto: PleiepengerSyktBarnSøknadDto,
        saksbehandler: String,
    ): Pair<SøknadEntitet, PleiepengerSyktBarnSøknadDto>? {
        val hentSøknad = soknadService.hentSøknad(pleiepengerSøknadDto.soeknadId)!!
        return if (hentSøknad.sendtInn.not()) {
            val journalposter = leggTilJournalpost(pleiepengerSøknadDto.journalposter, hentSøknad.journalposter)
            val søknadJson = objectMapper().convertValue<JsonB>(pleiepengerSøknadDto)
            val oppdatertSøknad =
                hentSøknad.copy(søknad = søknadJson, journalposter = journalposter, endret_av = saksbehandler)
            soknadService.oppdaterSøknad(oppdatertSøknad)

            val nySøknad = pleiepengerSøknadDto.copy(journalposter = journalposter.values.toList()
                .filterIsInstance<String>())
            Pair(oppdatertSøknad, nySøknad)
        } else {
            null
        }
    }

    suspend fun utfyllendeInnsendingPls(
        dto: PleiepengerLivetsSluttfaseSøknadDto,
        saksbehandler: String,
    ): Pair<SøknadEntitet, PleiepengerLivetsSluttfaseSøknadDto>? {
        val hentSøknad = soknadService.hentSøknad(dto.soeknadId)!!
        return if (hentSøknad.sendtInn.not()) {
            val journalposter = leggTilJournalpost(dto.journalposter, hentSøknad.journalposter)
            val søknadJson = objectMapper().convertValue<JsonB>(dto)
            val oppdatertSøknad =
                hentSøknad.copy(søknad = søknadJson, journalposter = journalposter, endret_av = saksbehandler)
            soknadService.oppdaterSøknad(oppdatertSøknad)

            val nySøknad = dto.copy(journalposter = journalposter.values.toList()
                .filterIsInstance<String>())
            Pair(oppdatertSøknad, nySøknad)
        } else {
            null
        }
    }

    suspend fun utfyllendeInnsendingOms(
        korrigeringInntektsmeldingDto: KorrigeringInntektsmeldingDto,
        saksbehandler: String,
    ): Pair<SøknadEntitet, KorrigeringInntektsmeldingDto>? {
        val hentSøknad = soknadService.hentSøknad(korrigeringInntektsmeldingDto.soeknadId)!!
        return if (hentSøknad.sendtInn.not()) {
            val journalposter = leggTilJournalpost(korrigeringInntektsmeldingDto.journalposter, hentSøknad.journalposter)
            val søknadJson = objectMapper().convertValue<JsonB>(korrigeringInntektsmeldingDto)
            val oppdatertSøknad =
                hentSøknad.copy(søknad = søknadJson, journalposter = journalposter, endret_av = saksbehandler)
            soknadService.oppdaterSøknad(oppdatertSøknad)
            val nySøknad = korrigeringInntektsmeldingDto.copy(journalposter = journalposter.values.toList().filterIsInstance<String>())
            Pair(oppdatertSøknad, nySøknad)
        } else {
            null
        }
    }

    suspend fun utfyllendeInnsendingOmsUt(
        omsorgspengerutbetalingSøknadDto: OmsorgspengerutbetalingSøknadDto,
        saksbehandler: String,
    ): Pair<SøknadEntitet, OmsorgspengerutbetalingSøknadDto>? {
        val hentSøknad = soknadService.hentSøknad(omsorgspengerutbetalingSøknadDto.soeknadId)!!
        return if (hentSøknad.sendtInn.not()) {
            val journalposter = leggTilJournalpost(omsorgspengerutbetalingSøknadDto.journalposter, hentSøknad.journalposter)
            val søknadJson = objectMapper().convertValue<JsonB>(omsorgspengerutbetalingSøknadDto)
            val oppdatertSøknad =
                hentSøknad.copy(søknad = søknadJson, journalposter = journalposter, endret_av = saksbehandler)
            soknadService.oppdaterSøknad(oppdatertSøknad)
            val nySøknad = omsorgspengerutbetalingSøknadDto.copy(journalposter = journalposter.values.toList().filterIsInstance<String>())
            Pair(oppdatertSøknad, nySøknad)
        } else {
            null
        }
    }

    suspend fun utfyllendeInnsendingOmsKSB(
        omsorgspengerKroniskSyktBarnSøknadDto: OmsorgspengerKroniskSyktBarnSøknadDto,
        saksbehandler: String,
    ): Pair<SøknadEntitet, OmsorgspengerKroniskSyktBarnSøknadDto>? {
        val hentSøknad = soknadService.hentSøknad(omsorgspengerKroniskSyktBarnSøknadDto.soeknadId)!!
        return if (hentSøknad.sendtInn.not()) {
            val journalposter = leggTilJournalpost(omsorgspengerKroniskSyktBarnSøknadDto.journalposter, hentSøknad.journalposter)
            val søknadJson = objectMapper().convertValue<JsonB>(omsorgspengerKroniskSyktBarnSøknadDto)
            val oppdatertSøknad =
                hentSøknad.copy(søknad = søknadJson, journalposter = journalposter, endret_av = saksbehandler)
            soknadService.oppdaterSøknad(oppdatertSøknad)
            val nySøknad = omsorgspengerKroniskSyktBarnSøknadDto.copy(journalposter = journalposter.values.toList().filterIsInstance<String>())
            Pair(oppdatertSøknad, nySøknad)
        } else {
            null
        }
    }

    suspend fun utfyllendeInnsendingOmsMA(
        omsorgspengerMidlertidigAleneSøknadDto: OmsorgspengerMidlertidigAleneSøknadDto,
        saksbehandler: String,
    ): Pair<SøknadEntitet, OmsorgspengerMidlertidigAleneSøknadDto>? {
        val hentSøknad = soknadService.hentSøknad(omsorgspengerMidlertidigAleneSøknadDto.soeknadId)!!
        return if (hentSøknad.sendtInn.not()) {
            val journalposter = leggTilJournalpost(omsorgspengerMidlertidigAleneSøknadDto.journalposter, hentSøknad.journalposter)
            val søknadJson = objectMapper().convertValue<JsonB>(omsorgspengerMidlertidigAleneSøknadDto)
            val oppdatertSøknad =
                hentSøknad.copy(søknad = søknadJson, journalposter = journalposter, endret_av = saksbehandler)
            soknadService.oppdaterSøknad(oppdatertSøknad)
            val nySøknad = omsorgspengerMidlertidigAleneSøknadDto.copy(journalposter = journalposter.values.toList().filterIsInstance<String>())
            Pair(oppdatertSøknad, nySøknad)
        } else {
            null
        }
    }

    suspend fun utfyllendeInnsendingOmsAO(
        dto: OmsorgspengerAleneOmsorgSøknadDto,
        saksbehandler: String,
    ): Pair<SøknadEntitet, OmsorgspengerAleneOmsorgSøknadDto>? {
        val hentSøknad = soknadService.hentSøknad(dto.soeknadId)!!
        return if (hentSøknad.sendtInn.not()) {
            val journalposter = leggTilJournalpost(dto.journalposter, hentSøknad.journalposter)
            val søknadJson = objectMapper().convertValue<JsonB>(dto)
            val oppdatertSøknad =
                hentSøknad.copy(søknad = søknadJson, journalposter = journalposter, endret_av = saksbehandler)
            soknadService.oppdaterSøknad(oppdatertSøknad)
            val nySøknad = dto.copy(journalposter = journalposter.values.toList().filterIsInstance<String>())
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
            BunkeEntitet(b.bunkeId,
                b.fagsakYtelseType,
                hentAlleSøknaderForBunker.filter { s -> s.bunkeId == b.bunkeId }.toList())
        }
        return Mappe(mappeId, søker, bunkerMedSøknader)
    }

    suspend fun hentSøknad(søknad: String): SøknadEntitet? {
        return soknadService.hentSøknad(søknad)
    }
}

private data class Søknadfelles(
    val søknadsId: UUID,
    val journalposter: MutableMap<String, Any?>,
    val mottattDato: LocalDateTime?,
    val klokkeslett: LocalTime?,
)

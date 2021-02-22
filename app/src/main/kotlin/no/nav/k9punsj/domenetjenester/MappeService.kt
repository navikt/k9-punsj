package no.nav.k9punsj.domenetjenester

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.k9punsj.db.datamodell.*
import no.nav.k9punsj.db.repository.BunkeRepository
import no.nav.k9punsj.db.repository.MappeRepository
import no.nav.k9punsj.db.repository.SøknadRepository
import no.nav.k9punsj.rest.web.Innsending
import no.nav.k9punsj.rest.web.objectMapper
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class MappeService(
    val mappeRepository: MappeRepository,
    val søknadRepository: SøknadRepository,
    val bunkeRepository: BunkeRepository,
    val personService: PersonService,
) {

    suspend fun hent(mappeId: MappeId): Mappe? {
        val hentEierAvMappe = mappeRepository.hentEierAvMappe(mappeId)
        if (hentEierAvMappe != null) {
            return henterMappeMedAlleKoblinger(mappeId,
                personService.finnPerson(hentEierAvMappe))

        }
        return null
    }

    suspend fun hentMapper(personer: List<Person>, søknadType: FagsakYtelseType): List<Mappe> {
        return personer.map { person ->
            henterMappeMedAlleKoblinger(mappeRepository.opprettEllerHentMappeForPerson(person.personId),
                person)
        }.toList()
    }

    suspend fun førsteInnsending(søknadType: FagsakYtelseType, innsending: Innsending): Mappe {
        val norskIdent = innsending.personer.keys.first()
        val søker = personService.finnEllerOpprettPersonVedNorskIdent(norskIdent)

        val mappeId = mappeRepository.opprettEllerHentMappeForPerson(søker.personId)
        val bunkeId = bunkeRepository.opprettEllerHentBunkeForFagsakType(mappeId, søknadType)
        val søknadId = UUID.randomUUID()
        val søknad = innsending.personer[norskIdent]?.soeknad
        val søknadTre = objectMapper.valueToTree<ObjectNode>(søknad)
        val barnNorskIdent = søknadTre.get("barn")?.get("norskIdentitetsnummer")?.toString()
        val barnBursdag = søknadTre.get("barn")?.get("fødselsdato")?.toString()

        val barnId =
            if (barnNorskIdent != null) personService.finnEllerOpprettPersonVedNorskIdent(barnNorskIdent).personId else null
        val dag = if (barnBursdag != null) java.time.LocalDate.parse(barnBursdag) else null


        val journalposter = mutableMapOf<String, Any?>()
        journalposter["journalposter"] = listOf(innsending.personer[norskIdent]?.journalpostId)


        //TODO(OJR) skal jeg legge på informasjon om hvilken saksbehandler som punsjet denne?
        val søknadEntitet = SøknadEntitet(
            søknadId = søknadId.toString(),
            bunkeId = bunkeId,
            søkerId = søker.personId,
            barnId = barnId,
            barnFødselsdato = dag,
            søknad = søknad as JsonB,
            journalposter = journalposter
        )

        val opprettSøknad = søknadRepository.opprettSøknad(søknadEntitet)

        return henterMappeMedAlleKoblinger(mappeId, søker, opprettSøknad.søknadId)
    }

    //TODO(OJR) skal vi kunne støtte delvis oppdatering går an ved json merging -> SøknadJson.mergeNy
    //skriver over gammel søknad
    suspend fun utfyllendeInnsending(mappeId: MappeId, innsending: Innsending): Mappe? {
        val norskIdent = innsending.personer.keys.first()

        val søknadIdDto = innsending.personer[norskIdent]?.søknadIdDto
        if (søknadIdDto != null) {
            val hentSøknad = søknadRepository.hentSøknad(søknadIdDto)!!

            if (hentSøknad.sendt_inn.not()) {
                val søknad = innsending.personer[norskIdent]?.soeknad
                val journalposter = mutableMapOf<String, Any?>()
                journalposter["journalposter"] = listOf(innsending.personer[norskIdent]?.journalpostId)
                val oppdatertSøknad = hentSøknad.copy(søknad = søknad as JsonB, journalposter = journalposter)
                søknadRepository.oppdaterSøknad(oppdatertSøknad)
                val person = personService.finnEllerOpprettPersonVedNorskIdent(norskIdent)

                return henterMappeMedAlleKoblinger(mappeId, person, søknadIdDto)
            } else {
                throw IllegalStateException("Kan ikke endre på en søknad som er sendt inn")
            }
        } else {
            throw IllegalStateException("Forventer en søknadId")
        }
    }

    private suspend fun henterMappeMedAlleKoblinger(
        mappeId: MappeId,
        søker: Person,
        søknadId: SøknadId? = null,
    ): Mappe {
        val alleBunker = bunkeRepository.hentAlleBunkerForMappe(mappeId)
        val bunkerId = alleBunker.map { b -> b.bunkeId }.toList()
        val hentAlleSøknaderForBunker = bunkerId.flatMap { id -> søknadRepository.hentAlleSøknaderForBunker(id) }

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
}

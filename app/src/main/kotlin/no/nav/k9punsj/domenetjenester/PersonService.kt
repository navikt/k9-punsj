package no.nav.k9punsj.domenetjenester

import no.nav.k9punsj.db.datamodell.AktørId
import no.nav.k9punsj.db.datamodell.NorskIdent
import no.nav.k9punsj.db.datamodell.Person
import no.nav.k9punsj.db.datamodell.PersonId
import no.nav.k9punsj.db.repository.PersonRepository
import no.nav.k9punsj.rest.eksternt.pdl.PdlService
import org.springframework.stereotype.Service


@Service
class PersonService(
    val personRepository: PersonRepository,
    val pdlService: PdlService,
) {

    suspend fun finnEllerOpprettPersonVedNorskIdent(norskIdent: NorskIdent): Person {
        val person = personRepository.hentPersonVedPersonIdent(norskIdent)

        if (person != null) {
            return person
        }
        val pdlResponse = pdlService.identifikator(norskIdent)
        val aktørId = pdlResponse?.identPdl?.data?.hentIdenter?.identer?.get(0)?.ident

        return personRepository.lagre(norskIdent = norskIdent, aktørId = aktørId!!)
    }

    suspend fun finnEllerOpprettPersonVedAktørId(aktørId: AktørId): Person {
        val person = personRepository.hentPersonVedAktørId(aktørId)

        if (person != null) {
            return person
        }
        val pdlResponse = pdlService.identifikatorMedAktørId(aktørId)
        val personIdent = pdlResponse?.identPdl?.data?.hentIdenter?.identer?.get(0)?.ident

        return personRepository.lagre(personIdent!!, aktørId)
    }

    suspend fun finnPerson(personId: PersonId): Person {
        return personRepository.hentPersonVedPersonId(personId)!!
    }

    suspend fun finnPersonVedNorskIdent(norskIdent: NorskIdent): Person? {
        return personRepository.hentPersonVedPersonIdent(norskIdent)
    }

    suspend fun finnPersoner(norskeIdenter: Set<NorskIdent>): List<Person> {
        return norskeIdenter
            .map { norskeIdent -> finnEllerOpprettPersonVedNorskIdent(norskeIdent) }
    }

    suspend fun finnPersonerVedPersonId(personIder: Set<PersonId>): List<Person> {
        return personIder
            .map { personId -> finnPerson(personId) }
    }
}

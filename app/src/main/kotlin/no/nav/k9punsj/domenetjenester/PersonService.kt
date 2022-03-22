package no.nav.k9punsj.domenetjenester

import no.nav.k9punsj.db.datamodell.NorskIdent
import no.nav.k9punsj.db.datamodell.Person
import no.nav.k9punsj.db.datamodell.PersonId
import no.nav.k9punsj.db.repository.PersonRepository
import no.nav.k9punsj.integrasjoner.pdl.PdlService
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
        val aktørId = pdlResponse?.identPdl?.data?.hentIdenter?.identer?.first()?.ident
            ?: throw IllegalStateException("Fant ikke aktørId i PDL")

        return personRepository.lagre(norskIdent = norskIdent, aktørId = aktørId)
    }

    suspend fun finnPerson(personId: PersonId): Person {
        return personRepository.hentPersonVedPersonId(personId)!!
    }

    suspend fun finnPersonVedNorskIdent(norskIdent: NorskIdent): Person? {
        return personRepository.hentPersonVedPersonIdent(norskIdent)
    }

    suspend fun finnPersonVedNorskIdentFørstDbSåPdl(norskIdent: NorskIdent): Person {
        val aktørId = finnAktørId(norskIdent)
        return Person("", norskIdent, aktørId)
    }

    suspend fun finnAktørId(norskIdent: NorskIdent): String {
        val person = personRepository.hentPersonVedPersonIdent(norskIdent)
        if (person != null) {
            return person.aktørId
        }
        val pdlResponse = pdlService.identifikator(norskIdent)
        return pdlResponse?.identPdl?.data?.hentIdenter?.identer?.first()?.ident
            ?: throw IllegalStateException("Fant ikke aktørId i PDL")
    }

}

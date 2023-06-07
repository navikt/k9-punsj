package no.nav.k9punsj.domenetjenester

import no.nav.k9punsj.domenetjenester.repository.PersonRepository
import no.nav.k9punsj.felles.dto.Person
import no.nav.k9punsj.integrasjoner.pdl.PdlService
import org.springframework.stereotype.Service

@Service
class PersonService(
    val personRepository: PersonRepository,
    val pdlService: PdlService
) {

    suspend fun finnEllerOpprettPersonVedNorskIdent(norskIdent: String): Person {
        val person = personRepository.hentPersonVedPersonIdent(norskIdent)

        if (person != null) {
            return person
        }
        val pdlResponse = pdlService.identifikator(norskIdent)
        val aktørId = pdlResponse?.identPdl?.data?.hentIdenter?.identer?.first()?.ident
            ?: throw IllegalStateException("Fant ikke aktørId i PDL")

        return personRepository.lagre(norskIdent = norskIdent, aktørId = aktørId)
    }

    suspend fun finnEllerOpprettPersonVedAktørId(aktørId: String): Person {
        personRepository.hentPersonVedAktørId(aktørId)?.let { return it }

        val pdlResponse = pdlService.identifikatorMedAktørId(aktørId)
        val norskIdent = pdlResponse?.identPdl?.data?.hentIdenter?.identer?.first()?.ident
            ?: throw IllegalStateException("Fant ikke identitetsnummer i PDL")

        return personRepository.lagre(norskIdent = norskIdent, aktørId = aktørId)
    }

    suspend fun finnPerson(personId: String): Person {
        return personRepository.hentPersonVedPersonId(personId)!!
    }

    suspend fun finnPersonVedNorskIdent(norskIdent: String): Person? {
        return personRepository.hentPersonVedPersonIdent(norskIdent)
    }

    suspend fun finnPersonVedNorskIdentFørstDbSåPdl(norskIdent: String): Person {
        val aktørId = finnAktørId(norskIdent)
        return Person("", norskIdent, aktørId)
    }

    suspend fun finnAktørId(norskIdent: String): String {
        personRepository.hentPersonVedPersonIdent(norskIdent)?.let {
            return it.aktørId
        }

        return pdlService.aktørIdFor(norskIdent) ?: throw IllegalStateException("Fant ikke aktørId i PDL")
    }
}

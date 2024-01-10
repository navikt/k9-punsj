package no.nav.k9punsj.pleiepengersyktbarn

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.domenetjenester.repository.MappeRepository
import no.nav.k9punsj.domenetjenester.repository.PersonRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class MappeRepositoryTest: AbstractContainerBaseTest() {

    @Autowired
    lateinit var mappeRepository: MappeRepository

    @Autowired
    lateinit var personRepository: PersonRepository

    private val dummyFnr = "11111111111"
    private val dummyAktørId = "1000000000000"

    @Test
    internal fun HentAlleMapperSomInneholderEnNorskIdent(): Unit = runBlocking {
        val person = personRepository.lagre(norskIdent = dummyFnr, aktørId = dummyAktørId)

        val mappe = mappeRepository.opprettEllerHentMappeForPerson(personId = person.personId)
        assertThat(mappe).isNotNull
    }
}

package no.nav.k9punsj.pleiepengersyktbarn

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.util.DatabaseUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
internal class MappeRepositoryTest {

    val dummyFnr = "11111111111"
    val dummyAktørId = "1000000000000"

    @Test
    internal fun HentAlleMapperSomInneholderEnNorskIdent(): Unit = runBlocking {
        val repository = DatabaseUtil.getMappeRepo()
        val personRepo = DatabaseUtil.getPersonRepo()

        val person = personRepo.lagre(norskIdent = dummyFnr, aktørId = dummyAktørId)

        val mappe = repository.opprettEllerHentMappeForPerson(personId = person.personId)
        assertThat(mappe).isNotNull
    }
}

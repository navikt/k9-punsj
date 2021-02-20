package no.nav.k9punsj.pleiepengersyktbarn

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import no.nav.k9punsj.db.datamodell.Mappe
import no.nav.k9punsj.db.datamodell.NorskIdent
import no.nav.k9punsj.db.datamodell.PersonInfo
import no.nav.k9punsj.util.DatabaseUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.UUID

@ExtendWith(SpringExtension::class)
internal class MappeRepositoryTest {

    @Test
    internal fun HentAlleMapperSomInneholderEnNorskIdent(): Unit = runBlocking {
        val repository = DatabaseUtil.getMappeRepo()
        val mappeId = UUID.randomUUID().toString()

        val m = Mappe(mappeId = mappeId,
                søknadType = FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
                personInfo = hashMapOf("24073125894" to PersonInfo(mutableSetOf("200"), mutableMapOf())))

        repository.oppretteMappe(m)
        val mappe = repository.finneMappe(mappeId)
        assertThat(mappe).isNotNull
    }

    @Test
    internal fun hentMapperMedIdenter(): Unit = runBlocking {
        val repo = DatabaseUtil.getMappeRepo()

        val identer = setOf<NorskIdent>("89485489754745")
        val mappeId = UUID.randomUUID().toString()



        val m = Mappe(mappeId = mappeId,
                søknadType = FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
                personInfo = hashMapOf("89485489754745" to PersonInfo(mutableSetOf("200"), mutableMapOf())))

        repo.oppretteMappe(m)
        val mapper = repo.hent(identer)
        assertThat(mapper).hasSize(1);
    }
}

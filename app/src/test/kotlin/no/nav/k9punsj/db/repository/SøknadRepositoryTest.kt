package no.nav.k9punsj.db.repository

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import no.nav.k9punsj.db.datamodell.SøknadEntitet
import no.nav.k9punsj.util.DatabaseUtil
import no.nav.k9punsj.util.LesFraFilUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import java.util.UUID

@ExtendWith(SpringExtension::class)
internal class SøknadRepositoryTest {

    private val standardIdent = "01122334410"
    private val standardAktørId = "1000000000000"

    private val barnIdent = "01122334412"
    private val barnAktørID = "1000000000001"

    val journalpostid1 = "2948688b-3ee6-4c05-b179-31830dde5069"
    val journalpostid2 = "2948688b-3ee6-4c05-b179-32830dde5069"


    @Test
    fun `Skal lagre søknad`(): Unit = runBlocking {
        val søknadRepository = DatabaseUtil.getSøknadRepo()
        val personRepository = DatabaseUtil.getPersonRepo()
        val bunkeRepository = DatabaseUtil.getBunkeRepo()
        val mappeRepo = DatabaseUtil.getMappeRepo()
        val barnFødselsdato = LocalDate.now()

        //oppretter en person
        val person = personRepository.lagre(norskIdent = standardIdent, aktørId = standardAktørId)
        val barn = personRepository.lagre(norskIdent = barnIdent, aktørId = barnAktørID)
        assertThat(person.personId).isNotNull
        assertThat(barn.personId).isNotNull

        //oppretter en tom mappe til en person
        val mappeId = mappeRepo.opprettEllerHentMappeForPerson(person.personId)

        //oppretter en bunke i mappen for pleiepenger
        val bunkeId = bunkeRepository.opprettEllerHentBunkeForFagsakType(mappeId, FagsakYtelseType.PLEIEPENGER_SYKT_BARN)

        val genererKomplettSøknad = LesFraFilUtil.genererKomplettSøknad()

        val journalposter = mutableMapOf<String, Any?>()
        journalposter["journalposter"] = listOf(journalpostid1, journalpostid2)

        //oppretter en søknad og legger den i bunken
        val søknadEntitet = SøknadEntitet(
            søknadId = UUID.randomUUID().toString(),
            bunkeId = bunkeId,
            søkerId = person.personId,
            barnId = barn.personId,
            barnFødselsdato = barnFødselsdato,
            søknad = genererKomplettSøknad,
            journalposter = journalposter,
            sendtInn = false
        )

        val søknad = søknadRepository.opprettSøknad(søknadEntitet)


        assertThat(søknad.søknad).isNotNull
    }
}

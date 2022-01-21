package no.nav.k9punsj.db.repository

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import no.nav.k9punsj.db.datamodell.SøknadEntitet
import no.nav.k9punsj.util.DatabaseUtil
import no.nav.k9punsj.util.LesFraFilUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import java.util.*

@ExtendWith(SpringExtension::class)
internal class SøknadRepositoryTest {

    private val standardIdent = "01122334410"
    private val standardAktørId = "1000000000000"

    private val barnIdent = "01122334412"
    private val barnAktørID = "1000000000001"

    private val journalpostid1 = "29486889"
    private val journalpostid2 = "29486887"

    val søknadRepository = DatabaseUtil.getSøknadRepo()
    val personRepository = DatabaseUtil.getPersonRepo()
    val bunkeRepository = DatabaseUtil.getBunkeRepo()
    val mappeRepo = DatabaseUtil.getMappeRepo()

    @AfterEach
    internal fun tearDown() {
        DatabaseUtil.cleanSøknadRepo()
        DatabaseUtil.cleanBunkeRepo()
        DatabaseUtil.cleanMappeRepo()
        DatabaseUtil.cleanPersonRepo()
    }

    @Test
    fun `Skal lagre pleiepenger sykt barn søknad`(): Unit = runBlocking {
        opprettOgAssertSøknad(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, LesFraFilUtil.søknadFraFrontend())
    }

    @Test
    fun `Skal lagre omsorgspenger kronisk sykt barn søknad`(): Unit = runBlocking {
        opprettOgAssertSøknad(FagsakYtelseType.OMSORGSPENGER_KRONISK_SYKT_BARN, LesFraFilUtil.søknadUtenBarnFraFrontendOmsKSB())
    }

    private suspend fun opprettOgAssertSøknad(fagsakYtelseType: FagsakYtelseType, søknad: MutableMap<String, Any?>) {
        val barnFødselsdato = LocalDate.now()

        //oppretter en person
        val person = personRepository.lagre(norskIdent = standardIdent, aktørId = standardAktørId)
        val barn = personRepository.lagre(norskIdent = barnIdent, aktørId = barnAktørID)
        assertThat(person.personId).isNotNull
        assertThat(barn.personId).isNotNull

        //oppretter en tom mappe til en person
        val mappeId = mappeRepo.opprettEllerHentMappeForPerson(person.personId)

        //oppretter en bunke i mappen for pleiepenger
        val bunkeId = bunkeRepository.opprettEllerHentBunkeForFagsakType(mappeId, fagsakYtelseType)

        val journalposter = mutableMapOf<String, Any?>()
        journalposter["journalposter"] = listOf(journalpostid1, journalpostid2)

        //oppretter en søknad og legger den i bunken
        val søknadEntitet = SøknadEntitet(
            søknadId = UUID.randomUUID().toString(),
            bunkeId = bunkeId,
            søkerId = person.personId,
            barnId = barn.personId,
            barnFødselsdato = barnFødselsdato,
            søknad = søknad,
            journalposter = journalposter,
            sendtInn = false
        )

        val søknad = søknadRepository.opprettSøknad(søknadEntitet)
        assertThat(søknad.søknad).isNotNull
    }
}

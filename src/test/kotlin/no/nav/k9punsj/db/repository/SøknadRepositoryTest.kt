package no.nav.k9punsj.db.repository

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.domenetjenester.repository.BunkeRepository
import no.nav.k9punsj.domenetjenester.repository.MappeRepository
import no.nav.k9punsj.domenetjenester.repository.PersonRepository
import no.nav.k9punsj.domenetjenester.repository.SøknadRepository
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.dto.SøknadEntitet
import no.nav.k9punsj.util.LesFraFilUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.*


internal class SøknadRepositoryTest: AbstractContainerBaseTest() {

    private val standardIdent = "01122334410"
    private val standardAktørId = "1000000000000"

    private val barnIdent = "01122334412"
    private val barnAktørID = "1000000000001"

    private val journalpostid1 = "29486889"
    private val journalpostid2 = "29486887"

    @Autowired
    lateinit var personRepository: PersonRepository

    @Autowired
    lateinit var mappeRepo: MappeRepository

    @Autowired
    lateinit var søknadRepository: SøknadRepository

    @Autowired
    lateinit var bunkeRepository: BunkeRepository

    @Test
    fun `Skal lagre pleiepenger sykt barn søknad`(): Unit = runBlocking {
        cleanUpDB()
        opprettOgAssertSøknad(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, LesFraFilUtil.søknadFraFrontend())
    }

    @Test
    fun `Skal lagre omsorgspenger kronisk sykt barn søknad`(): Unit = runBlocking {
        cleanUpDB()
        opprettOgAssertSøknad(FagsakYtelseType.OMSORGSPENGER_KRONISK_SYKT_BARN, LesFraFilUtil.søknadUtenBarnFraFrontendOmsKSB())
    }

    private suspend fun opprettOgAssertSøknad(fagsakYtelseType: FagsakYtelseType, søknad: MutableMap<String, Any?>) {
        val barnFødselsdato = LocalDate.now()

        // oppretter en person
        val person = personRepository.lagre(norskIdent = standardIdent, aktørId = standardAktørId)
        val barn = personRepository.lagre(norskIdent = barnIdent, aktørId = barnAktørID)
        assertThat(person.personId).isNotNull
        assertThat(barn.personId).isNotNull

        // oppretter en tom mappe til en person
        val mappeId = mappeRepo.opprettEllerHentMappeForPerson(person.personId)

        // oppretter en bunke i mappen for pleiepenger
        val bunkeId = bunkeRepository.opprettEllerHentBunkeForFagsakType(mappeId, fagsakYtelseType)

        val journalposter = mutableMapOf<String, Any?>()
        journalposter["journalposter"] = listOf(journalpostid1, journalpostid2)

        // oppretter en søknad og legger den i bunken
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

        val nySøknad = søknadRepository.opprettSøknad(søknadEntitet)
        assertThat(nySøknad.søknad).isNotNull
    }
}

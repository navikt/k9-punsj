package no.nav.k9punsj.domenetjenester

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.domenetjenester.repository.BunkeRepository
import no.nav.k9punsj.domenetjenester.repository.MappeRepository
import no.nav.k9punsj.domenetjenester.repository.PersonRepository
import no.nav.k9punsj.domenetjenester.repository.SøknadRepository
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.dto.Person
import no.nav.k9punsj.felles.dto.SøknadEntitet
import no.nav.k9punsj.pleiepengersyktbarn.tilPsbVisning
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.*

class MappeServiceTest: AbstractContainerBaseTest() {

    private companion object {
        private val standardIdent = "01122334410"
        private val standardAktørId = "1000000000000"

        private val barnIdent = "01122334412"
        private val barnAktørID = "1000000000001"

        private val journalpostid1 = "29486889"
        private val journalpostid2 = "29486887"
    }

    @Autowired
    private lateinit var mappeService: MappeService

    @Autowired
    lateinit var mappeRepository: MappeRepository

    @Autowired
    private lateinit var søknadService: SoknadService

    @Autowired
    private lateinit var søknadRepository: SøknadRepository

    @Autowired
    private lateinit var bunkeRepository: BunkeRepository

    @Autowired
    lateinit var personRepository: PersonRepository

    @Test
    fun `Gitt en mappe med søknader, når alle søknader slettes, forvent at mappen og alle tilkoblinger fjernes`(): Unit = runBlocking {
        // Gitt en person...
        val person = personRepository.lagre(norskIdent = standardIdent, aktørId = standardAktørId)
        val barn = personRepository.lagre(norskIdent = barnIdent, aktørId = barnAktørID)

        // med 10 påbegynte søknader...
        (1..10).forEach { i ->
            opprettOgAssertSøknad(
                person = person,
                barn = barn,
                fagsakYtelseType = FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
                søknad = mutableMapOf(
                    "soeknadId" to UUID.randomUUID().toString()
                )
            )
        }
        // har en mappe med en bunke.
        val mappe = mappeService.hentMappe(person)
        Assertions.assertThat(mappe.bunke).hasSize(1)

        // Forventer at det er 10 søknader i mappen
        val psbVisning = mappe.tilPsbVisning(person.norskIdent)
        Assertions.assertThat(psbVisning.søknader).hasSize(10)

        // Når alle søknader slettes...
        mappeService.slettMappeMedAlleKoblinger()
        val mappeEtterSletting = mappeService.hentMappe(person)

        // Forventer at mappen er tom
        Assertions.assertThat(mappeEtterSletting.bunke).isEmpty()
    }

    private suspend fun opprettOgAssertSøknad(person: Person, barn: Person, fagsakYtelseType: FagsakYtelseType, søknad: MutableMap<String, Any?>) {
        val barnFødselsdato = LocalDate.now()

        // oppretter en person

        Assertions.assertThat(person.personId).isNotNull
        Assertions.assertThat(barn.personId).isNotNull

        // oppretter en tom mappe til en person
        val mappeId = mappeRepository.opprettEllerHentMappeForPerson(person.personId)

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
        Assertions.assertThat(nySøknad.søknad).isNotNull
    }
}

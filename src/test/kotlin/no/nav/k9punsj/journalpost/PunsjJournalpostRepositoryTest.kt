package no.nav.k9punsj.journalpost

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.fordel.K9FordelType
import no.nav.k9punsj.journalpost.dto.PunsjJournalpost
import no.nav.k9punsj.journalpost.dto.PunsjJournalpostKildeType
import no.nav.k9punsj.util.IdGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import java.util.*


internal class PunsjJournalpostRepositoryTest : AbstractContainerBaseTest() {

    @Autowired
    private lateinit var journalpostRepository: JournalpostRepository

    @Test
    fun `Skal finne alle journalposter på personen`(): Unit = runBlocking {
        val dummyAktørId = IdGenerator.nesteId()

        val punsjJournalpost1 =
            PunsjJournalpost(
                uuid = UUID.randomUUID(),
                journalpostId = IdGenerator.nesteId(),
                aktørId = dummyAktørId,
                type = K9FordelType.PAPIRSØKNAD.kode
            )
        journalpostRepository.lagre(punsjJournalpost1) {
            punsjJournalpost1
        }

        val punsjJournalpost2 =
            PunsjJournalpost(
                uuid = UUID.randomUUID(),
                journalpostId = IdGenerator.nesteId(),
                aktørId = dummyAktørId,
                type = K9FordelType.PAPIRSØKNAD.kode
            )
        journalpostRepository.lagre(punsjJournalpost2) {
            punsjJournalpost2
        }

        val hent = journalpostRepository.hent(punsjJournalpost1.journalpostId)
        assertThat(hent.aktørId!!).isEqualTo(dummyAktørId)

        val hent2 = journalpostRepository.hent(punsjJournalpost2.journalpostId)
        assertThat(hent2.aktørId!!).isEqualTo(dummyAktørId)

        val finnJournalposterPåPerson = journalpostRepository.finnJournalposterPåPerson(dummyAktørId)
        assertThat(finnJournalposterPåPerson).hasSize(2)
        assertThat(finnJournalposterPåPerson[0].type).isEqualTo(K9FordelType.PAPIRSØKNAD.kode)
    }

    @Test
    fun `Skal bare finne de fra fordel`(): Unit = runBlocking {
        val dummyAktørId = IdGenerator.nesteId()

        val punsjJournalpost1 =
            PunsjJournalpost(
                uuid = UUID.randomUUID(),
                journalpostId = IdGenerator.nesteId(),
                aktørId = dummyAktørId,
                type = K9FordelType.PAPIRSØKNAD.kode
            )
        journalpostRepository.lagre(punsjJournalpost1, PunsjJournalpostKildeType.SAKSBEHANDLER) {
            punsjJournalpost1
        }

        val punsjJournalpost2 =
            PunsjJournalpost(
                uuid = UUID.randomUUID(),
                journalpostId = IdGenerator.nesteId(),
                aktørId = dummyAktørId,
                type = K9FordelType.PAPIRSØKNAD.kode
            )
        journalpostRepository.lagre(punsjJournalpost2, PunsjJournalpostKildeType.FORDEL) {
            punsjJournalpost2
        }

        val hent = journalpostRepository.hent(punsjJournalpost1.journalpostId)
        assertThat(hent.aktørId!!).isEqualTo(dummyAktørId)

        val hent2 = journalpostRepository.hent(punsjJournalpost2.journalpostId)
        assertThat(hent2.aktørId!!).isEqualTo(dummyAktørId)

        val finnJournalposterPåPerson = journalpostRepository.finnJournalposterPåPersonBareFordel(dummyAktørId)
        assertThat(finnJournalposterPåPerson).hasSize(1)
        assertThat(finnJournalposterPåPerson[0].type).isEqualTo(K9FordelType.PAPIRSØKNAD.kode)
    }

    @Test
    fun `Skal sette status til ferdig`(): Unit = runBlocking {
        val dummyAktørId = IdGenerator.nesteId()

        val punsjJournalpost1 =
            PunsjJournalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId)
        journalpostRepository.lagre(punsjJournalpost1) {
            punsjJournalpost1
        }

        val punsjJournalpost2 =
            PunsjJournalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId)
        journalpostRepository.lagre(punsjJournalpost2) {
            punsjJournalpost2
        }

        val hent = journalpostRepository.hent(punsjJournalpost1.journalpostId)
        assertThat(hent.aktørId!!).isEqualTo(dummyAktørId)

        val hent2 = journalpostRepository.hent(punsjJournalpost2.journalpostId)
        assertThat(hent2.aktørId!!).isEqualTo(dummyAktørId)

        val finnJournalposterPåPerson = journalpostRepository.finnJournalposterPåPerson(dummyAktørId)
        assertThat(finnJournalposterPåPerson).hasSize(2)

        journalpostRepository.settAlleTilFerdigBehandlet(
            listOf(
                punsjJournalpost1.journalpostId,
                punsjJournalpost2.journalpostId
            )
        )

        val finnJournalposterPåPersonSkalGiTom = journalpostRepository.finnJournalposterPåPerson(dummyAktørId)
        assertThat(finnJournalposterPåPersonSkalGiTom).isEmpty()
    }

    @Test
    fun `Endepunkt brukt for resett av journalpost fungerer`(): Unit = runBlocking {
        val dummyAktørId = IdGenerator.nesteId()

        val punsjJournalpost = PunsjJournalpost(
            uuid = UUID.randomUUID(),
            journalpostId = IdGenerator.nesteId(),
            aktørId = dummyAktørId,
            skalTilK9 = false
        )
        journalpostRepository.lagre(punsjJournalpost) { punsjJournalpost }

        webTestClient.get()
            .uri { it.path("/api/journalpost/resett/${punsjJournalpost.journalpostId}").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .exchange()
            .expectStatus().isOk

        val finnJournalposterPåPerson = journalpostRepository.finnJournalposterPåPerson(dummyAktørId)
        Assertions.assertEquals(finnJournalposterPåPerson[0].skalTilK9, null)
    }

    @Test
    fun `Skal sjekke om punsj kan sende inn`(): Unit = runBlocking {
        val dummyAktørId = IdGenerator.nesteId()

        val punsjJournalpost1 =
            PunsjJournalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId)
        journalpostRepository.lagre(punsjJournalpost1) {
            punsjJournalpost1
        }

        val punsjJournalpost2 =
            PunsjJournalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId)
        journalpostRepository.lagre(punsjJournalpost2) {
            punsjJournalpost2
        }

        val hent = journalpostRepository.hent(punsjJournalpost1.journalpostId)
        assertThat(hent.aktørId!!).isEqualTo(dummyAktørId)

        val hent2 = journalpostRepository.hent(punsjJournalpost2.journalpostId)
        assertThat(hent2.aktørId!!).isEqualTo(dummyAktørId)

        val finnJournalposterPåPerson = journalpostRepository.finnJournalposterPåPerson(dummyAktørId)
        assertThat(finnJournalposterPåPerson).hasSize(2)

        val kanSendeInn =
            journalpostRepository.kanSendeInn(listOf(punsjJournalpost1.journalpostId, punsjJournalpost2.journalpostId))
        assertThat(kanSendeInn).isTrue

        journalpostRepository.settAlleTilFerdigBehandlet(
            listOf(
                punsjJournalpost1.journalpostId,
                punsjJournalpost2.journalpostId
            )
        )

        val kanSendeInn2 =
            journalpostRepository.kanSendeInn(listOf(punsjJournalpost1.journalpostId, punsjJournalpost2.journalpostId))

        assertThat(kanSendeInn2).isFalse
    }

    @Test
    fun `skal vise om journalposten må til infotrygd`(): Unit = runBlocking {
        val dummyAktørId = IdGenerator.nesteId()

        val punsjJournalpost2 =
            PunsjJournalpost(
                uuid = UUID.randomUUID(),
                journalpostId = IdGenerator.nesteId(),
                aktørId = dummyAktørId,
                skalTilK9 = false
            )
        journalpostRepository.lagre(punsjJournalpost2) {
            punsjJournalpost2
        }

        assertThat(journalpostRepository.hent(punsjJournalpost2.journalpostId).skalTilK9).isFalse()
    }

    @Test
    fun `skal kunne sette alle til ferdig`(): Unit = runBlocking {
        val dummyAktørId = IdGenerator.nesteId()

        val punsjJournalpost1 =
            PunsjJournalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId)
        journalpostRepository.lagre(punsjJournalpost1) {
            punsjJournalpost1
        }

        val punsjJournalpost2 =
            PunsjJournalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId)
        journalpostRepository.lagre(punsjJournalpost2) {
            punsjJournalpost2
        }

        journalpostRepository.settAlleTilFerdigBehandlet(
            listOf(
                punsjJournalpost1.journalpostId,
                punsjJournalpost2.journalpostId
            )
        )
        assertThat(
            journalpostRepository.kanSendeInn(
                listOf(
                    punsjJournalpost1.journalpostId,
                    punsjJournalpost2.journalpostId
                )
            )
        ).isFalse()
    }

    @Test
    fun `skal feil hvis ikke alle kan settes til ferdig`(): Unit = runBlocking {
        val dummyAktørId = IdGenerator.nesteId()

        val punsjJournalpost1 =
            PunsjJournalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId)
        journalpostRepository.lagre(punsjJournalpost1) {
            punsjJournalpost1
        }

        val punsjJournalpost2 =
            PunsjJournalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId)

        var harFåttEx = false
        try {
            journalpostRepository.settAlleTilFerdigBehandlet(
                listOf(
                    punsjJournalpost1.journalpostId,
                    punsjJournalpost2.journalpostId
                )
            )
        } catch (e: IllegalStateException) {
            assertThat(e.message).isEqualTo("Klarte ikke sette alle til ferdig")
            harFåttEx = true
        }
        assertThat(harFåttEx).isTrue()
    }

    @Test
    fun `Forventer at gosysoppgaveId blir persistert på journalpost`(): Unit = runBlocking {
        val dummyAktørId = IdGenerator.nesteId()
        val forventetGosysoppgaveId = IdGenerator.nesteId()

        val punsjJournalpost = PunsjJournalpost(
            uuid = UUID.randomUUID(),
            journalpostId = IdGenerator.nesteId(),
            aktørId = dummyAktørId,
            type = K9FordelType.SAMTALEREFERAT.kode,
            gosysoppgaveId = forventetGosysoppgaveId
        )

        journalpostRepository.lagre(punsjJournalpost) { punsjJournalpost }

        val journalpost = journalpostRepository.hent(punsjJournalpost.journalpostId)
        assertThat(journalpost.aktørId!!).isEqualTo(dummyAktørId)
        assertThat(journalpost.gosysoppgaveId!!).isEqualTo(forventetGosysoppgaveId)

        val finnJournalposterPåPerson = journalpostRepository.finnJournalposterPåPerson(dummyAktørId)
        assertThat(finnJournalposterPåPerson[0].type).isEqualTo(K9FordelType.SAMTALEREFERAT.kode)
        assertThat(finnJournalposterPåPerson[0].gosysoppgaveId).isEqualTo(forventetGosysoppgaveId)
    }
}

package no.nav.k9punsj.journalpost

import no.nav.k9punsj.fordel.PunsjInnsendingType
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class VirkedagerUtilTest {


    @Test
    fun `skal gi 2 dager ekstra på papirsøknad`() {
        val papirsøknad = PunsjInnsendingType.PAPIRSØKNAD
        val mandag = LocalDateTime.of(2021, 5, 31, 10, 22)
        val tirsdag = LocalDateTime.of(2021, 6, 1, 10, 22)
        val onsdag = LocalDateTime.of(2021, 6, 2, 10, 22)
        val torsdag = LocalDateTime.of(2021, 6, 3, 10, 22)
        val fredag = LocalDateTime.of(2021, 6, 4, 10, 22)
        val lørdag = LocalDateTime.of(2021, 6, 5, 10, 22)
        val søndag = LocalDateTime.of(2021, 6, 6, 10, 22)

        val mandagForventetTidspunkt = LocalDateTime.of(2021, 5, 27, 10, 22)
        val tirsdagForventetTidspunkt = LocalDateTime.of(2021, 5, 28, 10, 22)
        val onsdagForventetTidspunkt = LocalDateTime.of(2021, 5, 31, 10, 22)
        val torsdagForventetTidspunkt = LocalDateTime.of(2021, 6, 1, 10, 22)
        val fredagForventetTidspunkt = LocalDateTime.of(2021, 6, 2, 10, 22)
        val lørdagForventetTidspunkt = LocalDateTime.of(2021, 6, 3, 10, 22)
        val søndagForventetTidspunkt = LocalDateTime.of(2021, 6, 3, 10, 22)

        Assertions.assertThat(mandagForventetTidspunkt).isEqualTo(VirkedagerUtil.tilbakeStillToVirkedagerHvisDetKommerFraScanning(papirsøknad.kode, mandag))
        Assertions.assertThat(tirsdagForventetTidspunkt).isEqualTo(VirkedagerUtil.tilbakeStillToVirkedagerHvisDetKommerFraScanning(papirsøknad.kode, tirsdag))
        Assertions.assertThat(onsdagForventetTidspunkt).isEqualTo(VirkedagerUtil.tilbakeStillToVirkedagerHvisDetKommerFraScanning(papirsøknad.kode, onsdag))
        Assertions.assertThat(torsdagForventetTidspunkt).isEqualTo(VirkedagerUtil.tilbakeStillToVirkedagerHvisDetKommerFraScanning(papirsøknad.kode, torsdag))
        Assertions.assertThat(fredagForventetTidspunkt).isEqualTo(VirkedagerUtil.tilbakeStillToVirkedagerHvisDetKommerFraScanning(papirsøknad.kode, fredag))
        Assertions.assertThat(lørdagForventetTidspunkt).isEqualTo(VirkedagerUtil.tilbakeStillToVirkedagerHvisDetKommerFraScanning(papirsøknad.kode, lørdag))
        Assertions.assertThat(søndagForventetTidspunkt).isEqualTo(VirkedagerUtil.tilbakeStillToVirkedagerHvisDetKommerFraScanning(papirsøknad.kode, søndag))
    }

    @Test
    fun `skal ikke gi 2 dager ekstra på ikke papirsøknad`() {
        val papirsøknad = PunsjInnsendingType.DIGITAL_ETTERSENDELSE

        val søndag = LocalDateTime.of(2021, 6, 6, 10, 22)

        Assertions.assertThat(søndag).isEqualTo(VirkedagerUtil.tilbakeStillToVirkedagerHvisDetKommerFraScanning(papirsøknad.kode, søndag))
    }
}

package no.nav.k9punsj.domenetjenester.mappers

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class MapFraVisningTilEksternFormatTest {



    @Test
    fun `Skal utlede riktig tall`() {
        val test1 = MapFraVisningTilEksternFormat.zeroTimerHvisTomString("7,48")
        val test2 = MapFraVisningTilEksternFormat.zeroTimerHvisTomString("7,3")
        val test3 = MapFraVisningTilEksternFormat.zeroTimerHvisTomString("0,08")
        val test4 = MapFraVisningTilEksternFormat.zeroTimerHvisTomString("4")
        val test5 = MapFraVisningTilEksternFormat.zeroTimerHvisTomString("7.4823")
        val test6 = MapFraVisningTilEksternFormat.zeroTimerHvisTomString("7,4348")
        val test7 = MapFraVisningTilEksternFormat.zeroTimerHvisTomString("7,0")

        Assertions.assertThat(test1.toString()).isEqualTo("PT7H28M")
        Assertions.assertThat(test2.toString()).isEqualTo("PT7H18M")
        Assertions.assertThat(test3.toString()).isEqualTo("PT4M")
        Assertions.assertThat(test4.toString()).isEqualTo("PT4H")
        Assertions.assertThat(test5.toString()).isEqualTo("PT7H28M")
        Assertions.assertThat(test6.toString()).isEqualTo("PT7H26M")
        Assertions.assertThat(test7.toString()).isEqualTo("PT7H")
    }
}

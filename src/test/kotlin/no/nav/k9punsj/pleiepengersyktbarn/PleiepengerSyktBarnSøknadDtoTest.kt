package no.nav.k9punsj.pleiepengersyktbarn

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.k9punsj.utils.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PleiepengerSyktBarnSøknadDtoTest {
    private val objectMapper = objectMapper()

    @Test
    fun `perDagString skal være styrende når tidsformat er desimaler`() {
        val tilsynsordningInfoDto = objectMapper.readValue<PleiepengerSyktBarnSøknadDto.TilsynsordningInfoDto>(
            """
                {
                  "tidsformat" : "desimaler",
                  "periode" : {
                    "fom" : "2024-07-30",
                    "tom" : "2024-07-30"
                  },
                  "perDagString" : "5.5",
                  "timer" : 0,
                  "minutter" : 0
                }
            """.trimIndent())
        assertEquals(5, tilsynsordningInfoDto.timer)
        assertEquals(30, tilsynsordningInfoDto.minutter)
    }

    @Test
    fun `timer og minutter skal være styrende når tidsformat er timerOgMinutter`() {
        val tilsynsordningInfoDto = objectMapper.readValue<PleiepengerSyktBarnSøknadDto.TilsynsordningInfoDto>(
            """
                {
                  "tidsformat" : "timerOgMin",
                  "periode" : {
                    "fom" : "2024-07-30",
                    "tom" : "2024-07-30"
                  },
                  "perDagString" : "5.5",
                  "timer" : 7,
                  "minutter" : 10
                }
            """.trimIndent())
        assertEquals(7, tilsynsordningInfoDto.timer)
        assertEquals(10, tilsynsordningInfoDto.minutter)
    }

    @Test
    fun `for legacy data, timer og minutter skal være styrende når tidsformat ikke er satt`() {
        val tilsynsordningInfoDto = objectMapper.readValue<PleiepengerSyktBarnSøknadDto.TilsynsordningInfoDto>(
            """
                {
                  "periode" : {
                    "fom" : "2024-07-30",
                    "tom" : "2024-07-30"
                  },
                  "timer" : 7,
                  "minutter" : 10
                }
            """.trimIndent())
        assertEquals(7, tilsynsordningInfoDto.timer)
        assertEquals(10, tilsynsordningInfoDto.minutter)
    }

}
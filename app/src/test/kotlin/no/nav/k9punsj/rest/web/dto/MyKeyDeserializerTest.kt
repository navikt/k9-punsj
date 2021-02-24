package no.nav.k9punsj.rest.web.dto

import no.nav.k9punsj.objectMapper
import org.junit.jupiter.api.Test
import java.time.LocalDate


internal class MyKeyDeserializerTest {

    @Test
    fun `Skal funke`() {
        val periodeDto = PeriodeDto(LocalDate.now(), LocalDate.now().plusDays(1))

        val perioder = periodeDto to PleiepengerSøknadVisningDto.PleiepengerYtelseDto.BostederDto.BostedPeriodeInfoDto("NORGE")

        val bostederDto = PleiepengerSøknadVisningDto.PleiepengerYtelseDto.BostederDto(mapOf(perioder))

        val writeValueAsString = objectMapper().writeValueAsString(bostederDto)

        val test: String

    }

}


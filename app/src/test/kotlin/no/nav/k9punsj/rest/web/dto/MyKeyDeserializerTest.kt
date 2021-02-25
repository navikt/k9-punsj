package no.nav.k9punsj.rest.web.dto

import org.junit.jupiter.api.Test
import java.time.LocalDate


internal class MyKeyDeserializerTest {

    @Test
    fun `Skal funke`() {
        val periodeDto = PeriodeDto(LocalDate.now(), LocalDate.now().plusDays(1))





    }

}


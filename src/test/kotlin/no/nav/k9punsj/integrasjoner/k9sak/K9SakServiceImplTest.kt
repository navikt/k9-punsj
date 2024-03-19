package no.nav.k9punsj.integrasjoner.k9sak

import no.nav.k9punsj.AbstractContainerBaseTest
import org.junit.jupiter.api.Test

import org.springframework.beans.factory.annotation.Autowired

class K9SakServiceImplTest: AbstractContainerBaseTest() {

    @Autowired
    lateinit var k9SakServiceImp: K9SakService

    @Test
    fun reserverSaksnummer() {
    }
}

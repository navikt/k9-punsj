package no.nav.k9.pdl

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.k9.objectMapper
import org.assertj.core.api.Assertions
import org.junit.Test

class PdlServiceTest {
    @Test
    fun deserialisereError()  {
        val s = """{"errors":[{"message":"Ikke autentisert","locations":[{"line":3,"column":5}],"path":["hentIdenter"],"extensions":{"code":"unauthenticated","classification":"ExecutionAborted"}}],"data":{"hentIdenter":null}}"""
        val (data, errors) = objectMapper().readValue<AktøridPdl>(s)
        Assertions.assertThat(data).isNotNull
        Assertions.assertThat(errors).isNotNull
    }
    @Test
    fun deserialisereOk()  {
        val s = """{"data":{"hentIdenter":{"identer":[{"ident":"2002220522526","historisk":false,"gruppe":"AKTORID"}]}}}"""
        val (data, errors) = objectMapper().readValue<AktøridPdl>(s)
        Assertions.assertThat(data).isNotNull
        Assertions.assertThat(errors).isNull()
    }
}
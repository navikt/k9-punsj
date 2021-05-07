package no.nav.k9punsj.innsending

import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9punsj.domenetjenester.mappers.SøknadMapper
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadVisningDto
import no.nav.k9punsj.util.LesFraFilUtil
import org.junit.jupiter.api.Test
import java.util.*

internal class InnsendingMappingTest {
    private val innsendingClient = LoggingInnsendingClient()

    @Test
    fun `mappe pleiepenger sykt barn søknad`() {
        val søknad = LesFraFilUtil.søknadFraFrontend()
        val dto = objectMapper().convertValue<PleiepengerSøknadVisningDto>(søknad)

        val k9Format = SøknadMapper.mapTilEksternFormat(
            søknad = SøknadMapper.mapTilSendingsformat(dto),
            soeknadId = "${UUID.randomUUID()}",
            hentPerioderSomFinnesIK9 = emptyList()
        ).first

        val (_, value) = innsendingClient.mapSøknad(
            søknadId = k9Format.søknadId.id,
            søknad = k9Format,
            tilleggsOpplysninger = emptyMap()
        )

        println(value)
    }
}
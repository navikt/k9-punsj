package no.nav.k9punsj.metrikker

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.k9punsj.domenetjenester.mappers.MapPsbTilK9Format
import no.nav.k9punsj.metrikker.SøknadMetrikkService.Companion.ANTALL_ARBEIDSGIVERE_BUCKET
import no.nav.k9punsj.metrikker.SøknadMetrikkService.Companion.ANTALL_INNSENDINGER
import no.nav.k9punsj.metrikker.SøknadMetrikkService.Companion.ANTALL_UKER_SØKNADER_GJELDER_BUCKET
import no.nav.k9punsj.metrikker.SøknadMetrikkService.Companion.ARBEIDSTID_FRILANSER_COUNTER
import no.nav.k9punsj.metrikker.SøknadMetrikkService.Companion.ARBEIDSTID_SELVSTENDING_COUNTER
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.SøknadJson
import no.nav.k9punsj.rest.web.dto.PleiepengerSyktBarnSøknadDto
import no.nav.k9punsj.util.LesFraFilUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SøknadMetrikkServiceTest {

    private lateinit var søknadMetrikkService: SøknadMetrikkService
    private lateinit var simpleMeterRegistry: SimpleMeterRegistry

    @BeforeEach
    internal fun setUp() {
        simpleMeterRegistry = SimpleMeterRegistry()
        søknadMetrikkService = SøknadMetrikkService(simpleMeterRegistry)
    }

    @Test
    internal fun forvent_riktig_publiserte_metrikker() {
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontend()
        val dto = objectMapper().convertValue(gyldigSoeknad, PleiepengerSyktBarnSøknadDto::class.java)
        val k9Format = MapPsbTilK9Format(dto.soeknadId, emptySet(), emptyList(), dto).søknad()
        søknadMetrikkService.publiserMetrikker(k9Format)

        val antallInnsendteSøknader = simpleMeterRegistry.get(ANTALL_INNSENDINGER).counter().count()
        assertEquals(1.0, antallInnsendteSøknader)


        val antallUkerSøknadenGjelder =
            simpleMeterRegistry.get(ANTALL_UKER_SØKNADER_GJELDER_BUCKET).summary().totalAmount()
        assertEquals(42.0, antallUkerSøknadenGjelder)

        val antallArbeidsgivere = simpleMeterRegistry.get(ANTALL_ARBEIDSGIVERE_BUCKET).summary().totalAmount()
        assertEquals(1.0, antallArbeidsgivere)

        val arbeidstidFrilanserCounter = simpleMeterRegistry.get(ARBEIDSTID_FRILANSER_COUNTER).counter().count()
        assertEquals(1.0, arbeidstidFrilanserCounter)

        val arbeidstidSelvstendigCounter = simpleMeterRegistry.get(ARBEIDSTID_SELVSTENDING_COUNTER).counter().count()
        assertEquals(1.0, arbeidstidSelvstendigCounter)

        /*val journalpostCounter = simpleMeterRegistry.get(JOURNALPOST_COUNTER).counter().count()
        assertEquals(1.0, journalpostCounter)*/
    }
}

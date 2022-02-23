package no.nav.k9punsj.metrikker

import io.micrometer.core.instrument.Statistic
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.k9punsj.domenetjenester.mappers.MapPsbTilK9Format
import no.nav.k9punsj.metrikker.SøknadMetrikkService.Companion.ANTALL_ARBEIDSGIVERE_BUCKET
import no.nav.k9punsj.metrikker.SøknadMetrikkService.Companion.ANTALL_INNSENDINGER
import no.nav.k9punsj.metrikker.SøknadMetrikkService.Companion.ANTALL_UKER_SØKNADER_GJELDER_BUCKET
import no.nav.k9punsj.metrikker.SøknadMetrikkService.Companion.ARBEIDSTID_FRILANSER_COUNTER
import no.nav.k9punsj.metrikker.SøknadMetrikkService.Companion.ARBEIDSTID_SELVSTENDING_COUNTER
import no.nav.k9punsj.metrikker.SøknadMetrikkService.Companion.JOURNALPOST_COUNTER
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.SøknadJson
import no.nav.k9punsj.rest.web.dto.PleiepengerSyktBarnSøknadDto
import no.nav.k9punsj.util.LesFraFilUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.actuate.metrics.MetricsEndpoint
import org.springframework.boot.actuate.metrics.MetricsEndpoint.MetricResponse


internal class SøknadMetrikkServiceTest {

    private lateinit var søknadMetrikkService: SøknadMetrikkService
    private lateinit var simpleMeterRegistry: SimpleMeterRegistry
    private lateinit var metricsEndpoint: MetricsEndpoint

    @BeforeEach
    internal fun setUp() {
        simpleMeterRegistry = SimpleMeterRegistry()
        søknadMetrikkService = SøknadMetrikkService(simpleMeterRegistry)
        metricsEndpoint = MetricsEndpoint(simpleMeterRegistry)
    }

    @Test
    internal fun forvent_riktig_publiserte_metrikker() {
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontend()
        val dto = objectMapper().convertValue(gyldigSoeknad, PleiepengerSyktBarnSøknadDto::class.java)
        val k9Format = MapPsbTilK9Format(dto.soeknadId, setOf("123", "456"), emptyList(), dto).søknad()
        søknadMetrikkService.publiserMetrikker(k9Format)

        assertCounter(
            metric = ANTALL_INNSENDINGER,
            forventetVerdi = 1.0,
            Tag.of("soknadstype", "PLEIEPENGER_SYKT_BARN"),
            Tag.of("soknadsId", dto.soeknadId)
        )

        assertBucket(
            metric = ANTALL_UKER_SØKNADER_GJELDER_BUCKET,
            forventetVerdi = 42.0,
            Tag.of("soknadstype", "PLEIEPENGER_SYKT_BARN"),
            Tag.of("soknadsId", dto.soeknadId)
        )

        assertBucket(
            metric = ANTALL_ARBEIDSGIVERE_BUCKET,
            forventetVerdi = 1.0,
            Tag.of("soknadstype", "PLEIEPENGER_SYKT_BARN"),
            Tag.of("soknadsId", dto.soeknadId)
        )

        assertCounter(
            metric = ARBEIDSTID_FRILANSER_COUNTER,
            forventetVerdi = 1.0,
            Tag.of("soknadstype", "PLEIEPENGER_SYKT_BARN"),
            Tag.of("soknadsId", dto.soeknadId)
        )

        assertCounter(
            metric = ARBEIDSTID_SELVSTENDING_COUNTER,
            forventetVerdi = 1.0,
            Tag.of("soknadstype", "PLEIEPENGER_SYKT_BARN"),
            Tag.of("soknadsId", dto.soeknadId)
        )

        assertCounter(
            metric = JOURNALPOST_COUNTER,
            forventetVerdi = 1.0,
            Tag.of("soknadstype", "PLEIEPENGER_SYKT_BARN"),
            Tag.of("soknadsId", dto.soeknadId),
            Tag.of("antall_journalposter", "2"),
            Tag.of("opplysninger", "IkkeKanPunsjes=true | MedOpplysninger=false")
        )
    }

    private fun assertCounter(metric: String, forventetVerdi: Double, vararg tags: Tag) {
        val metricResponse: MetricResponse = metricsEndpoint.metric(metric, listOf())
        assertThat(getCount(metricResponse)).isEqualTo(forventetVerdi)
        assertThat(tags(metricResponse)).contains(*tags)
    }

    private fun assertBucket(metric: String, forventetVerdi: Double, vararg tags: Tag) {
        val metricResponse = metricsEndpoint.metric(metric, listOf())
        assertThat(getBucketValue(metricResponse)).isEqualTo(forventetVerdi)
        assertThat(tags(metricResponse)).contains(*tags)
    }

    private fun getCount(response: MetricResponse): Double? {
        return response.measurements.stream()
            .filter { it.statistic == Statistic.COUNT }
            .findAny().map { it.value }.orElse(null)
    }

    private fun getBucketValue(response: MetricResponse): Double? {
        return response.measurements.stream()
            .filter { it.statistic == Statistic.TOTAL }
            .findAny().map { it.value }.orElse(null)
    }

    private fun tags(response: MetricResponse): List<Tag> {
        return response.availableTags.map { Tag.of(it.tag, it.values.first()) }
    }
}

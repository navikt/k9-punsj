package no.nav.k9punsj.util

import io.micrometer.core.instrument.Statistic
import no.nav.k9punsj.metrikker.Metrikk
import org.assertj.core.api.Assertions
import org.springframework.boot.actuate.metrics.MetricsEndpoint

class MetricUtils {

    companion object {
        fun assertCounter(
            metricsEndpoint: MetricsEndpoint,
            metric: Metrikk,
            forventetVerdi: Double,
            vararg tags: MetrikkTag
        ) {
            val metricResponse: MetricsEndpoint.MetricResponse = metricsEndpoint.metric(metric.navn, listOf())
            Assertions.assertThat(getCount(metricResponse)).isEqualTo(forventetVerdi)
            if (tags.isNotEmpty()) {
                val tagsResponse = tags(metricResponse)
                Assertions.assertThat(tagsResponse).containsAnyOf(*tags)
            }
        }

        fun assertGuage(
            metricsEndpoint: MetricsEndpoint,
            metric: Metrikk,
            forventetVerdi: Double,
            vararg tags: MetrikkTag
        ) {
            val metricResponse: MetricsEndpoint.MetricResponse = metricsEndpoint.metric(metric.navn, listOf())
            Assertions.assertThat(getGuageValue(metricResponse)).isEqualTo(forventetVerdi)
            Assertions.assertThat(tags(metricResponse)).contains(*tags)
        }

        fun assertBucket(
            metricsEndpoint: MetricsEndpoint,
            metric: Metrikk,
            forventetVerdi: Double,
            vararg tags: MetrikkTag
        ) {
            val metricResponse = metricsEndpoint.metric(metric.navn, listOf())
            Assertions.assertThat(getBucketValue(metricResponse)).isEqualTo(forventetVerdi)
            Assertions.assertThat(tags(metricResponse)).contains(*tags)
        }

        fun getCount(response: MetricsEndpoint.MetricResponse): Double? {
            return response.measurements.stream()
                .filter { it.statistic == Statistic.COUNT }
                .findAny().map { it.value }.orElse(null)
        }

        fun getGuageValue(response: MetricsEndpoint.MetricResponse): Double? {
            return response.measurements.stream()
                .filter { it.statistic == Statistic.VALUE }
                .findAny().map { it.value }.orElse(null)
        }

        private fun getBucketValue(response: MetricsEndpoint.MetricResponse): Double? {
            return response.measurements.stream()
                .filter { it.statistic == Statistic.TOTAL }
                .findAny().map { it.value }.orElse(null)
        }

        private fun tags(response: MetricsEndpoint.MetricResponse): List<MetrikkTag> {
            return response.availableTags.map {
                MetrikkTag(it.tag, it.values)
            }
        }
    }

    data class MetrikkTag(val tag: String, val values: Set<String> = setOf())
}

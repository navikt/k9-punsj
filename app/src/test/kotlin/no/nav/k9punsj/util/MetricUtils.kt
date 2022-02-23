package no.nav.k9punsj.util

import io.micrometer.core.instrument.Statistic
import io.micrometer.core.instrument.Tag
import org.assertj.core.api.Assertions
import org.springframework.boot.actuate.metrics.MetricsEndpoint

class MetricUtils {

    companion object {
        fun assertCounter(metricsEndpoint: MetricsEndpoint, metric: String, forventetVerdi: Double, vararg tags: Tag) {
            val metricResponse: MetricsEndpoint.MetricResponse = metricsEndpoint.metric(metric, listOf())
            Assertions.assertThat(getCount(metricResponse)).isEqualTo(forventetVerdi)
            Assertions.assertThat(tags(metricResponse)).contains(*tags)
        }
        fun assertGuage(metricsEndpoint: MetricsEndpoint, metric: String, forventetVerdi: Double, vararg tags: Tag) {
            val metricResponse: MetricsEndpoint.MetricResponse = metricsEndpoint.metric(metric, listOf())
            Assertions.assertThat(getGuageValue(metricResponse)).isEqualTo(forventetVerdi)
            Assertions.assertThat(tags(metricResponse)).contains(*tags)
        }

        fun assertBucket(metricsEndpoint: MetricsEndpoint, metric: String, forventetVerdi: Double, vararg tags: Tag) {
            val metricResponse = metricsEndpoint.metric(metric, listOf())
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

        private fun tags(response: MetricsEndpoint.MetricResponse): List<Tag> {
            return response.availableTags.map { Tag.of(it.tag, it.values.first()) }
        }
    }

}

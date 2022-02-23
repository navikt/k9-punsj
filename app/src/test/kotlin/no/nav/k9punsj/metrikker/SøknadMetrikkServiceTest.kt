package no.nav.k9punsj.metrikker

import io.micrometer.core.instrument.Statistic
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.k9.søknad.ytelse.Ytelse.Type
import no.nav.k9punsj.domenetjenester.mappers.*
import no.nav.k9punsj.metrikker.SøknadMetrikkService.Companion.ANTALL_ARBEIDSGIVERE_BUCKET
import no.nav.k9punsj.metrikker.SøknadMetrikkService.Companion.ANTALL_INNSENDINGER
import no.nav.k9punsj.metrikker.SøknadMetrikkService.Companion.ANTALL_UKER_SØKNADER_GJELDER_BUCKET
import no.nav.k9punsj.metrikker.SøknadMetrikkService.Companion.ARBEIDSTID_FRILANSER_COUNTER
import no.nav.k9punsj.metrikker.SøknadMetrikkService.Companion.ARBEIDSTID_SELVSTENDING_COUNTER
import no.nav.k9punsj.metrikker.SøknadMetrikkService.Companion.BEREDSKAP_COUNTER
import no.nav.k9punsj.metrikker.SøknadMetrikkService.Companion.JOURNALPOST_COUNTER
import no.nav.k9punsj.metrikker.SøknadMetrikkService.Companion.NATTEVAAK_COUNTER
import no.nav.k9punsj.metrikker.SøknadMetrikkService.Companion.TILSYNSORDNING_COUNTER
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.SøknadJson
import no.nav.k9punsj.rest.web.dto.*
import no.nav.k9punsj.util.LesFraFilUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.actuate.metrics.MetricsEndpoint
import org.springframework.boot.actuate.metrics.MetricsEndpoint.MetricResponse


internal class SøknadMetrikkServiceTest {

    private lateinit var søknadMetrikkService: SøknadMetrikkService
    private lateinit var metricsEndpoint: MetricsEndpoint

    @BeforeEach
    internal fun setUp() {
        val simpleMeterRegistry = SimpleMeterRegistry()
        søknadMetrikkService = SøknadMetrikkService(simpleMeterRegistry)
        metricsEndpoint = MetricsEndpoint(simpleMeterRegistry)
    }

    @Test
    internal fun forvent_riktig_publiserte_psb_metrikker() {
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontend()
        val dto = objectMapper().convertValue(gyldigSoeknad, PleiepengerSyktBarnSøknadDto::class.java)
        val k9Format = MapPsbTilK9Format(dto.soeknadId, setOf("123", "456"), emptyList(), dto).søknad()
        søknadMetrikkService.publiserMetrikker(k9Format)

        val søknadstypeTag = Tag.of("soknadstype", Type.PLEIEPENGER_SYKT_BARN.name)
        val søknadsIdTag = Tag.of("soknadsId", dto.soeknadId)

        assertCounter(metric = ANTALL_INNSENDINGER, forventetVerdi = 1.0, søknadstypeTag, søknadsIdTag)
        assertCounter(metric = ARBEIDSTID_FRILANSER_COUNTER, forventetVerdi = 1.0, søknadstypeTag, søknadsIdTag)
        assertCounter(metric = ARBEIDSTID_SELVSTENDING_COUNTER, forventetVerdi = 1.0, søknadstypeTag, søknadsIdTag)
        assertCounter(metric = BEREDSKAP_COUNTER, forventetVerdi = 1.0, søknadstypeTag, søknadsIdTag)
        assertCounter(metric = BEREDSKAP_COUNTER, forventetVerdi = 1.0, søknadstypeTag, søknadsIdTag)
        assertCounter(metric = NATTEVAAK_COUNTER, forventetVerdi = 1.0, søknadstypeTag, søknadsIdTag)
        assertCounter(metric = TILSYNSORDNING_COUNTER, forventetVerdi = 1.0, søknadstypeTag, søknadsIdTag)

        val forventetAntallUker = 42.0
        assertBucket(metric = ANTALL_UKER_SØKNADER_GJELDER_BUCKET, forventetVerdi = forventetAntallUker, søknadstypeTag, søknadsIdTag)
        assertBucket(metric = ANTALL_ARBEIDSGIVERE_BUCKET, forventetVerdi = 1.0, søknadstypeTag, søknadsIdTag)

        assertCounter(
            metric = JOURNALPOST_COUNTER,
            forventetVerdi = 1.0,
            søknadstypeTag,
            søknadsIdTag,
            Tag.of("antall_journalposter", "2"),
            Tag.of("opplysninger", "IkkeKanPunsjes=true | MedOpplysninger=false")
        )
    }

    @Test
    internal fun forvent_riktig_publiserte_oms_ao_metrikker() {
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsAO()
        val dto = objectMapper().convertValue(gyldigSoeknad, OmsorgspengerAleneOmsorgSøknadDto::class.java)
        val k9Format = MapOmsAOTilK9Format(dto.soeknadId, setOf("123", "456"), dto).søknad()
        søknadMetrikkService.publiserMetrikker(k9Format)

        val søknadstypeTag = Tag.of("soknadstype", Type.OMSORGSPENGER_UTVIDETRETT_ALENE_OMSORG.name)
        val søknadsIdTag = Tag.of("soknadsId", dto.soeknadId)

        assertCounter(metric = ANTALL_INNSENDINGER, forventetVerdi = 1.0, søknadstypeTag, søknadsIdTag)
    }

    @Test
    internal fun forvent_riktig_publiserte_oms_ma_metrikker() {
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsMA()
        val dto = objectMapper().convertValue(gyldigSoeknad, OmsorgspengerMidlertidigAleneSøknadDto::class.java)
        val k9Format = MapOmsMATilK9Format(dto.soeknadId, setOf("123", "456"), dto).søknad()
        søknadMetrikkService.publiserMetrikker(k9Format)

        val søknadstypeTag = Tag.of("soknadstype", Type.OMSORGSPENGER_UTVIDETRETT_MIDLERTIDIG_ALENE.name)
        val søknadsIdTag = Tag.of("soknadsId", dto.soeknadId)

        assertCounter(metric = ANTALL_INNSENDINGER, forventetVerdi = 1.0, søknadstypeTag, søknadsIdTag)
    }

    @Test
    internal fun forvent_riktig_publiserte_pls_metrikker() {
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendPls()
        val dto = objectMapper().convertValue(gyldigSoeknad, PleiepengerLivetsSluttfaseSøknadDto::class.java)
        val k9Format = MapPlsfTilK9Format(dto.soeknadId, setOf("123", "456"), dto).søknad()
        søknadMetrikkService.publiserMetrikker(k9Format)

            val søknadstypeTag = Tag.of("soknadstype", Type.PLEIEPENGER_LIVETS_SLUTTFASE.name)
        val søknadsIdTag = Tag.of("soknadsId", dto.soeknadId)

        assertCounter(metric = ANTALL_INNSENDINGER, forventetVerdi = 1.0, søknadstypeTag, søknadsIdTag)
    }

    @Test
    internal fun forvent_riktig_publiserte_oms_metrikker() {
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsKSB()
        val dto = objectMapper().convertValue(gyldigSoeknad, OmsorgspengerSøknadDto::class.java)
        val k9Format = MapOmsTilK9Format(dto.soeknadId, setOf("123", "456"), dto).søknad()
        søknadMetrikkService.publiserMetrikker(k9Format)

        val søknadstypeTag = Tag.of("soknadstype", Type.OMSORGSPENGER_UTBETALING.name)
        val søknadsIdTag = Tag.of("soknadsId", dto.soeknadId)

        assertCounter(metric = ANTALL_INNSENDINGER, forventetVerdi = 1.0, søknadstypeTag, søknadsIdTag)
    }

    @Test
    internal fun forvent_riktig_publiserte_omp_ks_metrikker() {
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsKSB()
        val dto = objectMapper().convertValue(gyldigSoeknad, OmsorgspengerKroniskSyktBarnSøknadDto::class.java)
        val k9Format = MapOmsKSBTilK9Format(dto.soeknadId, setOf("123", "456"), dto).søknad()
        søknadMetrikkService.publiserMetrikker(k9Format)

        val søknadstypeTag = Tag.of("soknadstype", Type.OMSORGSPENGER_UTVIDETRETT_KRONISK_SYKT_BARN.name)
        val søknadsIdTag = Tag.of("soknadsId", dto.soeknadId)

        assertCounter(metric = ANTALL_INNSENDINGER, forventetVerdi = 1.0, søknadstypeTag, søknadsIdTag)
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

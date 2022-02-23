package no.nav.k9punsj.metrikker

import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.k9.søknad.ytelse.Ytelse.Type
import no.nav.k9punsj.domenetjenester.mappers.*
import no.nav.k9punsj.metrikker.Metrikk.*
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.SøknadJson
import no.nav.k9punsj.rest.web.dto.*
import no.nav.k9punsj.util.LesFraFilUtil
import no.nav.k9punsj.util.MetricUtils
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.actuate.metrics.MetricsEndpoint


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

        MetricUtils.assertCounter(metricsEndpoint = metricsEndpoint, metric = ANTALL_INNSENDINGER.navn, forventetVerdi = 1.0, søknadstypeTag, søknadsIdTag)
        MetricUtils.assertCounter(metricsEndpoint = metricsEndpoint, metric = ARBEIDSTID_FRILANSER_COUNTER.navn, forventetVerdi = 1.0, søknadstypeTag, søknadsIdTag)
        MetricUtils.assertCounter(metricsEndpoint = metricsEndpoint, metric = ARBEIDSTID_SELVSTENDING_COUNTER.navn, forventetVerdi = 1.0, søknadstypeTag, søknadsIdTag)
        MetricUtils.assertCounter(metricsEndpoint = metricsEndpoint, metric = BEREDSKAP_COUNTER.navn, forventetVerdi = 1.0, søknadstypeTag, søknadsIdTag)
        MetricUtils.assertCounter(metricsEndpoint = metricsEndpoint, metric = BEREDSKAP_COUNTER.navn, forventetVerdi = 1.0, søknadstypeTag, søknadsIdTag)
        MetricUtils.assertCounter(metricsEndpoint = metricsEndpoint, metric = NATTEVAAK_COUNTER.navn, forventetVerdi = 1.0, søknadstypeTag, søknadsIdTag)
        MetricUtils.assertCounter(metricsEndpoint = metricsEndpoint, metric = TILSYNSORDNING_COUNTER.navn, forventetVerdi = 1.0, søknadstypeTag, søknadsIdTag)

        val forventetAntallUker = 42.0
        MetricUtils.assertBucket(metricsEndpoint = metricsEndpoint, metric = ANTALL_UKER_SØKNADER_GJELDER_BUCKET.navn, forventetVerdi = forventetAntallUker, søknadstypeTag, søknadsIdTag)
        MetricUtils.assertBucket(metricsEndpoint = metricsEndpoint, metric = ANTALL_ARBEIDSGIVERE_BUCKET.navn, forventetVerdi = 1.0, søknadstypeTag, søknadsIdTag)

        MetricUtils.assertCounter(metricsEndpoint = metricsEndpoint,
            metric = JOURNALPOST_COUNTER.navn,
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

        MetricUtils.assertCounter(metricsEndpoint = metricsEndpoint, metric = ANTALL_INNSENDINGER.navn, forventetVerdi = 1.0, søknadstypeTag, søknadsIdTag)
    }

    @Test
    internal fun forvent_riktig_publiserte_oms_ma_metrikker() {
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsMA()
        val dto = objectMapper().convertValue(gyldigSoeknad, OmsorgspengerMidlertidigAleneSøknadDto::class.java)
        val k9Format = MapOmsMATilK9Format(dto.soeknadId, setOf("123", "456"), dto).søknad()
        søknadMetrikkService.publiserMetrikker(k9Format)

        val søknadstypeTag = Tag.of("soknadstype", Type.OMSORGSPENGER_UTVIDETRETT_MIDLERTIDIG_ALENE.name)
        val søknadsIdTag = Tag.of("soknadsId", dto.soeknadId)

        MetricUtils.assertCounter(metricsEndpoint = metricsEndpoint, metric = ANTALL_INNSENDINGER.navn, forventetVerdi = 1.0, søknadstypeTag, søknadsIdTag)
    }

    @Test
    internal fun forvent_riktig_publiserte_pls_metrikker() {
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendPls()
        val dto = objectMapper().convertValue(gyldigSoeknad, PleiepengerLivetsSluttfaseSøknadDto::class.java)
        val k9Format = MapPlsfTilK9Format(dto.soeknadId, setOf("123", "456"), dto).søknad()
        søknadMetrikkService.publiserMetrikker(k9Format)

            val søknadstypeTag = Tag.of("soknadstype", Type.PLEIEPENGER_LIVETS_SLUTTFASE.name)
        val søknadsIdTag = Tag.of("soknadsId", dto.soeknadId)

        MetricUtils.assertCounter(metricsEndpoint = metricsEndpoint, metric = ANTALL_INNSENDINGER.navn, forventetVerdi = 1.0, søknadstypeTag, søknadsIdTag)
    }

    @Test
    internal fun forvent_riktig_publiserte_oms_metrikker() {
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOms()
        val dto = objectMapper().convertValue(gyldigSoeknad, OmsorgspengerSøknadDto::class.java)
        val k9Format = MapOmsTilK9Format(dto.soeknadId, setOf("123", "456"), dto).søknad()
        søknadMetrikkService.publiserMetrikker(k9Format)

        val søknadstypeTag = Tag.of("soknadstype", Type.OMSORGSPENGER_UTBETALING.name)
        val søknadsIdTag = Tag.of("soknadsId", dto.soeknadId)

        MetricUtils.assertCounter(metricsEndpoint = metricsEndpoint, metric = ANTALL_INNSENDINGER.navn, forventetVerdi = 1.0, søknadstypeTag, søknadsIdTag)
    }

    @Test
    internal fun forvent_riktig_publiserte_omp_ks_metrikker() {
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsKSB()
        val dto = objectMapper().convertValue(gyldigSoeknad, OmsorgspengerKroniskSyktBarnSøknadDto::class.java)
        val k9Format = MapOmsKSBTilK9Format(dto.soeknadId, setOf("123", "456"), dto).søknad()
        søknadMetrikkService.publiserMetrikker(k9Format)

        val søknadstypeTag = Tag.of("soknadstype", Type.OMSORGSPENGER_UTVIDETRETT_KRONISK_SYKT_BARN.name)
        val søknadsIdTag = Tag.of("soknadsId", dto.soeknadId)

        MetricUtils.assertCounter(metricsEndpoint = metricsEndpoint, metric = ANTALL_INNSENDINGER.navn, forventetVerdi = 1.0, søknadstypeTag, søknadsIdTag)
    }
}

package no.nav.k9punsj.metrikker

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.ytelse.Ytelse
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.temporal.ChronoUnit

private val logger = LoggerFactory.getLogger("no.nav.k9punsj.metrikker.SøknadMetrikkerKt.publiserMetrikker")

@Service
class SøknadMetrikkService(
    private val meterRegistry: MeterRegistry
) {

    fun publiserMetrikker(søknad: Søknad) {
        try {
            val type: Ytelse.Type = søknad.getYtelse<Ytelse>().type
            when (type) {
                Ytelse.Type.PLEIEPENGER_SYKT_BARN -> psbMetrikker(søknad)
                Ytelse.Type.OMSORGSPENGER_UTVIDETRETT_KRONISK_SYKT_BARN -> fellesMetrikker(søknad)
                Ytelse.Type.OMSORGSPENGER_UTVIDETRETT_MIDLERTIDIG_ALENE -> fellesMetrikker(søknad)
                Ytelse.Type.OMSORGSPENGER_UTVIDETRETT_ALENE_OMSORG -> fellesMetrikker(søknad)
                Ytelse.Type.OMSORGSPENGER_UTBETALING -> fellesMetrikker(søknad)
                Ytelse.Type.PLEIEPENGER_LIVETS_SLUTTFASE -> fellesMetrikker(søknad)
            }
        } catch (e: Exception) {
            logger.warn("Feilet med publisering av metrikker", e)
        }
    }

    fun fellesMetrikker(søknad: Søknad) {
        val ytelse = søknad.getYtelse<Ytelse>()
        val søknadstype = ytelse.type.name
        val søknadsId = søknad.søknadId.id
        val defaultTags = mutableListOf(
            Tag.of("soknadsId", søknadsId),
            Tag.of("soknadstype", søknadstype)
        )
        meterRegistry.Config().commonTags(defaultTags)
        meterRegistry.counter(Metrikk.ANTALL_INNSENDINGER.navn).increment()
    }

    private fun psbMetrikker(søknad: Søknad) {
        logger.info("Publiserer søknadsmetrikker.")
        val ytelse = søknad.getYtelse<PleiepengerSyktBarn>()
        fellesMetrikker(søknad)

        hentSøknadsperiodeUker(ytelse)?.apply {
            meterRegistry.summary(Metrikk.ANTALL_UKER_SØKNADER_GJELDER_BUCKET.navn).record(this)
        }

        søknad.journalposter.firstOrNull()?.apply {
            val builder = StringBuilder()
            builder.append("IkkeKanPunsjes=" + this.inneholderInformasjonSomIkkeKanPunsjes.toString())
            builder.append(" | ")
            builder.append("MedOpplysninger=" + this.inneholderMedisinskeOpplysninger.toString())

            meterRegistry.counter(
                Metrikk.JOURNALPOST_COUNTER.navn, listOf(
                    Tag.of("antall_journalposter", søknad.journalposter.size.toString()),
                    Tag.of("opplysninger", builder.toString())
                )
            ).increment()
        }

        ytelse.arbeidstid.apply {
            if (this.arbeidstakerList.isNotEmpty()) {
                meterRegistry.summary(Metrikk.ANTALL_ARBEIDSGIVERE_BUCKET.navn)
                    .record(this.arbeidstakerList.size.toDouble())
            }

            this.frilanserArbeidstidInfo.ifPresent {
                meterRegistry.counter(Metrikk.ARBEIDSTID_FRILANSER_COUNTER.navn).increment()
            }

            this.selvstendigNæringsdrivendeArbeidstidInfo.ifPresent {
                meterRegistry.counter(Metrikk.ARBEIDSTID_SELVSTENDING_COUNTER.navn).increment()
            }
        }

        ytelse.beredskap.apply {
            if (this.perioder.isNotEmpty()) {
                meterRegistry.counter(Metrikk.BEREDSKAP_COUNTER.navn).increment()
            }
        }

        ytelse.nattevåk.apply {
            if (this.perioder.isNotEmpty()) {
                meterRegistry.counter(Metrikk.NATTEVAAK_COUNTER.navn).increment()
            }
        }

        ytelse.tilsynsordning.apply {
            if (this.perioder.isNotEmpty()) {
                meterRegistry.counter(Metrikk.TILSYNSORDNING_COUNTER.navn).increment()
            }
        }
    }

    fun hentSøknadsperiodeUker(ytelse: Ytelse): Double? {
        val søknadsperiode = runCatching { ytelse.søknadsperiode }.getOrNull() ?: return null
        return ChronoUnit.WEEKS.between(søknadsperiode.fraOgMed, søknadsperiode.tilOgMed).toDouble()
    }
}

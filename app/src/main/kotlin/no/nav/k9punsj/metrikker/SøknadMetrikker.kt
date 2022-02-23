package no.nav.k9punsj.metrikker

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.ytelse.Ytelse
import no.nav.k9.søknad.ytelse.pls.v1.PleipengerLivetsSluttfase
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstid
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.temporal.ChronoUnit

private val logger = LoggerFactory.getLogger("no.nav.k9punsj.metrikker.SøknadMetrikkerKt.publiserMetrikker")

@Service
class SøknadMetrikkService(
    private val meterRegistry: MeterRegistry
) {
    fun publiserMetrikker(søknad: Søknad) {
        logger.info("Publiserer søknadsmetrikker.")
        val ytelse = søknad.getYtelse<Ytelse>()
        val søknadstype = hentType(ytelse)
        val søknadsId = søknad.søknadId.id
        val defaultTags = mutableListOf(
            Tag.of("soknadsId", søknadsId),
            Tag.of("soknadstype", søknadstype)
        )
        meterRegistry.Config().commonTags(defaultTags)

        meterRegistry.counter(ANTALL_INNSENDINGER).increment()

        hentSøknadsperiode(ytelse)?.apply {
            meterRegistry.summary(ANTALL_UKER_SØKNADER_GJELDER_BUCKET).record(this)
        }

        søknad.journalposter.firstOrNull()?.apply {
            val builder = StringBuilder()
            builder.append("IkkeKanPunsjes=" + this.inneholderInformasjonSomIkkeKanPunsjes.toString())
            builder.append(" | ")
            builder.append("MedOpplysninger=" + this.inneholderMedisinskeOpplysninger.toString())

            meterRegistry.counter(
                JOURNALPOST_COUNTER, listOf(
                    Tag.of("antall_journalposter", søknad.journalposter.size.toString()),
                    Tag.of("opplysninger", builder.toString())
                )
            ).increment()
        }

        hentArbeidstid(ytelse)?.apply {
            meterRegistry.summary(ANTALL_ARBEIDSGIVERE_BUCKET, defaultTags)
                .record(this.arbeidstakerList.size.toDouble())

            this.frilanserArbeidstidInfo.ifPresent {
                meterRegistry.counter(ARBEIDSTID_FRILANSER_COUNTER, defaultTags).increment()
            }

            this.selvstendigNæringsdrivendeArbeidstidInfo.ifPresent {
                meterRegistry.counter(ARBEIDSTID_SELVSTENDING_COUNTER, defaultTags).increment()
            }
        }

        hentBeredskap(ytelse)?.apply {
            meterRegistry.counter(BEREDSKAP_COUNTER, defaultTags).increment()
        }

        hentNatteVåk(ytelse)?.apply {
            meterRegistry.counter(NATTEVAAK_COUNTER, defaultTags).increment()
        }

        hentTilsynsordning(ytelse)?.apply {
            meterRegistry.counter(TILSYNSORDNING_COUNTER, defaultTags).increment()
        }
    }

    fun hentType(ytelse: Ytelse): String {
        return ytelse.type.name
    }

    fun hentArbeidstid(ytelse: Ytelse): Arbeidstid? {
        return when (ytelse) {
            is PleiepengerSyktBarn -> ytelse.arbeidstid
            is PleipengerLivetsSluttfase -> ytelse.arbeidstid
            else -> null
        }
    }

    fun hentSøknadsperiode(ytelse: Ytelse): Double? {
        val søknadsperiode = runCatching { ytelse.søknadsperiode }.getOrNull() ?: return null
        //legger på en dag siden until ikke tar med siste
        return ChronoUnit.WEEKS.between(søknadsperiode.fraOgMed, søknadsperiode.tilOgMed).toDouble()
    }

    fun hentBeredskap(ytelse: Ytelse) = when (ytelse) {
        is PleiepengerSyktBarn -> ytelse.beredskap
        else -> null
    }

    fun hentNatteVåk(ytelse: Ytelse) = when (ytelse) {
        is PleiepengerSyktBarn -> ytelse.nattevåk
        else -> null
    }

    fun hentTilsynsordning(ytelse: Ytelse) = when (ytelse) {
        is PleiepengerSyktBarn -> ytelse.tilsynsordning
        else -> null
    }

    companion object {
        val ANTALL_INNSENDINGER = "antall_innsendinger_counter"
        val ANTALL_UKER_SØKNADER_GJELDER_BUCKET = "antall_uker_soknaden_gjelder_histogram"
        val JOURNALPOST_COUNTER = "journalpost_counter"
        val ANTALL_ARBEIDSGIVERE_BUCKET = "antall_arbeidsgivere_histogram"
        val ARBEIDSTID_FRILANSER_COUNTER = "arbeidstid_frilanser_counter"
        val ARBEIDSTID_SELVSTENDING_COUNTER = "arbeidstid_selvstendig_counter"
        val BEREDSKAP_COUNTER = "beredskap_counter"
        val NATTEVAAK_COUNTER = "nattevaak_counter"
        val TILSYNSORDNING_COUNTER = "tilsynsordning_counter"
    }
}

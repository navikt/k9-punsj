package no.nav.k9punsj.metrikker

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.ytelse.Ytelse
import no.nav.k9.søknad.ytelse.omsorgspenger.utvidetrett.v1.OmsorgspengerAleneOmsorg
import no.nav.k9.søknad.ytelse.omsorgspenger.utvidetrett.v1.OmsorgspengerKroniskSyktBarn
import no.nav.k9.søknad.ytelse.omsorgspenger.utvidetrett.v1.OmsorgspengerMidlertidigAlene
import no.nav.k9.søknad.ytelse.omsorgspenger.v1.OmsorgspengerUtbetaling
import no.nav.k9.søknad.ytelse.pls.v1.PleipengerLivetsSluttfase
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn
import java.time.temporal.ChronoUnit

fun PleiepengerSyktBarn.publiserMetrikker(søknad: Søknad, meterRegistry: MeterRegistry) {
    fellesMetrikker(søknad, meterRegistry)
    arbeidstid.apply {
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

    beredskap.apply {
        if (this.perioder.isNotEmpty()) {
            meterRegistry.counter(Metrikk.BEREDSKAP_COUNTER.navn).increment()
        }
    }

    nattevåk.apply {
        if (this.perioder.isNotEmpty()) {
            meterRegistry.counter(Metrikk.NATTEVAAK_COUNTER.navn).increment()
        }
    }

    tilsynsordning.apply {
        if (this.perioder.isNotEmpty()) {
            meterRegistry.counter(Metrikk.TILSYNSORDNING_COUNTER.navn).increment()
        }
    }
}

fun PleipengerLivetsSluttfase.publiserMetrikker(søknad: Søknad, meterRegistry: MeterRegistry) {
    fellesMetrikker(søknad, meterRegistry)

    arbeidstid.apply {
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
}

fun OmsorgspengerMidlertidigAlene.publiserMetrikker(søknad: Søknad, meterRegistry: MeterRegistry) {
    fellesMetrikker(søknad, meterRegistry)
}

fun OmsorgspengerAleneOmsorg.publiserMetrikker(søknad: Søknad, meterRegistry: MeterRegistry) {
    fellesMetrikker(søknad, meterRegistry)
}

fun OmsorgspengerKroniskSyktBarn.publiserMetrikker(søknad: Søknad, meterRegistry: MeterRegistry) {
    fellesMetrikker(søknad, meterRegistry)
}

fun OmsorgspengerUtbetaling.publiserMetrikker(søknad: Søknad, meterRegistry: MeterRegistry) {
    fellesMetrikker(søknad, meterRegistry)
}

fun hentSøknadsperiodeUker(ytelse: Ytelse): Double? {
    val søknadsperiode = runCatching { ytelse.søknadsperiode }.getOrNull() ?: return null
    return ChronoUnit.WEEKS.between(søknadsperiode.fraOgMed, søknadsperiode.tilOgMed).toDouble()
}

fun fellesMetrikker(søknad: Søknad, meterRegistry: MeterRegistry) {
    val ytelse = søknad.getYtelse<Ytelse>()
    val søknadstype = ytelse.type.name
    val søknadsId = søknad.søknadId.id
    val defaultTags = mutableListOf(
        Tag.of("soknadsId", søknadsId),
        Tag.of("soknadstype", søknadstype)
    )
    meterRegistry.Config().commonTags(defaultTags)
    meterRegistry.counter(Metrikk.ANTALL_INNSENDINGER.navn).increment()

    søknad.journalposter.firstOrNull()?.apply {
        val builder = StringBuilder()
        builder.append("IkkeKanPunsjes=" + this.inneholderInformasjonSomIkkeKanPunsjes.toString())
        builder.append(" | ")
        builder.append("MedOpplysninger=" + this.inneholderMedisinskeOpplysninger.toString())

        meterRegistry.counter(
            Metrikk.JOURNALPOST_COUNTER.navn,
            listOf(
                Tag.of("antall_journalposter", søknad.journalposter.size.toString()),
                Tag.of("opplysninger", builder.toString())
            )
        ).increment()
    }
    hentSøknadsperiodeUker(ytelse)?.apply {
        meterRegistry.summary(Metrikk.ANTALL_UKER_SØKNADER_GJELDER_BUCKET.navn, listOf(Tag.of("uker", this.toString()))).record(this)
    }
}

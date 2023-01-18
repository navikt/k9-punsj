package no.nav.k9punsj.metrikker

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import kotlinx.coroutines.runBlocking
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.ytelse.Ytelse
import no.nav.k9.søknad.ytelse.pls.v1.PleipengerLivetsSluttfase
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import org.springframework.stereotype.Service
import java.time.temporal.ChronoUnit

@Service
internal class YtelseMetrikker(
    private val k9SakService: K9SakService,
    private val meterRegistry: MeterRegistry
) {

    fun publiserMetrikker(ytelse: Ytelse, søknad: Søknad) {
        fellesMetrikker(ytelse, søknad)

        when (ytelse) {
            is PleiepengerSyktBarn -> ytelse.publiserMetrikker(søknad)
            is PleipengerLivetsSluttfase -> ytelse.publiserMetrikker(søknad)
        }
    }

    private fun PleiepengerSyktBarn.publiserMetrikker(søknad: Søknad) {
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

        val harEndretArbeidstidsPerioder = runBlocking {
            val ytelse = søknad.getYtelse<PleiepengerSyktBarn>()
            val fagsakYtelseType = FagsakYtelseType.fromKode(ytelse.type.kode())
            val perioderISøknad = ytelse.arbeidstid.frilanserArbeidstidInfo.map {
                it.perioder.values.map {
                    it.
                }
            }
        }
    }

    private fun PleipengerLivetsSluttfase.publiserMetrikker(søknad: Søknad) {
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

    private fun hentSøknadsperiodeUker(ytelse: Ytelse): Double? {
        val søknadsperiode = runCatching { ytelse.søknadsperiode }.getOrNull() ?: return null
        return ChronoUnit.WEEKS.between(søknadsperiode.fraOgMed, søknadsperiode.tilOgMed).toDouble()
    }

    private fun fellesMetrikker(ytelse: Ytelse, søknad: Søknad) {
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
            meterRegistry.summary(
                Metrikk.ANTALL_UKER_SØKNADER_GJELDER_BUCKET.navn,
                listOf(Tag.of("uker", this.toString()))
            ).record(this)
        }
    }
}

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

private val logger = LoggerFactory.getLogger("no.nav.k9punsj.metrikker.SøknadMetrikkerKt.publiserMetrikker")

@Service
class SøknadMetrikkService(
    private val meterRegistry: MeterRegistry
) {
    fun publiserMetrikker(søknad: Søknad) {
        logger.info("Publiserer søknadsmetrikker.")
        val søknadstype = hentType(søknad.getYtelse())
        val søknadsId = søknad.søknadId.id

        meterRegistry.counter(
            ANTALL_INNSENDINGER, listOf(
                Tag.of("soknadId", søknadsId),
                Tag.of("soknadType", søknadstype),
            )
        ).increment()

        hentSøknadsperiode(søknad.getYtelse())?.apply {
            meterRegistry.summary(
                "antall_uker_soknaden_gjelder_histogram", listOf(
                    Tag.of("soknadId", søknadsId),
                    Tag.of("soknadType", søknadstype),
                )
            ).record(this)
        }

        søknad.journalposter.forEach {
            val builder = StringBuilder()
            builder.append("IkkeKanPunsjes=" + it.inneholderInformasjonSomIkkeKanPunsjes.toString())
            builder.append("|")
            builder.append("MedOpplysninger=" + it.inneholderMedisinskeOpplysninger.toString())

            meterRegistry.counter(
                "journalpost_counter", listOf(
                    Tag.of("soknadsId", søknadsId),
                    Tag.of("soknadstype", søknadstype),
                    Tag.of("antall_journalposter", søknad.journalposter.size.toString()),
                    Tag.of("opplysnigner", builder.toString()),
                )
            ).increment()
        }

        hentArbeidstid(søknad.getYtelse())?.apply {
            meterRegistry.summary(
                "antall_arbeidsgivere_counter", listOf(
                    Tag.of("soknadsId", søknadsId),
                    Tag.of("soknadstype", søknadstype)
                )
            ).record(this.arbeidstakerList.size.toDouble())

            this.frilanserArbeidstidInfo.ifPresent {
                meterRegistry.counter(
                    "arbeidstid_frilanser_counter", listOf(
                        Tag.of("soknadsId", søknadsId),
                        Tag.of("soknadstype", søknadstype)
                    )
                ).increment()
            }

            this.selvstendigNæringsdrivendeArbeidstidInfo.ifPresent {
                meterRegistry.counter(
                    "arbeidstid_selvstendig_counter", listOf(
                        Tag.of("soknadsId", søknadsId),
                        Tag.of("soknadstype", søknadstype)
                    )
                ).increment()
            }
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
        val until = søknadsperiode.fraOgMed.until(søknadsperiode.tilOgMed.plusDays(1))
        // ønsker antall uker
        return until.months.div(4).toDouble()
    }

    companion object {
        val ANTALL_INNSENDINGER = "antall_innsendinger_counter"
    }
}

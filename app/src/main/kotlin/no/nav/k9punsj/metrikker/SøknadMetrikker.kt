package no.nav.k9punsj.metrikker

import io.prometheus.client.Counter
import io.prometheus.client.Histogram
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.ytelse.Ytelse
import no.nav.k9.søknad.ytelse.pls.v1.PleipengerLivetsSluttfase
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstid
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("no.nav.k9punsj.metrikker.SøknadMetrikkerKt.publiserMetrikker")

fun Søknad.publiserMetrikker() {
    logger.info("Publiserer søknadsmetrikker.")
    val søknadstype = hentType(this.getYtelse())
    val søknadsId = this.søknadId.id

    antallInnsendinger
        .labels(søknadsId, søknadstype)
        .inc()

    hentSøknadsperiode(this.getYtelse())?.apply {
        periodeSoknadGjelderIUkerHistogram
            .labels(søknadsId, søknadstype)
            .observe(this)
    }

    this.journalposter.forEach {
        val builder = StringBuilder()
        builder.append("IkkeKanPunsjes=" + it.inneholderInformasjonSomIkkeKanPunsjes.toString())
        builder.append("|")
        builder.append("MedOpplysninger=" + it.inneholderMedisinskeOpplysninger.toString())

        journalpost
            .labels(søknadsId, søknadstype, this.journalposter.size.toString(), builder.toString())
            .inc()
    }

    hentArbeidstid(this.getYtelse())?.apply {
        antallArbeidstaker
            .labels(søknadsId, søknadstype)
            .observe(this.arbeidstakerList.size.toDouble())
        this.frilanserArbeidstidInfo.ifPresent {
            arbeidstidFrilanser
                .labels(søknadsId, søknadstype)
                .inc()
        }
        this.selvstendigNæringsdrivendeArbeidstidInfo.ifPresent {
            arbeidstidSelvstendigNæringsdrivende
                .labels(søknadsId, søknadstype)
                .inc()
        }
    }
}


internal const val ANTALL_INNSENDINGER = "antall_innsendinger"
val antallInnsendinger = Counter.build()
    .name(ANTALL_INNSENDINGER)
    .labelNames("soknadsId", "soknadstype")
    .help("Teller antall søknader sendt inn til k9-sak fra k9-punsj")
    .register()


val journalpost = Counter.build()
    .name("journalpost")
    .labelNames("soknadsId", "soknadstype", "antall_journalposter", "opplysnigner")
    .help("Viser oversikt over journalposter som er lagt ved søknaden")
    .register()

val arbeidstidFrilanser = Counter.build()
    .name("arbeidstid_frilanser")
    .labelNames("soknadsId", "soknadstype")
    .help("Har søker frilans")
    .register()

val arbeidstidSelvstendigNæringsdrivende = Counter.build()
    .name("arbeidstid_selvstendig")
    .labelNames("soknadsId", "soknadstype")
    .help("Har søker selvstendig")
    .register()

val periodeSoknadGjelderIUkerHistogram = Histogram.build()
    .name("antall_uker_soknaden_gjelder_histogram")
    .labelNames("soknadsId", "soknadstype")
    .buckets(0.00, 1.00, 4.00, 8.00, 12.00, 16.00, 20.00, 24.00, 28.00, 32.00, 36.00, 40.00, 44.00, 48.00, 52.00)
    .help("Antall uker søknaden gjelder")
    .register()

val antallArbeidstaker = Histogram.build()
    .name("antall_arbeidstaker_en_soker_har")
    .labelNames("soknadsId", "soknadstype")
    .buckets(0.00, 1.00, 2.00, 3.00, 4.00, 5.00, 6.00)
    .help("Antall arbeidstakere")
    .register()


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

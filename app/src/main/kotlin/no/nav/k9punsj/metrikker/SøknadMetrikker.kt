package no.nav.k9punsj.metrikker

import io.prometheus.client.Counter
import io.prometheus.client.Histogram
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.ytelse.Ytelse
import no.nav.k9.søknad.ytelse.pls.v1.PleipengerLivetsSluttfase
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstid


fun Søknad.publiserMetrikker() {
    val søknadstype = hentType(this.getYtelse())
    antallInnsendinger
        .labels(søknadstype)
        .inc()

    periodeSoknadGjelderIUkerHistogram
        .labels(søknadstype)
        .observe(hentSøknadsperiode(this.getYtelse()))

    this.journalposter.forEach{
        val builder = StringBuilder()
        builder.append("IkkeKanPunsjes=" + it.inneholderInformasjonSomIkkeKanPunsjes.toString())
        builder.append("|")
        builder.append("MedOpplysninger=" + it.inneholderMedisinskeOpplysninger.toString())

        journalpost
            .labels(this.søknadId.id, søknadstype, this.journalposter.size.toString(), builder.toString())
            .inc()
    }

    hentArbeidstid(this.getYtelse())?.apply {
        antallArbeidstaker
            .labels(søknadstype)
            .observe(this.arbeidstakerList.size.toDouble())
        this.frilanserArbeidstidInfo.ifPresent {
            arbeidstidFrilanser
                .labels(søknadstype)
                .inc()
        }
        this.selvstendigNæringsdrivendeArbeidstidInfo.ifPresent {
            arbeidstidSelvstendigNæringsdrivende
                .labels(søknadstype)
                .inc()
        }
    }
}


val journalpost = Counter.build()
    .name("journalpost")
    .labelNames("soknadsId", "soknadstype", "antall_journalposter", "opplysnigner")
    .help("Viser oversikt over journalposter som er lagt ved søknaden")
    .register()

val antallInnsendinger = Counter.build()
    .name("antall_innsendinger")
    .labelNames("soknadstype")
    .help("Teller antall søknader sendt inn til k9-sak fra k9-punsj")
    .register()


val soknadsperiode = Counter.build()
    .name("antall_innsendinger")
    .labelNames("soknadstype")
    .help("Teller antall søknader sendt inn til k9-sak fra k9-punsj")
    .register()

val arbeidstidFrilanser = Counter.build()
    .name("arbeidstid_frilanser")
    .labelNames("soknadstype")
    .help("Har søker frilans")
    .register()

val arbeidstidSelvstendigNæringsdrivende = Counter.build()
    .name("arbeidstid_selvstendig")
    .labelNames("soknadstype")
    .help("Har søker selvstendig")
    .register()


val periodeSoknadGjelderIUkerHistogram = Histogram.build()
    .buckets(0.00, 1.00, 4.00, 8.00, 12.00, 16.00, 20.00, 24.00, 28.00, 32.00, 36.00, 40.00, 44.00, 48.00, 52.00)
    .labelNames("soknadstype")
    .name("antall_uker_soknaden_gjelder_histogram")
    .help("Antall uker søknaden gjelder")
    .register()

val antallArbeidstaker = Histogram.build()
    .buckets(0.00, 1.00, 2.00, 3.00, 4.00, 5.00, 6.00)
    .labelNames("soknadstype")
    .name("antall_arbeidstaker_en_søker_har")
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

fun hentSøknadsperiode(ytelse: Ytelse): Double {
    val søknadsperiode = ytelse.søknadsperiode
    //legger på en dag siden until ikke tar med siste
    val until = søknadsperiode.fraOgMed.until(søknadsperiode.tilOgMed.plusDays(1))
    // ønsker antall uker
    return until.months.div(4).toDouble()
}



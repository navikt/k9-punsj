package no.nav.k9.pleiepengersyktbarn.soknad

import no.nav.k9.NorskIdent
import no.nav.k9.søknad.felles.*
import no.nav.k9.søknad.felles.Barn
import no.nav.k9.søknad.felles.Periode
import no.nav.k9.søknad.felles.Språk
import no.nav.k9.søknad.pleiepengerbarn.*
import no.nav.k9.søknad.pleiepengerbarn.Arbeid
import no.nav.k9.søknad.pleiepengerbarn.Tilsynsordning
import org.springframework.context.annotation.Configuration
import java.time.ZoneId
import java.util.*

@Configuration
internal class PleiepengerSyktBarnSoknadConverter {

    fun convert(pleiepengerSyktBarnSoknad: PleiepengerSyktBarnSoknad, ident: NorskIdent): PleiepengerBarnSøknad {

        var tilsynsordningBuilder = Tilsynsordning.builder().iTilsynsordning(when (pleiepengerSyktBarnSoknad.tilsynsordning?.iTilsynsordning) {
            JaNeiVetikke.ja -> TilsynsordningSvar.JA
            JaNeiVetikke.nei -> TilsynsordningSvar.NEI
            else -> TilsynsordningSvar.VET_IKKE
        })
        pleiepengerSyktBarnSoknad.tilsynsordning?.opphold?.forEach{
            tilsynsordningBuilder = tilsynsordningBuilder.uke(
                    TilsynsordningUke.builder()
                            .periode(convertPeriode(it.periode))
                            .mandag(it.mandag)
                            .tirsdag(it.tirsdag)
                            .onsdag(it.onsdag)
                            .torsdag(it.torsdag)
                            .fredag(it.fredag)
                            .build()
            )
        }

        return PleiepengerBarnSøknad.builder()
                .søknadId(SøknadId.of(UUID.randomUUID().toString()))
                .mottattDato(pleiepengerSyktBarnSoknad.datoMottatt?.atStartOfDay()?.atZone(ZoneId.systemDefault()))
                .søker(Søker.builder().norskIdentitetsnummer(NorskIdentitetsnummer.of(ident)).build())
                .barn(if (pleiepengerSyktBarnSoknad.barn?.norskIdent.isNullOrBlank())
                    Barn.builder().fødselsdato(pleiepengerSyktBarnSoknad.barn?.foedselsdato).build()
                    else Barn.builder().norskIdentitetsnummer(NorskIdentitetsnummer.of(pleiepengerSyktBarnSoknad.barn?.norskIdent)).build())
                .språk(Språk.of(pleiepengerSyktBarnSoknad.spraak.toString()))
                .søknadsperioder(pleiepengerSyktBarnSoknad.perioder?.map{convertPeriode(it) to SøknadsperiodeInfo.builder().build()}?.toMap())
                .arbeid(Arbeid.builder()
                        .arbeidstaker(pleiepengerSyktBarnSoknad.arbeid?.arbeidstaker?.map{Arbeidstaker.builder()
                                .norskIdentitetsnummer(NorskIdentitetsnummer.of(it.norskIdent))
                                .organisasjonsnummer(Organisasjonsnummer.of(it.organisasjonsnummer))
                                .perioder(it.skalJobbeProsent?.map{convertPeriode(it.periode) to Arbeidstaker.ArbeidstakerPeriodeInfo.builder().skalJobbeProsent(it.grad?.toBigDecimal()).build()}?.toMap())
                                .build()})
                        .frilanser(pleiepengerSyktBarnSoknad.arbeid?.frilanser?.map{Frilanser.builder().periode(convertPeriode(it.periode), Frilanser.FrilanserPeriodeInfo()).build()})
                        .selvstendigNæringsdrivende(pleiepengerSyktBarnSoknad.arbeid?.selvstendigNaeringsdrivende?.map{SelvstendigNæringsdrivende.builder().periode(convertPeriode(it.periode), SelvstendigNæringsdrivende.SelvstendigNæringsdrivendePeriodeInfo()).build()})
                        .build())
                .beredskap(Beredskap.builder()
                        .perioder(pleiepengerSyktBarnSoknad.beredskap?.map{convertPeriode(it.periode) to Beredskap.BeredskapPeriodeInfo.builder().tilleggsinformasjon(it.tilleggsinformasjon).build()}?.toMap())
                        .build())
                .nattevåk(Nattevåk.builder()
                        .perioder(pleiepengerSyktBarnSoknad.nattevaak?.map{convertPeriode(it.periode) to Nattevåk.NattevåkPeriodeInfo.builder().tilleggsinformasjon(it.tilleggsinformasjon).build()}?.toMap())
                        .build())
                .tilsynsordning(tilsynsordningBuilder.build())
                .build()
    }

    fun convertPeriode(periode: no.nav.k9.pleiepengersyktbarn.soknad.Periode?): Periode {
        return Periode.builder().fraOgMed(periode?.fraOgMed).tilOgMed(periode?.tilOgMed).build()
    }
}
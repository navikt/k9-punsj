package no.nav.k9.pleiepengersyktbarn.soknad

import java.time.LocalDate
import javax.validation.Constraint
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext
import kotlin.reflect.KClass

internal const val MåSettes = "MAA_SETTES"
internal const val MinstEnMåSettes = "MINST_EN_MAA_SETTES"
internal const val MåVæreIFortiden = "MAA_VAERE_I_FORTIDEN"
internal const val NorskIdentMåBeståAv11Sifre = "NORSK_IDENT_MAA_BESTAA_AV_11_SIFRE"
internal const val NorskIdentMåVæreGyldig = "NORSK_IDENT_MAA_VAERE_GYLDIG"
internal const val Duplikat = "DUPLIKAT"
internal const val Overlapp = "OVERLAPP"

@Constraint(validatedBy = arrayOf(SoknadValidator::class))
annotation class ValidPleiepengerSyktBarnSoknad(
        val message: String = MåSettes,
        val groups: Array<KClass<out Any>> = [],
        val payload: Array<KClass<out Any>> = []
)

class SoknadValidator : ConstraintValidator<ValidPleiepengerSyktBarnSoknad, PleiepengerSyktBarnSoknad> {
    override fun isValid(
            søknad: PleiepengerSyktBarnSoknad?,
            context: ConstraintValidatorContext?): Boolean {
        var valid = true

        fun validerPeriode(periode: Periode?, prefix: String, isOverordnetPeriode: Boolean = false) : Boolean {

            val prefixPeriode = if (isOverordnetPeriode) prefix else "$prefix.periode"

            if (periode == null) {
                return withError(context, MåSettes, prefixPeriode)
            }

            if (periode.fraOgMed == null) {
                valid = withError(context, MåSettes, "$prefixPeriode.fraOgMed")
            }

            if (periode.tilOgMed == null) {
                valid = withError(context, MåSettes, "$prefixPeriode.tilOgMed")
            }

            if (periode.tilOgMed != null && periode.fraOgMed != null && periode.tilOgMed.isBefore(periode.fraOgMed)) {
                valid = withError(context, "FRA_OG_MED_MAA_VAERE_FOER_TIL_OG_MED", prefixPeriode)
            }
            return valid
        }

        søknad!!.barn?.apply {
            if (this.foedselsdato == null && this.norskIdent.isNullOrBlank()) {
                valid = withError(context, "NORSK_IDENT_ELLER_FOEDSELSDATO_MAA_SETTES", "barn")
            }

            if (this.foedselsdato != null && this.foedselsdato.isAfter(LocalDate.now())) {
                valid = withError(context, MåVæreIFortiden, "barn.foedselsdato")
            }

            if (!this.norskIdent.isNullOrBlank()) {
                if (!Regex("^\\d{11}$").matches(this.norskIdent)) {
                    valid = withError(context, NorskIdentMåBeståAv11Sifre, "barn.norskIdent")
                } else if (!isValidFnrOrDnr(this.norskIdent)) {
                    valid = withError(context, NorskIdentMåVæreGyldig, "barn.norskIdent")
                }
            }
        }

        søknad.datoMottatt.apply {
            if (this == null) {
                valid = withError(context, MåSettes, "datoMottatt")
            } else if (this.isAfter(LocalDate.now())) {
                valid = withError(context, MåVæreIFortiden, "datoMottatt")
            }
        }

        søknad.perioder?.forEachIndexed { i, periode ->
            val prefix = "perioder[$i]"
            valid = validerPeriode(periode, prefix, true)
            if (søknad.perioder.count{arePeriodsValidAndEqual(it, periode)} > 1) {
                valid = withError(context, Duplikat, prefix)
            } else if (søknad.perioder.count{arePeriodsOverlapping(it, periode)} > 1) {
                valid = withError(context, Overlapp, prefix)
            }
        }

        if (søknad.tilsynsordning?.iTilsynsordning == JaNeiVetikke.ja || søknad.tilsynsordning?.iTilsynsordning == JaNeiVetikke.vetIkke) {

            søknad.nattevaak?.forEachIndexed { i, nattevaak ->
                val prefix = "nattevaak[$i].periode"
                valid = validerPeriode(nattevaak.periode, prefix)
                if (søknad.nattevaak.count{arePeriodsValidAndEqual(it.periode, nattevaak.periode)} > 1) {
                    valid = withError(context, Duplikat, "$prefix.periode")
                } else if (søknad.nattevaak.count{arePeriodsOverlapping(it.periode, nattevaak.periode)} > 1) {
                    valid = withError(context, Overlapp, "$prefix.periode")
                }
            }

            søknad.beredskap?.forEachIndexed { i, beredskap ->
                val prefix = "beredskap[$i].periode"
                valid = validerPeriode(beredskap.periode, prefix)
                if (søknad.beredskap.count{arePeriodsValidAndEqual(it.periode, beredskap.periode)} > 1) {
                    valid = withError(context, Duplikat, "$prefix.periode")
                } else if (søknad.beredskap.count{arePeriodsOverlapping(it.periode, beredskap.periode)} > 1) {
                    valid = withError(context, Overlapp, "$prefix.periode")
                }
            }
        }

        søknad.tilsynsordning?.apply {
            if (this.iTilsynsordning == null) {
                valid = withError(context, MåSettes, "iTilsynsordning")
            } else if (this.iTilsynsordning == JaNeiVetikke.ja) {
                if (this.opphold.isEmpty()) {
                    valid = withError(context, "MAA_OPPGIS_HVIS_AKTIV_I_TILSYNSORDNING", "opphold")
                }
                this.opphold.forEachIndexed { i, opphold ->
                    val prefix = "tilsynsordning.opphold[$i]"
                    valid = validerPeriode(opphold.periode, prefix)
                    if (this.opphold.count{arePeriodsValidAndEqual(it.periode, opphold.periode)} > 1) {
                        valid = withError(context, Duplikat, "$prefix.periode")
                    } else if (this.opphold.count{arePeriodsOverlapping(it.periode, opphold.periode)} > 1) {
                        valid = withError(context, Overlapp, "$prefix.periode")
                    }
                }
            }
        }

        søknad.arbeid?.apply {
            arbeidstaker?.forEachIndexed { i, arbeidstakerItem ->
                val prefix = "arbeid.arbeidstaker[$i]"

                if (arbeidstakerItem.organisasjonsnummer == null && arbeidstakerItem.norskIdent == null) {
                    valid = withError(context, "MAA_ENTEN_HA_ORGNR_ELLER_NORSKIDENT", prefix)
                }
                if (arbeidstakerItem.organisasjonsnummer != null && arbeidstakerItem.norskIdent != null) {
                    valid = withError(context, "KAN_IKKE_HA_BAADE_ORGNR_OG_NORSKIDENT", prefix)
                }
                if (arbeidstakerItem.organisasjonsnummer != null) {
                    if (!Regex("^\\d{9}$").matches(arbeidstakerItem.organisasjonsnummer)) {
                        valid = withError(context, "ORGNR_MAA_BESTAA_AV_9_SIFRE", "$prefix.organisasjonsnummer")
                    } else if (!isValidOrgnr(arbeidstakerItem.organisasjonsnummer)) {
                        valid = withError(context, "ORGNR_MAA_VAERE_GYLDIG", "$prefix.organisasjonsnummer")
                    } else if (arbeidstaker.count{it.organisasjonsnummer == arbeidstakerItem.organisasjonsnummer} > 1) {
                        valid = withError(context, Duplikat, "$prefix.organisasjonsnummer")
                    }
                }
                if (arbeidstakerItem.norskIdent != null) {
                    if (!Regex("^\\d{11}$").matches(arbeidstakerItem.norskIdent)) {
                        valid = withError(context, NorskIdentMåBeståAv11Sifre, "$prefix.norskIdent")
                    } else if (!isValidFnrOrDnr(arbeidstakerItem.norskIdent)) {
                        valid = withError(context, NorskIdentMåVæreGyldig, "$prefix.norskIdent")
                    } else if (arbeidstaker.count{it.norskIdent == arbeidstakerItem.norskIdent} > 1) {
                        valid = withError(context, Duplikat, "$prefix.norskIdent")
                    }
                }

                if (arbeidstakerItem.skalJobbeProsent == null) {
                    valid = withError(context, MåSettes, prefix)
                } else {
                    arbeidstakerItem.skalJobbeProsent.forEachIndexed { j, tilstedevaerelsesgrad ->
                        valid = validerPeriode(tilstedevaerelsesgrad.periode, "arbeid.arbeidstaker[$i].skalJobbeProsent[$j]")
                        if (tilstedevaerelsesgrad.grad!! > 100) {
                            valid = withError(context, "MAA_VAERE_MINDRE_ENN_ELLER_LIK_100", "arbeid.arbeidstaker[$i].skalJobbeProsent[$j].grad")
                        } else if (tilstedevaerelsesgrad.grad < 0) {
                            valid = withError(context, "MAA_VAERE_STOERRE_ENN_ELLER_LIK_0", "arbeid.arbeidstaker[$i].skalJobbeProsent[$j].grad")
                        }
                        if (arbeidstakerItem.skalJobbeProsent.count{arePeriodsValidAndEqual(it.periode, tilstedevaerelsesgrad.periode)} > 1) {
                            valid = withError(context, Duplikat, "arbeid.arbeidstaker[$i].skalJobbeProsent[$j].periode")
                        } else if (arbeidstakerItem.skalJobbeProsent.count{arePeriodsOverlapping(it.periode, tilstedevaerelsesgrad.periode)} > 1) {
                            valid = withError(context, Overlapp, "arbeid.arbeidstaker[$i].skalJobbeProsent[$j].periode")
                        }
                    }
                }
            }

            selvstendigNaeringsdrivende?.forEachIndexed { i,  sn ->
                valid = validerPeriode(sn.periode, "arbeid.selvstendigNaeringsdrivende[$i]")
                if (selvstendigNaeringsdrivende.count{arePeriodsValidAndEqual(it.periode, sn.periode)} > 1) {
                    valid = withError(context, Duplikat, "arbeid.selvstendigNaeringsdrivende[$i].periode")
                } else if (selvstendigNaeringsdrivende.count{arePeriodsOverlapping(it.periode, sn.periode)} > 1) {
                    valid = withError(context, Overlapp, "arbeid.selvstendigNaeringsdrivende[$i].periode")
                }
            }

            frilanser?.forEachIndexed { i, frilanserItem ->
                valid = validerPeriode(frilanserItem.periode, "arbeid.frilanser[$i]")
                if (frilanser.count{arePeriodsValidAndEqual(it.periode, frilanserItem.periode)} > 1) {
                    valid = withError(context, Duplikat, "arbeid.frilanser[$i].periode")
                } else if (frilanser.count{arePeriodsOverlapping(it.periode, frilanserItem.periode)} > 1) {
                    valid = withError(context, Overlapp, "arbeid.frilanser[$i].periode")
                }
            }
        }
        return valid
    }

    private fun isValidFnrOrDnr(ident: String): Boolean {

        val regexFnr = Regex("^((((0[1-9]|[12]\\d|30)(0[469]|11)|(0[1-9]|[12]\\d|3[01])(0[13578]|1[02])|((0[1-9]|1\\d|2[0-8])02))\\d{2})|2902([02468][048]|[13579][26]))\\d{5}\$")
        val regexDnr = Regex("^((((4[1-9]|[56]\\d|70)(0[469]|11)|(4[1-9]|[56]\\d|7[01])(0[13578]|1[02])|((4[1-9]|5\\d|6[0-8])02))\\d{2})|6902([02468][048]|[13579][26]))\\d{5}\$")
        val controlKey1 = listOf(3,7,6,1,8,9,4,5,2)
        val controlKey2 = listOf(5,4,3,2,7,6,5,4,3,2)

        fun isControlDigitValid(controlKey: List<Int>, value: String, controlDigitIndex: Int): Boolean {
            fun digitAt(index: Int, v: String): Int {return v[index].toString().toInt()}
            val controlDigit = 11-(controlKey.indices.fold(0, {s,i -> s+controlKey[i]*digitAt(i, value)}))%11
            return (if (controlDigit == 11) 0 else controlDigit) == digitAt(controlDigitIndex, value)
        }

        return Regex("^\\d{11}$").matches(ident)
                && (regexFnr.matches(ident) || regexDnr.matches(ident))
                && isControlDigitValid(controlKey1, ident, 9)
                && isControlDigitValid(controlKey2, ident, 10)
    }

    private fun isValidOrgnr(orgnr: String): Boolean {

        if (!Regex("^\\d{9}$").matches(orgnr)) {
            return false
        }

        val controlKey = listOf(3,2,7,6,5,4,3,2)
        val controlDigitIndex = 8

        fun digitAt(index: Int, v: String): Int {return v[index].toString().toInt()}

        val controlDigit = 11-(controlKey.indices.fold(0, {s,i -> s+controlKey[i]*digitAt(i, orgnr)}))%11

        return (if (controlDigit == 11) 0 else controlDigit) == digitAt(controlDigitIndex, orgnr)
    }

    private fun isPeriodValid(period: Periode?): Boolean {
        return period?.fraOgMed != null && period.tilOgMed != null && !period.tilOgMed.isBefore(period.fraOgMed)
    }

    private fun arePeriodsValidAndEqual(period1: Periode?, period2: Periode?): Boolean {
        return isPeriodValid(period1) && period1 == period2
    }

    private fun isDateWithinPeriod(date: LocalDate, period: Periode): Boolean {
        return isPeriodValid(period)
                && (date == period.fraOgMed || date == period.tilOgMed || (date.isAfter(period.fraOgMed) && date.isBefore(period.tilOgMed)))
    }

    private fun arePeriodsOverlapping(period1: Periode?, period2: Periode?): Boolean {
        return isPeriodValid(period1)
                && isPeriodValid(period2)
                && (isDateWithinPeriod(period1!!.fraOgMed!!, period2!!) || isDateWithinPeriod(period1.tilOgMed!!, period2))
    }

    private fun withError(
            context: ConstraintValidatorContext?,
            error: String,
            attributt: String) : Boolean {
        context!!.disableDefaultConstraintViolation()
        context.buildConstraintViolationWithTemplate(error)
                .addPropertyNode(attributt)
                .addConstraintViolation()
        return false
    }
}
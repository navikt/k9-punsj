package no.nav.k9.pleiepengersyktbarn.soknad

import java.time.LocalDate
import javax.validation.Constraint
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext
import kotlin.reflect.KClass

internal const val MåSettes = "MAA_SETTES"
internal const val MinstEnMåSettes = "MINST_EN_MAA_SETTES"
internal const val MåVæreIFortiden = "MAA_VAERE_I_FORTIDEN"

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

                val regexFnr = Regex("^((((0[1-9]|[12]\\d|30)(0[469]|11)|(0[1-9]|[12]\\d|3[01])(0[13578]|1[02])|((0[1-9]|1\\d|2[0-8])02))\\d{2})|2902([02468][048]|[13579][26]))\\d{5}\$")
                val regexDnr = Regex("^((((4[1-9]|[56]\\d|70)(0[469]|11)|(4[1-9]|[56]\\d|7[01])(0[13578]|1[02])|((4[1-9]|5\\d|6[0-8])02))\\d{2})|6902([02468][048]|[13579][26]))\\d{5}\$")
                val controlKey1 = listOf(3,7,6,1,8,9,4,5,2)
                val controlKey2 = listOf(5,4,3,2,7,6,5,4,3,2)

                fun isControlDigitValid(controlKey: List<Int>, value: String, controlDigitIndex: Int): Boolean {
                    fun digitAt(index: Int, v: String): Int {return v.get(index).toString().toInt()}
                    val controlDigit = 11-(controlKey.indices.fold(0, {s,i -> s+controlKey[i]*digitAt(i, value)}))%11
                    return (if (controlDigit == 11) 0 else controlDigit) == digitAt(controlDigitIndex, value)
                }

                if (!Regex("^\\d{11}$").matches(this.norskIdent)) {
                    valid = withError(context, "NORSK_IDENT_MAA_BESTAA_AV_11_SIFRE", "barn.norskIdent")
                } else if (
                    (!regexFnr.matches(this.norskIdent) && !regexDnr.matches(this.norskIdent))
                    || !isControlDigitValid(controlKey1, this.norskIdent, 9)
                    || !isControlDigitValid(controlKey2, this.norskIdent, 10)
                ) {
                    valid = withError(context, "NORSK_IDENT_MAA_VAERE_GYLDIG", "barn.norskIdent")
                }
            }
        }

        søknad.datoMottatt.apply {
            if (this == null) {
                valid = withError(context, MåSettes, "datoMottatt");
            } else if (this.isAfter(LocalDate.now())) {
                valid = withError(context, MåVæreIFortiden, "datoMottatt")
            }
        }

        søknad.perioder?.forEachIndexed { i, periode ->
            val prefix = "perioder[$i]"
            valid = validerPeriode(periode, prefix, true)
        }

        if (søknad.tilsynsordning?.iTilsynsordning == JaNeiVetikke.ja || søknad.tilsynsordning?.iTilsynsordning == JaNeiVetikke.vetIkke) {

            søknad.nattevaak?.forEachIndexed { i, nattevaak ->
                val prefix = "nattevaak[$i]"
                valid = validerPeriode(nattevaak.periode, prefix)
            }

            søknad.beredskap?.forEachIndexed { i, beredskap ->
                val prefix = "beredskap[$i]"
                valid = validerPeriode(beredskap.periode, prefix)
            }
        }

        søknad.tilsynsordning?.apply {
            if (this.iTilsynsordning == null) {
                valid = withError(context, MåSettes, "iTilsynsordning")
            }
            else if (this.iTilsynsordning == JaNeiVetikke.ja) {
                if (this.opphold.isEmpty()) {
                    valid = withError(context, "MAA_OPPGIS_HVIS_AKTIV_I_TILSYNSORDNING", "opphold")
                }
                this.opphold.forEachIndexed { i, opphold ->
                    val prefix = "tilsynsordning.opphold[$i]"
                    valid = validerPeriode(opphold.periode, prefix)
                }
            }
        }

        søknad.arbeid?.apply {
            arbeidstaker?.forEachIndexed { i, arbeidstaker ->
                val prefix = "arbeid.arbeidstaker[$i]"

                if (arbeidstaker.organisasjonsnummer == null && arbeidstaker.norskIdent == null) {
                    valid = withError(context, "MAA_ENTEN_HA_ORGNR_ELLER_NORSKIDENT", prefix)
                }
                if (arbeidstaker.organisasjonsnummer != null && arbeidstaker.norskIdent != null) {
                    valid = withError(context, "KAN_IKKE_HA_BAADE_ORGNR_OG_NORSKIDENT", prefix)
                }

                if (arbeidstaker.skalJobbeProsent == null) {
                    valid = withError(context, MåSettes, prefix)
                } else {
                    arbeidstaker.skalJobbeProsent.forEachIndexed { j, tilstedevaerelsesgrad ->
                        valid = validerPeriode(tilstedevaerelsesgrad.periode, "arbeid.arbeidstaker[$i].skalJobbeProsent[$j]")
                    }
                }
            }

            selvstendigNaeringsdrivende?.forEachIndexed { i,  sn ->
                valid = validerPeriode(sn.periode, "arbeid.selvstendigNaeringsdrivende[$i]")
            }

            frilanser?.forEachIndexed { i, frilanser ->
                valid = validerPeriode(frilanser.periode, "arbeid.frilanser[$i]")
            }
        }
        return valid
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
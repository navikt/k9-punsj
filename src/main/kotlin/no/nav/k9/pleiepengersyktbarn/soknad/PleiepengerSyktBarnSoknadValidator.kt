package no.nav.k9.pleiepengersyktbarn.soknad

import javax.validation.Constraint
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext
import kotlin.reflect.KClass

internal const val MåSettes = "MAA_SETTES"
internal const val MåSigneres = "MAA_SIGNERES"
internal const val MinstEnMåSettes = "MINST_EN_MAA_SETTES"

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

        fun validerPeriode(periode: Periode?, prefix: String) : Boolean {
            if (periode == null) {
                return withError(context, MåSettes, "$prefix.periode")
            }

            if (periode.fraOgMed == null) {
                valid = withError(context, MåSettes, "$prefix.periode.fraOgMed")
            }

            if (periode.tilOgMed == null) {
                valid = withError(context, MåSettes, "$prefix.periode.tilOgMed")
            }

            if (periode.tilOgMed != null && periode.fraOgMed != null && periode.tilOgMed.isBefore(periode.fraOgMed)) {
                valid = withError(context, "MAA_VAERE_FOER_TIL_OG_MED", "$prefix.periode.fraOgMed")
            }
            return valid
        }

        fun validerSvar(jaNeiMedTilleggsinformasjon: JaNeiMedTilleggsinformasjon, prefix: String) : Boolean {
            if (jaNeiMedTilleggsinformasjon.svar == null) {
                valid = withError(context, MåSettes, "$prefix.svar")
            }

            return validerPeriode(jaNeiMedTilleggsinformasjon.periode, prefix)
        }

        søknad!!.barn?.apply {
            if (this.foedselsdato == null && this.norskIdent.isNullOrBlank()) {
                valid = withError(context, "NORSK_IDENT_ELLER_FOEDSELSDATO_MAA_SETTES", "barn")
            }
        }

        søknad.nattevaak?.forEachIndexed { i, nattevaak ->
            val prefix = "nattevaar[$i]"
            valid = validerSvar(nattevaak, prefix)
        }

        søknad.beredskap?.forEachIndexed { i, beredskap ->
            val prefix = "beredskap[$i]"
            valid = validerSvar(beredskap, prefix)
        }

        søknad.tilsynsordning?.forEachIndexed{ i, tilsynsordning ->
            val prefix = "tilsynsordning[$i]"
            valid = validerPeriode(tilsynsordning.periode, prefix)
        }

        søknad.signert?.apply {
            if (!this) {
                valid = withError(context, MåSigneres, "signert")
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
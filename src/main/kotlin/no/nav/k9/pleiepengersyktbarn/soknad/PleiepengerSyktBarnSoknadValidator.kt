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

        søknad!!.barn?.apply {
            if (this.foedselsdato == null && this.norsk_ident.isNullOrBlank()) {
                valid = withError(context, "NORSK_IDENT_ELLER_FOEDSELSDATO_MAA_SETTES", "barn")
            }
        }
        søknad.perioder?.forEachIndexed { i, periode ->
            val prefix = "perioder[$i]"

            periode.beredskap?.apply {
                if (this.svar == null) {
                    valid = withError(context, MåSettes, "$prefix.beredskap.svar")
                }
            }

            periode.nattevaak?.apply {
                if (this.svar == null) {
                    valid = withError(context, MåSettes, "$prefix.nattevaak.svar")

                }
            }

            if (periode.fra_og_med == null) {
                valid = withError(context, MåSettes, "$prefix.fra_og_med")
            }

            if (periode.til_og_med == null) {
                valid = withError(context, MåSettes, "$prefix.til_og_med")
            }

            if (periode.til_og_med != null && periode.fra_og_med != null && periode.til_og_med.isBefore(periode.fra_og_med)) {
                valid = withError(context, "MAA_VAERE_FOER_TIL_OG_MED", "$prefix.fra_og_med")
            }
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
        context
                .buildConstraintViolationWithTemplate(error)
                .addPropertyNode(attributt)
                .addConstraintViolation()
        return false
    }

}
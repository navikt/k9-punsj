package no.nav.k9.pleiepengersyktbarn.soknad

import javax.validation.Constraint
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext
import kotlin.reflect.KClass


@Constraint(validatedBy = arrayOf(SoknadValidator::class))
annotation class ValidPleiepengerSyktBarnSoknad(
        val message: String = "UGYLDIG_SOEKNAD",
        val groups: Array<KClass<out Any>> = [],
        val payload: Array<KClass<out Any>> = []
)

class SoknadValidator : ConstraintValidator<ValidPleiepengerSyktBarnSoknad, PleiepengerSyktBarnSoknad> {
    override fun isValid(
            søknad: PleiepengerSyktBarnSoknad?,
            context: ConstraintValidatorContext?): Boolean {
        var valid = true

        søknad!!.barn?.apply {
            if (this.foedselsdato == null && this.norsk_ident == null) {
                valid = withError(context, "MAA_SETTE_NORSK_IDENT_ELLER_FOEDSELSDATO_PAA_BARN", "barn")
            }
        }
        søknad.perioder?.filter { it.fra_og_med != null && it.til_og_med!= null }?.forEach {
            if (it.til_og_med!!.isBefore(it.fra_og_med)) {
                valid = withError(context, "FRA_OG_MED_MAA_VAERE_FOER_TIL_OG_MED", "perioder[].fra_og_med")
            }
        }

        søknad.signert?.apply {
            if (!this) {
                valid = withError(context, "SOEKNADEN_MAA_SIGNERES", "signert")
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
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
            arbeidstaker?.forEachIndexed { i, arbidstaker ->
                val prefix = "arbeid.arbidstakere[$i]"

                if (arbidstaker.organisasjonsnummer == null && arbidstaker.norskIdent == null) {
                    valid = withError(context, "MAA_ENTEN_HA_ORGNR_ELLER_NORSKIDENT", prefix)
                }
                if (arbidstaker.organisasjonsnummer != null && arbidstaker.norskIdent != null) {
                    valid = withError(context, "KAN_IKKE_HA_BAADE_ORGNR_OG_NORSKIDENT", prefix)
                }

                valid = validerPeriode(arbidstaker.periode, prefix)

                if (arbidstaker.skalJobbeProsent == null) {
                    valid = withError(context, MåSettes, prefix)
                }
            }

            selvstendigNaeringsdrivende?.forEachIndexed { i,  sn ->
                valid = validerPeriode(sn.periode, "arbeid.selvstendigNæringsdrivende[$i]")
            }

            frilanser?.forEachIndexed { i, frilanser ->
                valid = validerPeriode(frilanser.periode, "arbeid.frilansere[$i]")
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
        context.buildConstraintViolationWithTemplate(error)
                .addPropertyNode(attributt)
                .addConstraintViolation()
        return false
    }
}
package no.nav.k9punsj.ruting

data class RutingGrunnlag(
    internal val søker: Boolean? = null,
    internal val pleietrengende: Boolean? = null,
    internal val annenPart: Boolean? = null) {
    private val ingenParter = søker == false && pleietrengende == false && annenPart == false
    internal val minstEnPart = søker == true || pleietrengende == true || annenPart == true
    init { require(minstEnPart || ingenParter) {"Minst en part må være true, eller alle false. Var $this"} }
}
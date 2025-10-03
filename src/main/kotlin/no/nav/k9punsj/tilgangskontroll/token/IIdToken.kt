package no.nav.k9punsj.tilgangskontroll.token

interface IIdToken {
    val value: String
    val jwt: JWTToken?
    fun getNavIdent(): String
    fun erSaksbehandler() :Boolean
    fun erVeileder() :Boolean
    fun harBasistilgang() :Boolean
}

package no.nav.k9punsj.tilgangskontroll.token

data class IdTokenLocal(
    override val value: String = ""

) : IIdToken {
    override val jwt: Nothing? = null
    override fun getName(): String = "saksbehandler@nav.no"
    override fun getUsername(): String = "saksbehandler@nav.no"
    override fun erSaksbehandler(): Boolean = true
    override fun erVeileder(): Boolean = false
    override fun harBasistilgang(): Boolean = true
    override fun getNavIdent(): String = "Z000000"
}

package no.nav.k9punsj.tilgangskontroll.token

data class IdTokenLocal(
    override val value: String = ""

) : IIdToken {
    override val jwt: Nothing? = null
    override fun erSaksbehandler(): Boolean = true
    override fun erVeileder(): Boolean = false
    override fun harBasistilgang(): Boolean = true
    override fun getNavIdent(): String = "Z000000"
    override fun getName(): String = "Lokal Saksbehandler"
    override fun harHistoriskTilgang(): Boolean = value
        .takeIf { it.isNotBlank() }
        ?.let { runCatching { IdToken(it).harHistoriskTilgang() }.getOrDefault(false) }
        ?: false
}

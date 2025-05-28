package no.nav.k9punsj.tilgangskontroll.token

import no.nav.k9punsj.utils.objectMapper
import java.nio.charset.Charset
import java.util.Base64

data class IdToken(
    override val value: String
) : IIdToken {
    override val jwt: JWTToken = try {
        val split = value.split(".")
        val header = String(Base64.getDecoder().decode(split[0]), Charset.defaultCharset())
        val body = String(Base64.getDecoder().decode(split[1]), Charset.defaultCharset())
        objectMapper().readValue(body, JWTToken::class.java)
    } catch (cause: Throwable) {
        throw IdTokenInvalidFormatException(cause)
    }

    override fun getName(): String = jwt.name
    override fun getUsername(): String = jwt.preferredUsername
    override fun erSaksbehandler(): Boolean = jwt.groups.any { s -> s == System.getenv("BRUKER_GRUPPE_ID_SAKSBEHANDLER")!! }
    override fun erVeileder(): Boolean = jwt.groups.any { s -> s == System.getenv("BRUKER_GRUPPE_ID_VEILEDER")!! }
    override fun harBasistilgang(): Boolean = erSaksbehandler() || erVeileder()
    override fun getNavIdent(): String = jwt.NAVident
}

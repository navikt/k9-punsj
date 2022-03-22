package no.nav.k9punsj.tilgangskontroll.token

import no.nav.k9punsj.objectMapper
import java.nio.charset.Charset
import java.util.Base64

data class IdToken(
    override val value: String,
) : IIdToken {
    override val jwt: JWTToken = try {
        val split = value.split(".")
        val header = String(Base64.getDecoder().decode(split[0]), Charset.defaultCharset())
        val body = String(Base64.getDecoder().decode(split[1]), Charset.defaultCharset())
        objectMapper().readValue(body, JWTToken::class.java)
    } catch (cause: Throwable) {
        throw IdTokenInvalidFormatException(this, cause)
    }

    override fun getName(): String = jwt.name
    override fun getUsername(): String = jwt.preferredUsername
    override fun kanBehandleKode6(): Boolean = jwt.groups.any { s -> s == "87ea7c87-08a2-43bc-83d6-0bfeee92185d" }
    override fun kanBehandleKode7(): Boolean = jwt.groups.any { s -> s == "69d4a70f-1c83-42a8-8fb8-2f5d54048647" }
    override fun kanBehandleEgneAnsatte(): Boolean = jwt.groups.any { s -> s == "de44052d-b062-4497-89a2-0c85b935b808" }
    override fun erOppgavebehandler(): Boolean = jwt.groups.any { s -> s == "a9f5ef81-4e81-42e8-b368-0273071b64b9" }
}


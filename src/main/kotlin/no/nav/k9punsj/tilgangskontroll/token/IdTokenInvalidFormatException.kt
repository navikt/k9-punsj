package no.nav.k9punsj.tilgangskontroll.token

class IdTokenInvalidFormatException(cause: Throwable? = null) :
    RuntimeException("IdToken er på ugyldig format.", cause)

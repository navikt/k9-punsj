package no.nav.k9punsj.tilgangskontroll.token

class IdTokenInvalidFormatException(idToken: IdToken, cause: Throwable? = null) :
    RuntimeException("$idToken er på ugyldig format.", cause)

package no.nav.k9punsj.sikkerhet.token

class IdTokenInvalidFormatException(idToken: IdToken, cause: Throwable? = null) :
    RuntimeException("$idToken er på ugyldig format.", cause)

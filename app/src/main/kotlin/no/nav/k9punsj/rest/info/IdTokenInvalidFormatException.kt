package no.nav.k9punsj.rest.info

class IdTokenInvalidFormatException(idToken: IdToken, cause: Throwable? = null) :
    RuntimeException("$idToken er på ugyldig format.", cause)

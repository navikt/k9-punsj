package no.nav.k9punsj.tilgangskontroll.token

interface ITokenService {

    fun decodeToken(accessToken: String) : IIdToken
}

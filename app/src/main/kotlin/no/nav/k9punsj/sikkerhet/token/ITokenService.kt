package no.nav.k9punsj.sikkerhet.token

interface ITokenService {

    fun decodeToken(accessToken: String) : IIdToken
}

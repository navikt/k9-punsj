package no.nav.k9punsj.rest.info

interface ITokenService {

    fun decodeToken(accessToken: String) : IIdToken
}

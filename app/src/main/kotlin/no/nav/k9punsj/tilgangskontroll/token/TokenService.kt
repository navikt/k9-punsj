package no.nav.k9punsj.tilgangskontroll.token

import org.springframework.stereotype.Component

@Component
class TokenService : ITokenService {
    override fun decodeToken(accessToken: String): IIdToken {
        return IdToken(accessToken)
    }
}

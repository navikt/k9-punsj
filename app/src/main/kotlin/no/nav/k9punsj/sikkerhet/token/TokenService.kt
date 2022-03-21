package no.nav.k9punsj.sikkerhet.token

import org.springframework.stereotype.Component

@Component
class TokenService : ITokenService {
    override fun decodeToken(accessToken: String): IIdToken {
        return IdToken(accessToken)
    }
}

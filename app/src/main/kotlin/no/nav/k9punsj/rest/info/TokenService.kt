package no.nav.k9punsj.rest.info

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!test")
class TokenService : ITokenService {
    override fun decodeToken(accessToken: String): IIdToken {
        return IdToken(accessToken)
    }
}

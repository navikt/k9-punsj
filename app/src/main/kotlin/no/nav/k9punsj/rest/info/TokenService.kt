package no.nav.k9punsj.rest.info

import no.nav.k9punsj.IkkeTestProfil
import org.springframework.context.annotation.Configuration

@Configuration
@IkkeTestProfil
class TokenService : ITokenService {
    override fun decodeToken(accessToken: String): IIdToken {
        return IdToken(accessToken)
    }
}

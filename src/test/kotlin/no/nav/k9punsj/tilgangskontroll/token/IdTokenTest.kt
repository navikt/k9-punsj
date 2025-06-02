package no.nav.k9punsj.tilgangskontroll.token

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

class IdTokenTest {

    @Test
    fun skal_ikke_feile_når_jwt_body_er_som_forventet() {
        val headerContent = """noe"""
        val bodyContent = """{"aud": "00000000-0000-0000-0000-000000000000","iss": "https://login.foobar.com/00000000-0000-0000-0000-000000000000/v2.0","iat": 1748420376,"nbf": 1748420376,"exp": 1748420377,"aio": "noe","azp": "00000000-0000-0000-0000-000000000001","azpacr": "2","groups": ["00000000-0000-0000-0000-000000000000","00000000-0000-0000-0000-000000000001"],"name": "Bar, Foo","oid": "00000000-0000-0000-0000-000000000000","preferred_username": "Foo.Bar@nav.no","rh": "noe","scp": "noe","sid": "00000000-0000-0000-0000-000000000000","sub": "noe","tid": "00000000-0000-0000-0000-000000000000","uti": "noe","ver": "2.0","xms_ftd": "noe","NAVident": "Z000000","azp_name": "dev-gcp:k9saksbehandling:k9-punsj-frontend"}"""
        val signatur = "unsigned"
        val jwt : String = base64stripped(headerContent) + "." + base64stripped(bodyContent) + "." + signatur

        val idToken = IdToken(jwt)
        assertEquals("Z000000", idToken.getNavIdent())
    }

    @Test
    fun skal_feile_når_forventet_element_NAVident_mangler() {
        val headerContent = """noe"""
        val bodyContent = """{"aud": "00000000-0000-0000-0000-000000000000","iss": "https://login.foobar.com/00000000-0000-0000-0000-000000000000/v2.0","iat": 1748420376,"nbf": 1748420376,"exp": 1748420377,"aio": "noe","azp": "00000000-0000-0000-0000-000000000001","azpacr": "2","groups": ["00000000-0000-0000-0000-000000000000","00000000-0000-0000-0000-000000000001"],"name": "Bar, Foo","oid": "00000000-0000-0000-0000-000000000000","preferred_username": "Foo.Bar@nav.no","rh": "noe","scp": "noe","sid": "00000000-0000-0000-0000-000000000000","sub": "noe","tid": "00000000-0000-0000-0000-000000000000","uti": "noe","ver": "2.0","xms_ftd": "noe","azp_name": "dev-gcp:k9saksbehandling:k9-punsj-frontend"}"""
        val signatur = "unsigned"
        val jwt : String = base64stripped(headerContent) + "." + base64stripped(bodyContent) + "." + signatur

        assertThrows<IdTokenInvalidFormatException> {
            IdToken(jwt)
        }

    }

    fun base64stripped(input: String): String {
        val encoded = Base64.getEncoder().encodeToString(input.toByteArray())
        val stripped = encoded.replace("=", "")
        return stripped
    }
}
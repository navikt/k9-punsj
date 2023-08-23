package no.nav.k9punsj.util

import java.net.URI

object TestUtils {
    fun hentSøknadId(location: URI?): String? {
        val path = location?.path
        val søknadId = path?.substring(path.lastIndexOf('/'))
        return søknadId?.trim('/')
    }
}

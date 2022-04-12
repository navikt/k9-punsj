package no.nav.k9punsj

import no.nav.k9punsj.UriParams.SøknadIdKey

internal object Urls {
    internal const val HenteMappe = "/mappe"
    internal const val HenteSøknad = "/mappe/{${SøknadIdKey}}"
    internal const val NySøknad = "/"
    internal const val OppdaterEksisterendeSøknad = "/oppdater"
    internal const val SendEksisterendeSøknad = "/send"
    internal const val ValiderSøknad = "/valider"
    internal const val HentInfoFraK9sak = "/k9sak/info"
    internal const val HentArbeidsforholdIderFraK9sak = "/k9sak/arbeidsforholdIder"
}

internal object UriParams {
    internal const val SøknadIdKey = "søknadId"
}
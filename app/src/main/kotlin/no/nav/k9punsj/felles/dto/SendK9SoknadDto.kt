package no.nav.k9punsj.felles.dto

import no.nav.k9.søknad.Søknad

data class SendK9SoknadDto(
    val søknadFeil: SøknadFeil?,
    val søknad: Søknad?
)

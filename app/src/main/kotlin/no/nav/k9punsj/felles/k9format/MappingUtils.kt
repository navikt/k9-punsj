package no.nav.k9punsj.felles.k9format

import no.nav.k9.søknad.felles.Feil

object MappingUtils {
    fun <Til> mapEllerLeggTilFeil(feil: MutableList<Feil>, felt: String, map: () -> Til?) = kotlin.runCatching {
        map()
    }.fold(onSuccess = { it }, onFailure = { throwable ->
        feil.add(Feil(felt, throwable.javaClass.simpleName, throwable.message ?: "Ingen feilmelding"))
        null
    })
}

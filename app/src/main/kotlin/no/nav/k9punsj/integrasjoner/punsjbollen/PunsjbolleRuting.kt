package no.nav.k9punsj.integrasjoner.punsjbollen

enum class PunsjbolleRuting {
    K9Sak,
    Infotrygd,
    IkkeStøttet
}

internal fun Boolean.somPunsjbolleRuting() = when (this) {
    true -> PunsjbolleRuting.K9Sak
    false -> PunsjbolleRuting.Infotrygd
}
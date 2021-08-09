package no.nav.k9punsj.rest.eksternt.punsjbollen

enum class PunsjbolleRuting {
    K9Sak,
    Infotrygd,
    IkkeStøttet
}

internal fun Boolean.somPunsjbolleRuting() = when (this) {
    true -> PunsjbolleRuting.K9Sak
    false -> PunsjbolleRuting.Infotrygd
}
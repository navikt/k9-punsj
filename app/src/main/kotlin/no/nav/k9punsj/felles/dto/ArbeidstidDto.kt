package no.nav.k9punsj.felles.dto

data class ArbeidstidDto(
    val arbeidstakerList: List<ArbeidAktivitetDto.ArbeidstakerDto>?,
    val frilanserArbeidstidInfo: ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto?,
    val selvstendigNæringsdrivendeArbeidstidInfo: ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto?
)

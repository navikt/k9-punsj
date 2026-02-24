package no.nav.k9punsj.felles.k9format

import no.nav.k9.søknad.felles.Feil
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstid
import no.nav.k9punsj.felles.dto.ArbeidstidDto

fun ArbeidstidDto.mapTilArbeidstid(feil: MutableList<Feil>, støtterFravær: Boolean = false): Arbeidstid {
    val k9Arbeidstid = Arbeidstid()
    arbeidstakerList?.also {
        k9Arbeidstid.medArbeidstaker(it.mapArbeidstidArbeidstaker(feil, støtterFravær))
    }
    selvstendigNæringsdrivendeArbeidstidInfo?.mapArbeidstid("selvstendigNæringsdrivendeArbeidstidInfo", feil, støtterFravær)?.also {
        k9Arbeidstid.medSelvstendigNæringsdrivendeArbeidstidInfo(it)
    }
    frilanserArbeidstidInfo?.mapArbeidstid("frilanserArbeidstidInfo", feil, støtterFravær)?.also {
        k9Arbeidstid.medFrilanserArbeidstid(it)
    }
    return k9Arbeidstid
}

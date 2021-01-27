package no.nav.k9punsj.pleiepengersyktbarn

import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarnValidator
import no.nav.k9punsj.søknad.Ytelse

internal class PleiepengerSyktBarnYtelseMapper {
    companion object {
        fun map(pleiepengerSyktBarnSoknad: Ytelse): PleiepengerSyktBarn {

            val ytelse: PleiepengerSyktBarn = PleiepengerSyktBarn()
            valider(ytelse)

            return ytelse
        }

        private fun valider(ytelse: PleiepengerSyktBarn) {
            PleiepengerSyktBarnValidator().forsikreValidert(ytelse)
        }
    }
}

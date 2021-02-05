package no.nav.k9punsj.domenetjenester.mappers

import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarnValidator
import no.nav.k9punsj.db.datamodell.Ytelse

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

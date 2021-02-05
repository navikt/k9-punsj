package no.nav.k9punsj.db.datamodell

import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.ytelse.Ytelse


interface Ytelse {

   fun getType() : FagsakYtelseType

   fun getSøknadsperiode() : Periode

   /** @return andre berørte, kjente identifiserte personer (enn søker) - f.eks. barn, ektefelle, verge etc. som er involveres i denne saken.*/
   fun getBerørtePersoner() : List<Person>

   fun mapTilEksternYtelse() : Ytelse

}








package no.nav.k9punsj.søknad

import no.nav.k9punsj.person.Person
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.ytelse.Ytelse


interface Ytelse {

   fun getType() : Type

   fun getSøknadsperiode() : Periode

   /** @return andre berørte, kjente identifiserte personer (enn søker) - f.eks. barn, ektefelle, verge etc. som er involveres i denne saken.*/
   fun getBerørtePersoner() : List<Person>

   fun mapTilEksternYtelse() : Ytelse

   enum class Type(val kode: String) {
      OMSORGSPENGER_UTBETALING("OMP_UT"),
      PLEIEPENGER_SYKT_BARN("PLEIEPENGER_SYKT_BARN");

   }
}








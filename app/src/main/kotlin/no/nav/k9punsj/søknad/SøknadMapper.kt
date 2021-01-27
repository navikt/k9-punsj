package no.nav.k9punsj.søknad

import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Versjon
import no.nav.k9.søknad.felles.personopplysninger.Søker
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.SøknadId
import no.nav.k9punsj.person.Person
import org.springframework.context.annotation.Configuration
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*


internal class SøknadMapper{
        companion object {
                fun map(søknadId: UUID, ytelse: Ytelse, søker: Person): Søknad {
                        return Søknad(
                                SøknadId.of(søknadId.toString()),
                                Versjon.of(""),
                                ZonedDateTime.now(),
                                Søker.builder().norskIdentitetsnummer(NorskIdentitetsnummer.of(søker.personIdent.ident))
                                        .build(),
                                ytelse.mapTilEksternYtelse()
                        )
                }
        }
}



package no.nav.k9punsj.domenetjenester.mappers

import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Versjon
import no.nav.k9.søknad.felles.personopplysninger.Søker
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.SøknadId
import no.nav.k9punsj.db.datamodell.NorskIdent
import no.nav.k9punsj.db.datamodell.Ytelse
import java.time.ZonedDateTime
import java.util.UUID


internal class SøknadMapper{
        companion object {
                fun map(søknadId: UUID, ytelse: Ytelse, norskIdent: NorskIdent): Søknad {
                        return Søknad(
                                SøknadId.of(søknadId.toString()),
                                Versjon.of(""),
                                ZonedDateTime.now(),
                                Søker.builder().norskIdentitetsnummer(NorskIdentitetsnummer.of(norskIdent))
                                        .build(),
                                ytelse.mapTilEksternYtelse()
                        )
                }
        }
}



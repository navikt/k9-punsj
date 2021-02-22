package no.nav.k9punsj.domenetjenester.mappers


import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.Validator
import no.nav.k9.søknad.felles.Feil
import no.nav.k9.søknad.felles.Versjon
import no.nav.k9.søknad.felles.personopplysninger.Søker
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.SøknadId
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadDto
import java.time.ZonedDateTime
import java.util.UUID


internal class SøknadMapper {
    companion object {
        private val validator = Validator()

        fun mapTilEksternFormat(søknad: PleiepengerSøknadDto): Pair<Søknad, List<Feil>>{
            val ytelse = søknad.ytelse
            val pleiepengerSyktBarn = PleiepengerSyktBarnYtelseMapper.map(ytelse!!)

            val søknadK9Format = opprettSøknad(
                søker = Søker(NorskIdentitetsnummer.of(søknad.søker?.norskIdentitetsnummer)),
                ytelse = pleiepengerSyktBarn
            )
            val feil = validator.valider(søknadK9Format)

            return Pair(søknadK9Format, feil)
        }

        private fun opprettSøknad(søknadId: UUID = UUID.randomUUID(), versjon: Versjon = Versjon.of("1.0.0"), mottattDato: ZonedDateTime = ZonedDateTime.now(), søker: Søker, ytelse: no.nav.k9.søknad.ytelse.Ytelse) : Søknad {
            return Søknad(SøknadId.of(søknadId.toString()), versjon, mottattDato, søker, ytelse)
        }
    }
}



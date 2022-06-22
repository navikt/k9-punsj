package no.nav.k9punsj.rest.eksternt.pdl

import no.nav.k9punsj.TestProfil
import no.nav.k9punsj.integrasjoner.pdl.IdentPdl
import no.nav.k9punsj.integrasjoner.pdl.PdlResponse
import no.nav.k9punsj.integrasjoner.pdl.PdlService
import no.nav.k9punsj.integrasjoner.pdl.Personopplysninger
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
@TestProfil
internal class TestPdlService : PdlService {
    private val dummyFnr = "11111111111"
    private val dummyAktørId = "1000000000000"
    private val harBarn = "66666666666"
    private val barn = setOf("77777777777", "88888888888", "99999999999")

    override suspend fun identifikator(fnummer: String): PdlResponse {
        val identer = IdentPdl.Data.HentIdenter.Identer(gruppe = "AKTORID", false, dummyAktørId)
        val identPdl = IdentPdl(IdentPdl.Data(IdentPdl.Data.HentIdenter(identer = listOf(identer))), null)
        return PdlResponse(false, identPdl)
    }

    override suspend fun identifikatorMedAktørId(aktørId: String): PdlResponse {
        val identer = IdentPdl.Data.HentIdenter.Identer(gruppe = "FOLKEREGISTERIDENT", false, dummyFnr)
        val identPdl = IdentPdl(IdentPdl.Data(IdentPdl.Data.HentIdenter(identer = listOf(identer))), null)

        return PdlResponse(false, identPdl)
    }

    override suspend fun aktørIdFor(fnummer: String): String {
        return dummyAktørId
    }

    override suspend fun hentBarn(identitetsnummer: String) = when (identitetsnummer == harBarn) {
        true -> barn
        false -> emptySet()
    }

    override suspend fun hentPersonopplysninger(identitetsnummer: Set<String>) = when (identitetsnummer) {
        barn -> setOf(
            Personopplysninger(
                identitetsnummer = "77777777777",
                fødselsdato = LocalDate.parse("2005-12-12"),
                fornavn = "Ola",
                mellomnavn = null,
                etternavn = "Nordmann",
                gradering = Personopplysninger.Gradering.STRENGT_FORTROLIG
            ),
            Personopplysninger(
                identitetsnummer = "88888888888",
                fødselsdato = LocalDate.parse("2005-12-12"),
                fornavn = "Kari",
                mellomnavn = "Mellomste",
                etternavn = "Nordmann",
                gradering = Personopplysninger.Gradering.UGRADERT
            ),
            Personopplysninger(
                identitetsnummer = "99999999999",
                fødselsdato = LocalDate.parse("2004-06-24"),
                fornavn = "Pål",
                mellomnavn = null,
                etternavn = "Nordmann",
                gradering = Personopplysninger.Gradering.UGRADERT
            )
        )
        setOf(harBarn) -> setOf(
            Personopplysninger(
                identitetsnummer = harBarn,
                fødselsdato = LocalDate.parse("1980-05-06"),
                fornavn = "Søker",
                mellomnavn = null,
                etternavn = "Søkersen",
                gradering = Personopplysninger.Gradering.STRENGT_FORTROLIG_UTLAND
            )
        )
        else -> setOf()
    }
}

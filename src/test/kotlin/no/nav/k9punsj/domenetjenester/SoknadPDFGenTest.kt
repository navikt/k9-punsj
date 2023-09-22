package no.nav.k9punsj.domenetjenester

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.k9punsj.innsending.journalforjson.HtmlGenerator
import no.nav.k9punsj.innsending.journalforjson.PdfGenerator
import no.nav.k9punsj.utils.objectMapper
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File

class SoknadPDFGenTest {

    private fun pdfPath(id: String) = "${System.getProperty("user.dir")}/generated-pdf-$id.pdf"

    @Test
    @Disabled
    internal fun `Tester mapping på punsj innsending PDF`() {
        val jsonPayload = """
            {"søknadId":"febd8b95-0fc4-4c9a-928f-5fda60ad7e1d","versjon":"1.0.0","mottattDato":"2020-10-12T10:53:00.000Z","søker":{"norskIdentitetsnummer":"02020050123"},"ytelse":{"type":"PLEIEPENGER_LIVETS_SLUTTFASE","pleietrengende":{"norskIdentitetsnummer":"22222222222","fødselsdato":null},"søknadsperiode":["2018-12-30/2019-10-20"],"trekkKravPerioder":[],"opptjeningAktivitet":{"selvstendigNæringsdrivende":[{"perioder":{"2018-12-30/..":{"virksomhetstyper":["FISKE"],"regnskapsførerNavn":"Regskapsfører","regnskapsførerTlf":"88888889","erVarigEndring":true,"endringDato":"2018-12-30","endringBegrunnelse":"Dota2 er best","bruttoInntekt":1.3E+6,"erNyoppstartet":false,"registrertIUtlandet":false,"erFiskerPåBladB":true}},"organisasjonsnummer":"910909088","virksomhetNavn":"FiskerAS"}],"frilanser":{"startdato":"2019-10-10","sluttdato":null}},"bosteder":{"perioder":{"2018-12-30/2019-10-20":{"land":"RU"}},"perioderSomSkalSlettes":{}},"utenlandsopphold":{"perioder":{"2018-12-30/2019-10-20":{"land":"RU","årsak":null}},"perioderSomSkalSlettes":{}},"arbeidstid":{"arbeidstakerList":[{"norskIdentitetsnummer":null,"organisasjonsnummer":"910909088","organisasjonsnavn":null,"arbeidstidInfo":{"perioder":{"2018-12-30/2019-10-20":{"jobberNormaltTimerPerDag":"PT7H29M","faktiskArbeidTimerPerDag":"PT7H29M"}}}}],"frilanserArbeidstidInfo":{"perioder":{"2018-12-30/2019-10-20":{"jobberNormaltTimerPerDag":"PT6H","faktiskArbeidTimerPerDag":"PT5H"}}},"selvstendigNæringsdrivendeArbeidstidInfo":{"perioder":{"2018-12-30/2019-10-20":{"jobberNormaltTimerPerDag":"PT7H","faktiskArbeidTimerPerDag":"PT4H"}}}},"uttak":{"perioder":{"2018-12-30/2019-10-20":{"timerPleieAvBarnetPerDag":"PT7H30M"}}},"lovbestemtFerie":{"perioder":{"2018-12-30/2019-10-20":{"skalHaFerie":true}}},"dataBruktTilUtledning":null},"språk":"nb","journalposter":[{"inneholderInfomasjonSomIkkeKanPunsjes":null,"inneholderInformasjonSomIkkeKanPunsjes":true,"inneholderMedisinskeOpplysninger":false,"journalpostId":"7523521"}],"begrunnelseForInnsending":{"tekst":null},"kildesystem":null,"punsjet av":"saksbehandler@nav.no"}
        """.trimIndent()

        val soeknadObject = objectMapper().readValue<ObjectNode>(jsonPayload)
        val pdf = PdfGenerator.genererPdf(
            html = HtmlGenerator.genererHtml(
                tittel = "Innsending fra Punsj",
                json = soeknadObject
            )
        )

        File(pdfPath("punsjinnsending")).writeBytes(pdf)
    }
}
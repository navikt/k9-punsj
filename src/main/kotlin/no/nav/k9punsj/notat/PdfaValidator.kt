package no.nav.k9punsj.notat

import org.verapdf.core.EncryptedPdfException
import org.verapdf.core.ModelParsingException
import org.verapdf.core.ValidationException
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider
import org.verapdf.pdfa.Foundries
import org.verapdf.pdfa.flavours.PDFAFlavour
import org.verapdf.pdfa.results.TestAssertion
import org.verapdf.pdfa.results.ValidationResult
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

/**
 * bruker VeraPDF for å validere at produsert pdf er gyldig PDFA og dermed egnet for arkivering
 *
 *
 * For dokumentasjon, se https://docs.verapdf.org/develop/
 */
class PdfaValidator {

    init {
        VeraGreenfieldFoundryProvider.initialise()
    }

    fun validatePdf(pdf: ByteArray) {
        try {
            validatePdf(ByteArrayInputStream(pdf))
        } catch (e: ModelParsingException) {
            throw PdfaValideringException("Feil ved parsing av pdf modell", e)
        } catch (e: EncryptedPdfException) {
            throw PdfaValideringException("Klarer ikke å håndtere kryptert pdf", e)
        } catch (e: IOException) {
            throw PdfaValideringException("IO exception ved validering av pdf", e)
        } catch (e: ValidationException) {
            throw PdfaValideringException("Validering av pdf feilet", e)
        }
    }

    @Throws(ModelParsingException::class, EncryptedPdfException::class, IOException::class, ValidationException::class)
    fun validatePdf(inputStream: InputStream) {
        val flavour = PDFAFlavour.fromString("2u")
        Foundries.defaultInstance().createValidator(flavour, false).use { validator ->
            Foundries.defaultInstance().createParser(inputStream, flavour).use { parser ->
                val result = validator.validate(parser)
                if (!result.isCompliant) {
                    throw PdfaValideringException(result)
                }
            }
        }
    }
}

class PdfaValideringException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(result: ValidationResult) : this(result.formater())

    companion object {
        private fun ValidationResult.formater(): String {
            val feilmeldinger = testAssertions
                .filter { ta: TestAssertion -> ta.status != TestAssertion.Status.PASSED }
                .map { ta: TestAssertion -> ta.status.toString() + ":" + ta.message }
                .toList()
            return "Validering av pdf feilet. Validerer versjon $pdfaFlavour feil er: " + java.lang.String.join(
                ", ",
                feilmeldinger
            )
        }
    }
}

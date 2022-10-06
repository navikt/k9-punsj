package no.nav.k9punsj.utils

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.Template
import com.github.jknack.handlebars.context.MapValueResolver
import com.github.jknack.handlebars.io.ClassPathTemplateLoader
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import com.openhtmltopdf.slf4j.Slf4jLogger
import com.openhtmltopdf.util.XRLog
import org.springframework.core.io.ClassPathResource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Baseklasse for generering av pdf.
 *
 * Bruk: Implementer denne klassen med type T som ønskes generert på pdf.
 *
 * @property templateNavn er navnet på handlebars template-filen.
 * Altså har du en handlebars template fil ved navn søknad.hbs, skal `templateNavn` være søknad.
 *
 * @property bilder laster inn bilder man ønsker skal kunne brukes i templaten.
 *
 * @property tilMap metoden mapper opp instansen T til en map som kan parses i templaten.
 */
abstract class PDFGenerator<in T> {
    protected abstract val templateNavn: String
    protected abstract val bilder: Map<String, String>

    private val ROOT = "handlebars"
    private val REGULAR_FONT = ClassPathResource("$ROOT/fonts/SourceSansPro-Regular.ttf").inputStream.readAllBytes()
    private val BOLD_FONT = ClassPathResource("$ROOT/fonts/SourceSansPro-Bold.ttf").inputStream.readAllBytes()
    private val ITALIC_FONT = ClassPathResource("$ROOT/fonts/SourceSansPro-Italic.ttf").inputStream.readAllBytes()
    private val sRGBColorSpace = ClassPathResource("$ROOT/sRGB.icc").inputStream.readAllBytes()
    protected val handlebars = configureHandlebars()
    private val søknadsTemplate: Template = handlebars.compile(templateNavn)

    protected val ZONE_ID = ZoneId.of("Europe/Oslo")
    protected val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZONE_ID)
    protected val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZONE_ID)

    init {
        XRLog.setLoggingEnabled(true)
        XRLog.setLoggerImpl(Slf4jLogger())
    }

    abstract fun T.tilMap(): Map<String, Any?>

    fun genererPDF(melding: T): ByteArray = søknadsTemplate.apply(
        Context
            .newBuilder(melding.tilMap())
            .resolver(MapValueResolver.INSTANCE)
            .build()
    ).let { html ->
        val outputStream = ByteArrayOutputStream()
        PdfRendererBuilder()
            .useFastMode()
            .usePdfUaAccessbility(true)
            .usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_2_U)
            .useColorProfile(sRGBColorSpace)
            .withHtmlContent(html, "")
            .medFonter()
            .toStream(outputStream)
            .buildPdfRenderer()
            .createPDF()

        outputStream.use {
            val pdf = it.toByteArray()
            valider(pdf)
            pdf
        }
    }

    private fun valider(pdf: ByteArray) {
        PdfaValidator().validatePdf(pdf)
    }

    protected fun loadPng(name: String): String {
        val bytes = ClassPathResource("$ROOT/images/$name.png").inputStream.readAllBytes()
        val base64string = Base64.getEncoder().encodeToString(bytes)
        return "data:image/png;base64,$base64string"
    }

    private fun configureHandlebars(): Handlebars {
        return Handlebars(ClassPathTemplateLoader("/$ROOT")).apply {
            registerHelper(
                "image",
                Helper<String> { context, _ ->
                    if (context == null) "" else bilder[context]
                }
            )
            registerHelper(
                "eq",
                Helper<String> { context, options ->
                    if (context == options.param(0)) options.fn() else options.inverse()
                }
            )
            registerHelper(
                "eqTall",
                Helper<Int> { context, options ->
                    if (context == options.param(0)) options.fn() else options.inverse()
                }
            )
            registerHelper(
                "fritekst",
                Helper<String> { context, _ ->
                    if (context == null) "" else {
                        val text = Handlebars.Utils.escapeExpression(context)
                            .toString()
                            .replace(Regex("\\r\\n|[\\n\\r]"), "<br/>")
                        Handlebars.SafeString(text)
                    }
                }
            )
            registerHelper(
                "jaNeiSvar",
                Helper<Boolean> { context, _ ->
                    if (context == true) "Ja" else "Nei"
                }
            )

            infiniteLoops(true)
        }
    }

    private fun PdfRendererBuilder.medFonter() =
        useFont(
            { ByteArrayInputStream(REGULAR_FONT) },
            "Source Sans Pro",
            400,
            BaseRendererBuilder.FontStyle.NORMAL,
            false
        )
            .useFont(
                { ByteArrayInputStream(BOLD_FONT) },
                "Source Sans Pro",
                700,
                BaseRendererBuilder.FontStyle.NORMAL,
                false
            )
            .useFont(
                { ByteArrayInputStream(ITALIC_FONT) },
                "Source Sans Pro",
                400,
                BaseRendererBuilder.FontStyle.ITALIC,
                false
            )
}

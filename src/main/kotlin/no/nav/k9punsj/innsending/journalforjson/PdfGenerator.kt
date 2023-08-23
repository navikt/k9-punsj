package no.nav.k9punsj.innsending.journalforjson

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import com.openhtmltopdf.util.XRLog
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

internal object PdfGenerator {
    private fun String.fraResources() = PdfGenerator::class.java.getResourceAsStream(this)!!.readBytes()
    private val colorProfile = "/pdf/sRGB2014.icc".fraResources()
    private val REGULAR_FONT = "/pdf/fonts/SourceSansPro-Regular.ttf".fraResources()
    private val BOLD_FONT = "/pdf/fonts/SourceSansPro-Bold.ttf".fraResources()
    private val ITALIC_FONT = "/pdf/fonts/SourceSansPro-Italic.ttf".fraResources()

    init {
        XRLog.setLoggingEnabled(false)
    }

    internal fun genererPdf(
        html: String
    ) = ByteArrayOutputStream().apply {
        PdfRendererBuilder()
            .usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_2_U)
            .useFont({ ByteArrayInputStream(REGULAR_FONT) }, "Source Sans Pro",
                400, BaseRendererBuilder.FontStyle.NORMAL, false)
            .useFont({ ByteArrayInputStream(BOLD_FONT) }, "Source Sans Pro",
                700, BaseRendererBuilder.FontStyle.NORMAL, false)
            .useFont({ ByteArrayInputStream(ITALIC_FONT) }, "Source Sans Pro",
                400, BaseRendererBuilder.FontStyle.ITALIC, false)
            .withHtmlContent(html, null)
            .useColorProfile(colorProfile)
            .toStream(this)
            .run()
    }.toByteArray()
}
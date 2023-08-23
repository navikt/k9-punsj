package no.nav.k9punsj.notat

import org.junit.jupiter.api.Test
import java.io.File

internal class NotatPDFGeneratorTest {

    private val notatPDFGenerator = NotatPDFGenerator()

    @Test
    internal fun test() {
        val pdf = notatPDFGenerator.genererPDF(
            NotatOpplysninger(
                søkerIdentitetsnummer = "29099012345",
                søkerNavn = "Trane Kreativ",
                fagsakId = "ABC123",
                tittel = "Opprettelse av notat",
                saksbehandlerNavn = "Saksbehandler Isaksen",
                saksbehandlerEnhet = "4403",
                notat = """
                Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec egestas neque non augue varius vulputate. 
                Nulla ultrices neque elit, id commodo mauris mattis in. Mauris rhoncus rutrum orci, at molestie purus placerat sed. 
                Integer luctus eleifend nisi sed malesuada. Ut sed nisi justo. Nam commodo eu sapien at efficitur. 
                Etiam tincidunt ex non justo congue auctor. Phasellus massa eros, interdum eget nibh in, viverra condimentum velit. 
                Curabitur ac lectus scelerisque, placerat ex eget, posuere ligula.

                Duis at magna sed neque dictum hendrerit. Phasellus ultrices massa vitae turpis molestie posuere. 
                Sed non placerat urna. Maecenas in ultrices magna. Ut in arcu leo. Duis in luctus metus. 
                Suspendisse ac neque venenatis, ornare ipsum et, mattis elit. Fusce aliquam enim eu dui ullamcorper, sit amet egestas diam molestie. 
                Vestibulum eget sollicitudin odio. Nam placerat dapibus ante eu luctus. 
                Sed a ipsum fringilla, tincidunt enim et, consequat lacus. 
                Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; 
                Donec luctus nibh quis enim ullamcorper, eu pellentesque erat vehicula. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. 
                Aliquam erat volutpat. Nulla facilisi.
                """.trimIndent()
            )
        )
        File(pdfPath("notat")).writeBytes(pdf)
    }

    private fun pdfPath(id: String) = "${System.getProperty("user.dir")}/generated-pdf-$id.pdf"
}

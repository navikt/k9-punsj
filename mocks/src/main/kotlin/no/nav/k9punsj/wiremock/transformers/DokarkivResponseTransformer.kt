package no.nav.k9punsj.wiremock.transformers

import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.extension.ResponseTransformer
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import org.json.JSONObject
import org.slf4j.LoggerFactory

internal class DokarkivResponseTransformer : ResponseTransformer() {
    override fun transform(
        request: Request?,
        response: Response?,
        files: FileSource?,
        parameters: Parameters?
    ): Response {
        val requestEntity = request!!.bodyAsString
        val logger = LoggerFactory.getLogger(DokarkivResponseTransformer::class.java)
        logger.info("Request entity: {}", requestEntity)
        val tema = JSONObject(requestEntity).getString("tema")

        val journalpostId = when {
            requestEntity.contains("-") && "OMS" == tema -> "1"
            requestEntity.contains("NAV 09-11.05") && "OMS" == tema -> "1"
            requestEntity.contains("NAV 09-06.05") && "OMS" == tema -> "2"
            requestEntity.contains("NAV 09-35.01") && "OMS" == tema -> "3"
            requestEntity.contains("NAV 09-35.02") && "OMS" == tema -> "4"
            requestEntity.contains("NAV 09-06.08") && "OMS" == tema -> "5"
            requestEntity.contains("NAV 09-11.08") && "OMS" == tema -> "6"
            requestEntity.contains("NAV 00-03.02") && "FRI" == tema -> "7"
            requestEntity.contains("NAV 09-06.07") && "OMS" == tema -> "8"
            requestEntity.contains("NAVe 09-11.05") && "OMS" == tema -> "9"
            requestEntity.contains("NAVe 09-06.05") && "OMS" == tema -> "10"
            requestEntity.contains("NAVe 09-35.01") && "OMS" == tema -> "11"
            requestEntity.contains("NAVe 09-35.02") && "OMS" == tema -> "12"
            requestEntity.contains("NAVe 09-06.07") && "OMS" == tema -> "13"
            requestEntity.contains("NAVe 09-06.08") && "OMS" == tema -> "14"
            requestEntity.contains("NAV 09-06.10") && "OMS" == tema -> "15"
            requestEntity.contains("NAV 09-12.05") && "OMS" == tema -> "16"
            requestEntity.contains("NAVe 09-12.05") && "OMS" == tema -> "17"
            else -> throw IllegalArgumentException("Ikke støttet brevkode.")
        }

        return Response.Builder.like(response)
            .body(getResponse(journalpostId))
            .build()
    }

    override fun getName(): String {
        return "dokarkiv"
    }

    override fun applyGlobally(): Boolean {
        return false
    }
}

private fun getResponse(journalpostId: String) =
    """       
        {
          "journalpostId": "$journalpostId",
          "journalstatus": "M",
          "melding": null,
          "journalpostferdigstilt": false,
          "dokumenter": [
            {
              "dokumentInfoId": "485201432"
            },
            {
              "dokumentInfoId": "485201433"
            }
          ]
        }
    """.trimIndent()

package no.nav.k9

import java.lang.System.setProperty

internal object MockConfiguration {
    private val azureJwk = """
        {
            "kid": "imAKid",
            "kty": "RSA",
            "n": "vTs0mS-huLVOv7_EaaIHoqkM3Rz1TOImAEVdQK6PZqqQLnbRC5yszxuqOOFPvw8QFY3HT2iUrkxlVPkW3Z9LAXS3dmZKw0MJboLHvusdmLFn0FhIgbldRyAxJ4UcepLJdcR4xofW_MgIH34xkjEDY-dSeDB4fiKi1_8lPTJYuVP5vAywfV3Z_R7msK6rlvl0g28SsOZrxJ9OC6nH3cVsT75vZcmd2eip7LLGCkO8-V9qGgAYUjocn7x6-0XlPVilCF8ic6PNClwe4bmjDR2a_SbDSc3akE8vxaMtINt49CcPfUhkPPm_0mfWsayCXzuwBfUeTaXF_ABCxkipYYpu6w",
            "e": "AQAB",
            "d": "p-V0Eca1UtFrga6AcskUxToA897RttmgpfTlfJJlIc6MBu3dJNRqb4g4TCd9PiP7PWSCRu6fnNajwfUQWKsRPcV1UlQIWZ-NKsRWvgqWQ_iEB9OM4ay6GnVxp4LvdcHvhdJA5sV39uj0bBznlrJuM6H3BjTbc-7_VW5IeDfHiQZ2pWW4DSbiwBhIUYu13IspZcsyk-fLfU-asS_lpz-Mc7XdP56xmstc9D22rOhBCz2NWpamM2UaqRS1zdn1V0wULjfO-tRMGghef_LaAuEEeOhhd-rw_wS89MfPdAoHvJWEyBVgQAS9LqxwRfrcjIu5pEf51Q0VrlV7dij8K1-KQQ",
            "p": "3auUKk-tkDtyQFr8bW_aA74hkVIBuRbnWe5F-r3O66Vy40tWHhJnGCyncCWb5f_k146ZwxwNlooheJS6T3bi4hsI5bbj3ElE_jSR_7SBVEab4bHbGkYpER4AUCHxfZwD8PJEoLZ4f14U87cBYL1GNEyZnLUitSjCDDmfJfI5LBM",
            "q": "2omKIMQetGibKTS60lMAxcVn4kQiCX3_spnrBIxLuEgXGYrOAqbsRVdSc07wKAljE-ig2SN1EaLKHyWVBxdxd4KMZqxbmh6HNuYru412ilPwZM5csLasU9TAD1yYCtju1Bj2GMU6awnc3hKoTXeWZWBrK4eubX4nb2WZxvfwXMk",
            "dp": "Eh6NTOwYZtrFGweU7Kkg6_9lpQhMBcIehRZZ-AX93Ps4KeYlku20KaC0yxD37lP9c7U_Ulh_r9d4pu-ZTxeLsim9j3FkrMP8dL79VCaAD9B5u3gbTcmAX9rQ8bvkjnzrQY28GFrx_I9HLSi_XxX5oBrGz61qud4sBm3LWYG0NKs",
            "dq": "CxnLc2ii6qUZpJkyGDbxJhql8T9mvzawQ2FAJ-X8fqriyYBcgJP8EnWiEYtj9ZSsfLlnWkBL1Q6A194v2MFfGSP_f8Onj4eXdLlyZT-FUvd6kZRN7wgIbuWyr9UTQBHO5-UwswdptUA2AO3PsMevUwz3xKlKufMbi7QMgKfdhMk",
            "qi": "vTdHKVXjTPkhe5IIzv-YxhANMIsBZVT4OFF2a0eZr06anwM-tEJCkCJTjlkQmjBqKtjbYaubTXAnX6_uTRpfNVmhhpws_hRsN6fmGBdQXwe7wSlzudrctpv-02ABRnT0EGBi9r9LSRATzh22uGwoan2xfChuUKZhy4uZqxcicbI"
        }
        """.trimIndent()

    private val configMap = mapOf(
            "AZURE_client_id" to "k9-punsj",
            "AZURE_jwk" to azureJwk,
            "AZURE_token_endpoint" to "http://localhost:8081/oauth2/v2.0/token",
            "AZURE_V1_discovery_url" to "https://login.microsoftonline.com/966ac572-f5b7-4bbe-aa88-c76419c0f851/.well-known/openid-configuration",
            "AZURE_V2_discovery_url" to "https://login.microsoftonline.com/966ac572-f5b7-4bbe-aa88-c76419c0f851/v2.0/.well-known/openid-configuration",
            "SAF_BASE_URL" to "http://localhost:8081/saf",
            "SAF_HENTE_JOURNALPOST_SCOPES" to "saf-client-id/.default",
            "SAF_HENTE_DOKUMENT_SCOPES" to "saf-client-id/.default"
    )

    internal fun config() = configMap
}

internal fun Map<String, String>.setAsProperties() = forEach { key, value ->
    setProperty(key, value)
}
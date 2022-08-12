package no.nav.syfo.kuhrsar

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import no.nav.syfo.azuread.AccessTokenClient
import no.nav.syfo.log

class KuhrSarClient(
    private val endpointUrl: String,
    private val accessTokenClient: AccessTokenClient,
    private val scope: String,
    private val httpClient: HttpClient
) {
    suspend fun getSamhandler(ident: String, callId: String): List<Samhandler> {
        val accessToken = accessTokenClient.getAccessToken(scope)
        val httpResponse = httpClient.get("$endpointUrl/sar/rest/v2/samh") {
            accept(ContentType.Application.Json)
            header("Authorization", "Bearer $accessToken")
            parameter("ident", ident)
        }

        when (httpResponse.status) {
            HttpStatusCode.OK -> {
                log.info("Hentet samhandler for callId {}", callId)
                return httpResponse.body<List<Samhandler>>()
            }
            else -> {
                log.warn("Kuhr-sar svarte med kode {} for callId {}", httpResponse.status, callId)
                throw IOException("Kuhr-sar svarte med uventet kode ${httpResponse.status} for $callId")
            }
        }
    }
}

package no.nav.syfo.btsys

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import no.nav.syfo.client.StsOidcClient
import no.nav.syfo.log

class BtsysClient(
    private val endpointUrl: String,
    private val stsClient: StsOidcClient,
    private val httpClient: HttpClient
) {
    suspend fun getSuspensjonsstatus(
        behandlerFnr: String,
        oppslagsdato: String,
        callId: String,
        consumerId: String
    ): Suspendert {
        val httpResponse =
            httpClient.get("$endpointUrl/api/v1/suspensjon/status") {
                accept(ContentType.Application.Json)
                val oidcToken = stsClient.oidcToken()
                headers {
                    append("Nav-Call-Id", callId)
                    append("Nav-Consumer-Id", consumerId)
                    append("Nav-Personident", behandlerFnr)

                    append("Authorization", "Bearer ${oidcToken.access_token}")
                }
                parameter("oppslagsdato", oppslagsdato)
            }

        when (httpResponse.status) {
            HttpStatusCode.OK -> {
                log.info("Hentet supensjonstatus for callId {}", callId)
                return httpResponse.call.response.body<Suspendert>()
            }
            else -> {
                log.warn("Btsys svarte med kode {} for callId {}", httpResponse.status, callId)
                throw IOException(
                    "Btsys svarte med uventet kode ${httpResponse.status} for $callId"
                )
            }
        }
    }
}

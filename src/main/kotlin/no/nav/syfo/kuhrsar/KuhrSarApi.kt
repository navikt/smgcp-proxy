package no.nav.syfo.kuhrsar

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.utils.io.errors.IOException
import no.nav.syfo.log

fun Route.registerKuhrSarApi(kuhrSarClient: KuhrSarClient) {
    get("/kuhrsar/sar/rest/v2/samh") {
        log.info("Mottatt proxy-request for kuhr-sar")
        val ident = call.request.queryParameters["ident"]
        val callId = call.request.headers["Nav-Call-Id"]

        if (ident.isNullOrEmpty()) {
            log.warn("Ident mangler")
            call.respond(HttpStatusCode.BadRequest, "Ident mangler")
            return@get
        } else if (callId.isNullOrEmpty()) {
            log.warn("CallId mangler")
            call.respond(HttpStatusCode.BadRequest, "CallId mangler")
            return@get
        }

        try {
            val samhandler = kuhrSarClient.getSamhandler(
                ident = ident,
                callId = callId
            )
            call.respond(samhandler)
        } catch (e: IOException) {
            log.error("Noe gikk galt ved kall til kuhr-sar: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, e.message ?: "Noe gikk galt ved proxykall til kuhr-sar")
        }
    }
}

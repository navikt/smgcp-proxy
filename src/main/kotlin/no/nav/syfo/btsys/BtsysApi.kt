package no.nav.syfo.btsys

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.utils.io.errors.IOException
import no.nav.syfo.log

fun Route.registerBtsysApi(btsysClient: BtsysClient) {
    get("/btsys/api/v1/suspensjon/status") {
        log.info("Mottatt proxy-request for btsys")
        val behandlerFnr = call.request.headers["Nav-Personident"]
        val callId = call.request.headers["Nav-Call-Id"]
        val consumerId = call.request.headers["Nav-Consumer-Id"]
        val oppslagsdato = call.request.queryParameters["oppslagsdato"]

        if (behandlerFnr.isNullOrEmpty()) {
            log.warn("BehandlerFnr mangler")
            call.respond(HttpStatusCode.BadRequest, "BehandlerFnr mangler")
            return@get
        } else if (callId.isNullOrEmpty()) {
            log.warn("CallId mangler")
            call.respond(HttpStatusCode.BadRequest, "CallId mangler")
            return@get
        } else if (consumerId.isNullOrEmpty()) {
            log.warn("ConsumerId mangler")
            call.respond(HttpStatusCode.BadRequest, "ConsumerId mangler")
            return@get
        } else if (oppslagsdato.isNullOrEmpty()) {
            log.warn("Oppslagsdato mangler")
            call.respond(HttpStatusCode.BadRequest, "Oppslagsdato mangler")
            return@get
        }

        try {
            val suspensjonsstatus =
                btsysClient.getSuspensjonsstatus(
                    behandlerFnr = behandlerFnr,
                    oppslagsdato = oppslagsdato,
                    callId = callId,
                    consumerId = consumerId
                )
            call.respond(suspensjonsstatus)
        } catch (e: IOException) {
            log.error("Noe gikk galt ved kall til Btsys: ${e.message}")
            call.respond(
                HttpStatusCode.InternalServerError,
                e.message ?: "Noe gikk galt ved proxykall til btsys"
            )
        }
    }
}

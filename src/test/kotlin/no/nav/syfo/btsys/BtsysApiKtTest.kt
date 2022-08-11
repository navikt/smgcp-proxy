package no.nav.syfo.btsys

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.core.spec.style.FunSpec
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.syfo.client.OidcToken
import no.nav.syfo.client.StsOidcClient
import no.nav.syfo.log
import org.amshove.kluent.shouldBeEqualTo
import java.net.ServerSocket
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.TimeUnit

class BtsysApiKtTest : FunSpec({
    val fnr = "12345678910"
    val oppslagsdato = DateTimeFormatter.ISO_DATE.format(LocalDateTime.now())

    val httpClient = HttpClient(Apache) {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            jackson {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
    }
    val mockHttpServerPort = ServerSocket(0).use { it.localPort }
    val mockHttpServerUrl = "http://localhost:$mockHttpServerPort"
    val mockServer = embeddedServer(Netty, mockHttpServerPort) {
        install(ContentNegotiation) {
            jackson {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
        routing {
            get("/api/v1/suspensjon/status") {
                when (call.request.headers["Nav-Personident"]) {
                    fnr -> {
                        call.respond(Suspendert(false))
                    }
                }
            }
        }
    }.start()

    val stsOidcClient = mockk<StsOidcClient>()
    val btsysClient = BtsysClient(mockHttpServerUrl, stsOidcClient, httpClient)

    afterSpec {
        mockServer.stop(TimeUnit.SECONDS.toMillis(1), TimeUnit.SECONDS.toMillis(1))
    }

    beforeEach {
        coEvery { stsOidcClient.oidcToken() } returns OidcToken("token", "type", 1L)
    }

    context("Btsys-api") {
        with(TestApplicationEngine()) {
            start()
            application.install(ContentNegotiation) {
                jackson {
                    registerKotlinModule()
                    registerModule(JavaTimeModule())
                    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                }
            }
            application.install(CallId) {
                generate { UUID.randomUUID().toString() }
                verify { callId: String -> callId.isNotEmpty() }
                header(HttpHeaders.XCorrelationId)
            }
            application.install(StatusPages) {
                exception<Throwable> { call, cause ->
                    log.error("Caught exception", cause)
                    call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Unknown error")
                }
            }
            application.routing { registerBtsysApi(btsysClient) }

            test("Happy-case") {
                with(
                    handleRequest(HttpMethod.Get, "/btsys/api/v1/suspensjon/status?oppslagsdato=$oppslagsdato") {
                        addHeader("Nav-Personident", fnr)
                        addHeader("Nav-Call-Id", "callId")
                        addHeader("Nav-Consumer-Id", "consumerId")
                    }
                ) {
                    response.status() shouldBeEqualTo HttpStatusCode.OK
                    response.content shouldBeEqualTo "{\"suspendert\":false}"
                }
            }
            test("Returnerer bad request hvis oppslagsdato mangler") {
                with(
                    handleRequest(HttpMethod.Get, "/btsys/api/v1/suspensjon/status") {
                        addHeader("Nav-Personident", fnr)
                        addHeader("Nav-Call-Id", "callId")
                        addHeader("Nav-Consumer-Id", "consumerId")
                    }
                ) {
                    response.status() shouldBeEqualTo HttpStatusCode.BadRequest
                }
            }
            test("Returnerer bad request hvis behandlerFnr mangler") {
                with(
                    handleRequest(HttpMethod.Get, "/btsys/api/v1/suspensjon/status?oppslagsdato=$oppslagsdato") {
                        addHeader("Nav-Call-Id", "callId")
                        addHeader("Nav-Consumer-Id", "consumerId")
                    }
                ) {
                    response.status() shouldBeEqualTo HttpStatusCode.BadRequest
                }
            }
            test("Returnerer bad request hvis consumerId mangler") {
                with(
                    handleRequest(HttpMethod.Get, "/btsys/api/v1/suspensjon/status?oppslagsdato=$oppslagsdato") {
                        addHeader("Nav-Personident", fnr)
                        addHeader("Nav-Call-Id", "callId")
                    }
                ) {
                    response.status() shouldBeEqualTo HttpStatusCode.BadRequest
                }
            }
            test("Returnerer bad request hvis callId mangler") {
                with(
                    handleRequest(HttpMethod.Get, "/btsys/api/v1/suspensjon/status?oppslagsdato=$oppslagsdato") {
                        addHeader("Nav-Personident", fnr)
                        addHeader("Nav-Consumer-Id", "consumerId")
                    }
                ) {
                    response.status() shouldBeEqualTo HttpStatusCode.BadRequest
                }
            }
        }
    }
})

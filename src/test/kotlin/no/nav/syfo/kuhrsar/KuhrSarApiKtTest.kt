package no.nav.syfo.kuhrsar

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
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
import no.nav.syfo.azuread.AccessTokenClient
import no.nav.syfo.log
import org.amshove.kluent.shouldBeEqualTo
import java.net.ServerSocket
import java.util.UUID
import java.util.concurrent.TimeUnit

class KuhrSarApiKtTest : FunSpec({
    val objectMapper: ObjectMapper = ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }
    val ident = "12345678910"
    val samhandlerliste = listOf(
        objectMapper.readValue(
            KuhrSarApiKtTest::class.java.getResourceAsStream("/kuhrsar_response.json"),
            Samhandler::class.java
        )
    )

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
            get("/sar/rest/v2/samh") {
                when (call.request.queryParameters["ident"]) {
                    ident -> call.respond(samhandlerliste)
                    else -> call.respond(HttpStatusCode.BadRequest)
                }
            }
        }
    }.start()

    val accessTokenClient = mockk<AccessTokenClient>()
    val kuhrSarClient = KuhrSarClient(mockHttpServerUrl, accessTokenClient, "scope", httpClient)

    afterSpec {
        mockServer.stop(TimeUnit.SECONDS.toMillis(1), TimeUnit.SECONDS.toMillis(1))
    }

    beforeEach {
        coEvery { accessTokenClient.getAccessToken(any()) } returns "token"
    }

    context("Kuhr-sar-api") {
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
            application.routing { registerKuhrSarApi(kuhrSarClient) }

            test("Happy-case") {
                with(
                    handleRequest(HttpMethod.Get, "/kuhrsar/sar/rest/v2/samh?ident=$ident") {
                        addHeader("Nav-Call-Id", "callId")
                    }
                ) {
                    response.status() shouldBeEqualTo HttpStatusCode.OK
                    response.content shouldBeEqualTo objectMapper.writeValueAsString(samhandlerliste)
                }
            }
            test("Returnerer 500 hvis kall mot kuhr-sar feiler") {
                with(
                    handleRequest(HttpMethod.Get, "/kuhrsar/sar/rest/v2/samh?ident=123") {
                        addHeader("Nav-Call-Id", "callId")
                    }
                ) {
                    response.status() shouldBeEqualTo HttpStatusCode.InternalServerError
                }
            }
            test("Returnerer bad request hvis ident mangler") {
                with(
                    handleRequest(HttpMethod.Get, "/kuhrsar/sar/rest/v2/samh") {
                        addHeader("Nav-Call-Id", "callId")
                    }
                ) {
                    response.status() shouldBeEqualTo HttpStatusCode.BadRequest
                }
            }
            test("Returnerer bad request hvis callId mangler") {
                with(
                    handleRequest(HttpMethod.Get, "/kuhrsar/sar/rest/v2/samh?ident=$ident")
                ) {
                    response.status() shouldBeEqualTo HttpStatusCode.BadRequest
                }
            }
        }
    }
})

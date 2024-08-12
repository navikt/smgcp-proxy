package no.nav.syfo.ws

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.FormPart
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.runBlocking

class StsOidcClient(
    username: String,
    password: String,
    private val stsUrl: String = "http://security-token-service/rest/v1/sts/token",
    private val apiKey: String? = null
) {
    private var tokenExpires: Long = 0
    private val oidcClient =
        HttpClient(Apache) {
            install(ContentNegotiation) {
                jackson {
                    registerKotlinModule()
                    registerModule(JavaTimeModule())
                    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                }
            }
            install(Auth) {
                basic {
                    credentials { BasicAuthCredentials(username = username, password = password) }
                    sendWithoutRequest { true }
                }
            }
        }

    private var oidcToken: OidcToken = runBlocking { oidcToken() }

    suspend fun oidcToken(): OidcToken {
        if (tokenExpires < System.currentTimeMillis()) {
            oidcToken = newOidcToken()
            tokenExpires = System.currentTimeMillis() + (oidcToken.expires_in - 600) * 1000
        }
        return oidcToken
    }

    private suspend fun newOidcToken(): OidcToken =
        oidcClient
            .post(stsUrl) {
                if (!apiKey.isNullOrBlank()) {
                    header("x-nav-apikey", apiKey)
                }
                formData(FormPart("grant_type", "client_credentials"), FormPart("scope", "openid"))
            }
            .body()
}

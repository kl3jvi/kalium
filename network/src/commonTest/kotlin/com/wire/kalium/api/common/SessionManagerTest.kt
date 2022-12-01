package com.wire.kalium.api.common

import com.wire.kalium.api.TEST_BACKEND_CONFIG
import com.wire.kalium.api.json.model.testCredentials
import com.wire.kalium.network.AuthenticatedNetworkClient
import com.wire.kalium.network.api.base.authenticated.AccessTokenApi
import com.wire.kalium.network.api.base.model.AccessTokenDTO
import com.wire.kalium.network.api.base.model.AssetId
import com.wire.kalium.network.api.base.model.ProxyCredentialsDTO
import com.wire.kalium.network.api.base.model.RefreshTokenDTO
import com.wire.kalium.network.api.base.model.SessionDTO
import com.wire.kalium.network.api.v0.authenticated.AccessTokenApiV0
import com.wire.kalium.network.api.v0.authenticated.AssetApiV0
import com.wire.kalium.network.session.SessionManager
import com.wire.kalium.network.session.installAuth
import com.wire.kalium.network.tools.ServerConfigDTO
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.RefreshTokensParams
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SessionManagerTest {

    @Test
    fun givenClientWithAuth_whenServerReturns401_thenShouldTryAgainWithNewToken() = runTest {
        val sessionManager = createFakeSessionManager()

        val loadToken: suspend () -> BearerTokens? = {
            val session = sessionManager.session() ?: error("missing user session")
            BearerTokens(accessToken = session.accessToken, refreshToken = session.refreshToken)
        }

        val refreshToken: suspend RefreshTokensParams.() -> BearerTokens? = {
            val newSession = sessionManager.updateToken(AccessTokenApiV0(client), oldTokens!!.accessToken, oldTokens!!.refreshToken)
            newSession?.let {
                BearerTokens(accessToken = it.accessToken, refreshToken = it.refreshToken)
            }
        }

        val bearerAuthProvider = BearerAuthProvider(refreshToken, loadToken, { true }, null)

        var callCount = 0
        var didFail = false
        val mockEngine = MockEngine() {
            callCount++
            // Fail only the first time, so the test can
            // proceed when sessionManager is called again
            if (!didFail) {
                assertEquals("Bearer ${testCredentials.accessToken}", it.headers[HttpHeaders.Authorization])
                didFail = true
                respondError(status = HttpStatusCode.Unauthorized)
            } else {
                assertEquals("Bearer $UPDATED_ACCESS_TOKEN", it.headers[HttpHeaders.Authorization])
                respondOk()
            }
        }

        val client = HttpClient(mockEngine) {
            installAuth(bearerAuthProvider)
            expectSuccess = false
        }

        client.get(TEST_BACKEND_CONFIG.links.api)
        assertEquals(2, callCount)
    }

    @Test
    fun givenClientWithAuth_whenServerReturnsOK_thenShouldNotAddBearerWWWAuthHeader() = runTest {
        val sessionManager = createFakeSessionManager()

        val loadToken: suspend () -> BearerTokens? = {
            val session = sessionManager.session() ?: error("missing user session")
            BearerTokens(accessToken = session.accessToken, refreshToken = session.refreshToken)
        }

        val refreshToken: suspend RefreshTokensParams.() -> BearerTokens? = {
            val newSession = sessionManager.updateToken(AccessTokenApiV0(client), oldTokens!!.accessToken, oldTokens!!.refreshToken)
            newSession?.let {
                BearerTokens(accessToken = it.accessToken, refreshToken = it.refreshToken)
            }
        }

        val bearerAuthProvider = BearerAuthProvider(refreshToken, loadToken, { true }, null)

        val mockEngine = MockEngine() {
            respondOk()
        }

        val client = HttpClient(mockEngine) {
            installAuth(bearerAuthProvider)
            expectSuccess = false
        }

        val response = client.get(TEST_BACKEND_CONFIG.links.api)

        assertNull(response.headers[HttpHeaders.WWWAuthenticate])
    }

    @Test
    fun givenClientWithAuth_whenServerReturns401ForAssetDownload_thenShouldTryAgainWithNewToken() = runTest {
        val sessionManager = createFakeSessionManager()

        val loadToken: suspend () -> BearerTokens? = {
            val session = sessionManager.session() ?: error("missing user session")
            BearerTokens(accessToken = session.accessToken, refreshToken = session.refreshToken)
        }

        val refreshToken: suspend RefreshTokensParams.() -> BearerTokens? = {
            val newSession = sessionManager.updateToken(AccessTokenApiV0(client), oldTokens!!.accessToken, oldTokens!!.refreshToken)
            newSession?.let {
                BearerTokens(accessToken = it.accessToken, refreshToken = it.refreshToken)
            }
        }

        val bearerAuthProvider = BearerAuthProvider(refreshToken, loadToken, { true }, null)

        var callCount = 0
        var didFail = false
        val mockEngine = MockEngine() {
            callCount++
            // Fail only the first time, so the test can
            // proceed when sessionManager is called again
            if (!didFail) {
                assertEquals("Bearer ${testCredentials.accessToken}", it.headers[HttpHeaders.Authorization])
                didFail = true
                respondError(status = HttpStatusCode.Unauthorized)
            } else {
                assertEquals("Bearer $UPDATED_ACCESS_TOKEN", it.headers[HttpHeaders.Authorization])
                respondOk()
            }
        }

        val client = AuthenticatedNetworkClient(mockEngine, sessionManager.serverConfig(), bearerAuthProvider, false)
        val assetApi = AssetApiV0(client)
        val kaliumFileSystem: FileSystem = FakeFileSystem()
        val tempPath = "some-dummy-path".toPath()
        val tempOutputSink = kaliumFileSystem.sink(tempPath)

        assetApi.downloadAsset(AssetId("asset_id", "asset_domain"), null, tempFileSink = tempOutputSink)
        assertEquals(2, callCount)
    }

    private companion object {
        const val UPDATED_ACCESS_TOKEN = "new access token"
    }

    private fun createFakeSessionManager() = object : SessionManager {
        override suspend fun session(): SessionDTO = testCredentials
        override fun serverConfig(): ServerConfigDTO = TEST_BACKEND_CONFIG
        override suspend fun updateToken(
            accessTokenApi: AccessTokenApi,
            oldAccessToken: String,
            oldRefreshToken: String
        ): SessionDTO? = testCredentials.copy(accessToken = UPDATED_ACCESS_TOKEN)

        override suspend fun updateLoginSession(newAccessTokeDTO: AccessTokenDTO, newRefreshTokenDTO: RefreshTokenDTO?): SessionDTO? =
            testCredentials

        override fun proxyCredentials(): ProxyCredentialsDTO? = ProxyCredentialsDTO("username", "password")
    }
}
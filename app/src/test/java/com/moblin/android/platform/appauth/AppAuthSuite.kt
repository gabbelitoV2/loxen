package com.moblin.android.platform.appauth

import android.net.Uri
import androidx.activity.ComponentActivity
import com.moblin.android.platform.uikit.UIViewController
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.TokenResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppAuthSuite {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        AppAuthTestSupport.useServer(server)
    }

    @After
    fun tearDown() {
        AppAuthTestSupport.restore()
        server.shutdown()
    }

    private fun configuration(): OIDServiceConfiguration {
        return OIDServiceConfiguration(
            AuthorizationServiceConfiguration(
                Uri.parse(server.url("/o/oauth2/v2/auth").toString()),
                Uri.parse(server.url("/token").toString()),
            ),
        )
    }

    private fun request(configuration: OIDServiceConfiguration = configuration()): OIDAuthorizationRequest {
        return OIDAuthorizationRequest(
            configuration = configuration,
            clientId = "client",
            clientSecret = null,
            scopes = listOf("https://www.googleapis.com/auth/youtube"),
            redirectURL = "com.example.app:/",
            responseType = OIDResponseTypeCode,
            additionalParameters = null,
        )
    }

    private fun authorizedState(accessToken: String, expiresAt: Instant): OIDAuthState {
        val request = request().request
        val response = AuthorizationResponse.Builder(request)
            .setAuthorizationCode("code")
            .setState(request.state)
            .build()
        val state = AuthState(response, null)
        state.update(
            TokenResponse.Builder(response.createTokenExchangeRequest())
                .setAccessToken(accessToken)
                .setTokenType("Bearer")
                .setAccessTokenExpirationTime(expiresAt.toEpochMilli())
                .setRefreshToken("refresh")
                .build(),
            null,
        )
        return OIDAuthState(state)
    }

    private fun presentingActivity(): ComponentActivity {
        return Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
    }

    @Test
    fun discoveryReadsTheIssuerConfigurationOnTheMainThread() {
        server.enqueue(AppAuthTestSupport.discoveryResponse(server, "https://accounts.example.com"))
        var result: OIDServiceConfiguration? = null
        var done = false
        var mainThread = false
        OIDAuthorizationService.discoverConfiguration(forIssuer = "https://accounts.example.com") { configuration, _ ->
            result = configuration
            mainThread = android.os.Looper.myLooper() == android.os.Looper.getMainLooper()
            done = true
        }
        AppAuthTestSupport.waitFor { done }
        assertTrue(done)
        assertTrue(mainThread)
        assertEquals(server.url("/o/oauth2/v2/auth").toString(), result?.authorizationEndpoint)
        assertEquals(server.url("/token").toString(), result?.tokenEndpoint)
        assertEquals("/.well-known/openid-configuration", server.takeRequest().path)
    }

    @Test
    fun failedDiscoveryGivesNoConfiguration() {
        server.enqueue(okhttp3.mockwebserver.MockResponse().setResponseCode(500))
        var done = false
        var result: OIDServiceConfiguration? = null
        var error: Throwable? = null
        OIDAuthorizationService.discoverConfiguration(forIssuer = "https://accounts.example.com") { c, e ->
            result = c
            error = e
            done = true
        }
        AppAuthTestSupport.waitFor { done }
        assertTrue(done)
        assertNull(result)
        assertNotNull(error)
    }

    @Test
    fun theRequestCarriesTheSwiftParameters() {
        val request = request().request
        assertEquals("client", request.clientId)
        assertEquals("code", request.responseType)
        assertEquals("https://www.googleapis.com/auth/youtube", request.scope)
        assertEquals(Uri.parse("com.example.app:/"), request.redirectUri)
        assertNotNull(request.codeVerifier)
        assertNotNull(request.state)
    }

    @Test
    fun onlyAnActivityThatCanPresentMakesAUserAgent() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        assertNotNull(OIDExternalUserAgentIOS(presenting = UIViewController(controller.get())))
        controller.pause().stop().destroy()
        assertNull(OIDExternalUserAgentIOS(presenting = UIViewController(controller.get())))
    }

    @Test
    fun signInPresentsTheRequestAndExchangesTheCode() {
        AppAuthTestSupport.installBrowser()
        server.enqueue(AppAuthTestSupport.tokenResponse("access"))
        val activity = presentingActivity()
        val userAgent = assertNotNull(OIDExternalUserAgentIOS(presenting = UIViewController(activity)))
        var done = false
        var result: OIDAuthState? = null
        var error: Throwable? = null
        val session = OIDAuthState.authState(byPresenting = request(), externalUserAgent = userAgent) { state, e ->
            result = state
            error = e
            done = true
        }
        assertNotNull(session)
        val started = AppAuthTestSupport.startedAuthorization(activity)
        val uri = AppAuthTestSupport.authorizationUri(started)
        assertEquals("client", uri.getQueryParameter("client_id"))
        assertEquals("com.example.app:/", uri.getQueryParameter("redirect_uri"))
        assertEquals("S256", uri.getQueryParameter("code_challenge_method"))
        assertFalse(done)
        AppAuthTestSupport.completeAuthorization(activity, started, code = "the-code")
        AppAuthTestSupport.waitFor { done }
        assertTrue(done)
        assertNull(error)
        val state = assertNotNull(result)
        assertTrue(state.isAuthorized)
        assertEquals("access", state.lastTokenResponse?.accessToken)
        val expiration = assertNotNull(state.lastTokenResponse?.accessTokenExpirationDate)
        assertTrue(Duration.between(Instant.now(), expiration) > Duration.ofMinutes(55))
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("code=the-code"), body)
        assertTrue(body.contains("grant_type=authorization_code"), body)
        assertTrue(body.contains("code_verifier="), body)
    }

    @Test
    fun cancellingTheBrowserGivesAnError() {
        AppAuthTestSupport.installBrowser()
        val activity = presentingActivity()
        val userAgent = assertNotNull(OIDExternalUserAgentIOS(presenting = UIViewController(activity)))
        var done = false
        var result: OIDAuthState? = null
        var error: Throwable? = null
        OIDAuthState.authState(byPresenting = request(), externalUserAgent = userAgent) { state, e ->
            result = state
            error = e
            done = true
        }
        AppAuthTestSupport.cancelAuthorization(activity, AppAuthTestSupport.startedAuthorization(activity))
        AppAuthTestSupport.waitFor { done }
        assertTrue(done)
        assertNull(result)
        assertEquals(
            AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW.code,
            (error as? AuthorizationException)?.code,
        )
        assertEquals(0, server.requestCount)
    }

    @Test
    fun withoutABrowserSignInFailsAtOnce() {
        val activity = presentingActivity()
        val userAgent = assertNotNull(OIDExternalUserAgentIOS(presenting = UIViewController(activity)))
        var done = false
        var error: Throwable? = null
        OIDAuthState.authState(byPresenting = request(), externalUserAgent = userAgent) { _, e ->
            error = e
            done = true
        }
        AppAuthTestSupport.waitFor { done }
        assertTrue(done)
        assertNotNull(error)
    }

    @Test
    fun performActionUsesAFreshTokenOnTheMainThread() {
        val state = authorizedState("fresh", Instant.now().plus(Duration.ofHours(1)))
        var token: String? = null
        var done = false
        state.performAction { accessToken, _, _ ->
            token = accessToken
            done = true
        }
        AppAuthTestSupport.waitFor { done }
        assertEquals("fresh", token)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun performActionRefreshesAnExpiredToken() {
        server.enqueue(AppAuthTestSupport.tokenResponse("renewed"))
        val state = authorizedState("old", Instant.now().minus(Duration.ofMinutes(5)))
        var token: String? = null
        var done = false
        state.performAction { accessToken, _, _ ->
            token = accessToken
            done = true
        }
        AppAuthTestSupport.waitFor { done }
        assertEquals("renewed", token)
        assertTrue(server.takeRequest().body.readUtf8().contains("grant_type=refresh_token"))
        assertEquals("renewed", state.lastTokenResponse?.accessToken)
    }

    @Test
    fun archivedStateReadsBack() {
        val expiresAt = Instant.ofEpochMilli(Instant.now().plus(Duration.ofHours(1)).toEpochMilli())
        val state = authorizedState("token", expiresAt)
        val copy = assertNotNull(OIDAuthState.unarchivedObject(from = state.archivedData()))
        assertTrue(copy.isAuthorized)
        assertEquals("token", copy.lastTokenResponse?.accessToken)
        assertEquals(expiresAt, copy.lastTokenResponse?.accessTokenExpirationDate)
        assertEquals("refresh", copy.refreshToken)
        assertNull(OIDAuthState.unarchivedObject(from = "not json".toByteArray()))
    }

    @Test
    fun theRequestKeepsItsRedirectForTheManifestScheme() {
        val redirect = com.moblin.android.streamingplatforms.youtube.youTubeRedirectUri
        val request: AuthorizationRequest = OIDAuthorizationRequest(
            configuration = configuration(),
            clientId = com.moblin.android.streamingplatforms.youtube.youTubeClientId,
            clientSecret = null,
            scopes = com.moblin.android.streamingplatforms.youtube.youTubeScopes,
            redirectURL = redirect,
            responseType = OIDResponseTypeCode,
            additionalParameters = null,
        ).request
        assertEquals(Uri.parse(redirect).scheme, request.redirectUri.scheme)
        val info = org.robolectric.RuntimeEnvironment.getApplication().packageManager.queryIntentActivities(
            android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(redirect + "?code=x"))
                .addCategory(android.content.Intent.CATEGORY_BROWSABLE),
            0,
        )
        assertTrue(info.any { it.activityInfo.name == "net.openid.appauth.RedirectUriReceiverActivity" }, "$info")
    }
}

package com.moblin.android.various.model

import android.os.Looper
import androidx.activity.ComponentActivity
import com.moblin.android.platform.appauth.AppAuthTestSupport
import com.moblin.android.platform.host.SystemEvents
import com.moblin.android.platform.host.SystemEventsState
import com.moblin.android.streamingplatforms.youtube.YouTubeApi
import com.moblin.android.streamingplatforms.youtube.youTubeClientId
import com.moblin.android.streamingplatforms.youtube.youTubeIssuer
import com.moblin.android.streamingplatforms.youtube.youTubeRedirectUri
import com.moblin.android.streamingplatforms.youtube.youTubeScopes
import com.moblin.android.various.settings.SettingsStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ModelYouTubeSignInSuite {
    private lateinit var server: MockWebServer
    private lateinit var model: Model
    private lateinit var stream: SettingsStream

    @Before
    fun setUp() {
        SystemEventsState.reset()
        SystemEvents.install(RuntimeEnvironment.getApplication())
        server = MockWebServer()
        server.start()
        AppAuthTestSupport.useServer(server)
        model = Model()
        stream = SettingsStream(name = "YouTube")
    }

    @After
    fun tearDown() {
        AppAuthTestSupport.restore()
        server.shutdown()
        SystemEventsState.reset()
    }

    private fun startSignIn(activity: ComponentActivity): android.content.Intent {
        server.enqueue(AppAuthTestSupport.discoveryResponse(server, youTubeIssuer))
        model.youTubeSignIn(stream)
        AppAuthTestSupport.waitFor { shadowOf(activity).peekNextStartedActivityForResult() != null }
        assertEquals("/.well-known/openid-configuration", server.takeRequest().path)
        return AppAuthTestSupport.startedAuthorization(activity)
    }

    @Test
    fun signInNeedsAWindowToPresentFrom() {
        model.youTubeSignIn(stream)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(0, server.requestCount)
        assertNull(model.youTube.session)
        assertNull(stream.youTubeAuthState)
    }

    @Test
    fun signInPresentsGoogleAndStoresTheAuthorizedState() {
        AppAuthTestSupport.installBrowser()
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        stream.youTubeNotLoggedInCount = 2
        val started = startSignIn(activity)
        val uri = AppAuthTestSupport.authorizationUri(started)
        assertEquals(youTubeClientId, uri.getQueryParameter("client_id"))
        assertEquals(youTubeRedirectUri, uri.getQueryParameter("redirect_uri"))
        assertEquals(youTubeScopes.joinToString(" "), uri.getQueryParameter("scope"))
        assertEquals("code", uri.getQueryParameter("response_type"))
        assertNotNull(model.youTube.session)
        server.enqueue(AppAuthTestSupport.tokenResponse("youtube-token"))
        AppAuthTestSupport.completeAuthorization(activity, started, code = "google-code")
        AppAuthTestSupport.waitFor { stream.youTubeAuthState != null }
        assertTrue(stream.isYouTubeAuthorized())
        assertTrue(stream.youTubeWantsToBeLoggedIn)
        assertEquals(0, stream.youTubeNotLoggedInCount)
        assertNull(model.youTube.session)
        assertTrue(server.takeRequest().body.readUtf8().contains("code=google-code"))
        var api: YouTubeApi? = null
        var done = false
        model.getYouTubeApi(stream) {
            api = it
            done = true
        }
        AppAuthTestSupport.waitFor { done }
        assertNotNull(api)
    }

    @Test
    fun cancelledSignInLeavesTheStreamSignedOut() {
        AppAuthTestSupport.installBrowser()
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        stream.youTubeWantsToBeLoggedIn = true
        val started = startSignIn(activity)
        AppAuthTestSupport.cancelAuthorization(activity, started)
        AppAuthTestSupport.waitFor { model.youTube.session == null }
        assertNull(model.youTube.session)
        assertNull(stream.youTubeAuthState)
        assertFalse(stream.youTubeWantsToBeLoggedIn)
        assertFalse(stream.isYouTubeAuthorized())
    }

    @Test
    fun withoutAnAuthStateThereIsNoApi() {
        var api: YouTubeApi? = null
        var done = false
        model.getYouTubeApi(stream) {
            api = it
            done = true
        }
        assertTrue(done)
        assertNull(api)
    }
}

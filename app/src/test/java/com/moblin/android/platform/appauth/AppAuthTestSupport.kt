package com.moblin.android.platform.appauth

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.os.Looper
import androidx.activity.ComponentActivity
import java.net.HttpURLConnection
import java.net.URL
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.connectivity.ConnectionBuilder
import net.openid.appauth.connectivity.DefaultConnectionBuilder
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

internal object AppAuthTestSupport {
    fun installBrowser() {
        val packageManager = shadowOf(RuntimeEnvironment.getApplication().packageManager)
        val component = ComponentName("com.example.browser", "com.example.browser.Browser")
        val application = ApplicationInfo().apply {
            packageName = component.packageName
        }
        packageManager.installPackage(
            PackageInfo().apply {
                packageName = component.packageName
                applicationInfo = application
                @Suppress("DEPRECATION")
                signatures = arrayOf(Signature(byteArrayOf(1, 2, 3, 4)))
            },
        )
        packageManager.addActivityIfNotPresent(component)
        packageManager.addIntentFilterForActivity(
            component,
            IntentFilter(Intent.ACTION_VIEW).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addCategory(Intent.CATEGORY_DEFAULT)
                addDataScheme("http")
                addDataScheme("https")
            },
        )
    }

    fun useServer(server: MockWebServer) {
        OIDAuthorizationService.connectionBuilder = ConnectionBuilder { uri ->
            val url = server.url(uri.encodedPath ?: "/").newBuilder().encodedQuery(uri.encodedQuery).build()
            URL(url.toString()).openConnection() as HttpURLConnection
        }
    }

    fun restore() {
        OIDAuthorizationService.connectionBuilder = DefaultConnectionBuilder.INSTANCE
    }

    fun discoveryResponse(server: MockWebServer, issuer: String): MockResponse {
        val body = """
            {
              "issuer": "$issuer",
              "authorization_endpoint": "${server.url("/o/oauth2/v2/auth")}",
              "token_endpoint": "${server.url("/token")}",
              "jwks_uri": "${server.url("/certs")}",
              "response_types_supported": ["code"],
              "subject_types_supported": ["public"],
              "id_token_signing_alg_values_supported": ["RS256"]
            }
        """.trimIndent()
        return MockResponse().setHeader("Content-Type", "application/json").setBody(body)
    }

    fun tokenResponse(accessToken: String, expiresIn: Long = 3600): MockResponse {
        val body = """
            {"access_token": "$accessToken", "token_type": "Bearer", "expires_in": $expiresIn,
             "refresh_token": "refresh-$accessToken"}
        """.trimIndent()
        return MockResponse().setHeader("Content-Type", "application/json").setBody(body)
    }

    fun waitFor(condition: () -> Boolean) {
        repeat(500) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) {
                return
            }
            Thread.sleep(10)
        }
        shadowOf(Looper.getMainLooper()).idle()
    }

    fun startedAuthorization(activity: ComponentActivity): Intent {
        val started = checkNotNull(shadowOf(activity).nextStartedActivityForResult) { "no activity started" }
        return started.intent
    }

    fun authorizationRequest(started: Intent): AuthorizationRequest {
        return AuthorizationRequest.jsonDeserialize(checkNotNull(started.getStringExtra("authRequest")))
    }

    fun authorizationUri(started: Intent): android.net.Uri {
        @Suppress("DEPRECATION")
        val authIntent = checkNotNull(started.getParcelableExtra<Intent>("authIntent"))
        return checkNotNull(authIntent.data)
    }

    fun completeAuthorization(activity: ComponentActivity, started: Intent, code: String) {
        val request = authorizationRequest(started)
        val response = AuthorizationResponse.Builder(request)
            .setAuthorizationCode(code)
            .setState(request.state)
            .build()
        shadowOf(activity).receiveResult(started, Activity.RESULT_OK, response.toIntent())
    }

    fun cancelAuthorization(activity: ComponentActivity, started: Intent) {
        shadowOf(activity).receiveResult(
            started,
            Activity.RESULT_CANCELED,
            AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW.toIntent(),
        )
    }
}

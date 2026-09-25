package com.moblin.android.platform.appauth

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.moblin.android.AppDelegate
import com.moblin.android.platform.uikit.UIViewController
import java.time.Instant
import java.util.UUID
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ClientAuthentication
import net.openid.appauth.ClientSecretBasic
import net.openid.appauth.NoClientAuthentication
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenResponse
import net.openid.appauth.connectivity.ConnectionBuilder
import net.openid.appauth.connectivity.DefaultConnectionBuilder

private const val TAG = "AppAuth"

const val OIDResponseTypeCode: String = ResponseTypeValues.CODE

typealias OIDAuthStateAuthorizationCallback = (OIDAuthState?, Throwable?) -> Unit

typealias OIDAuthStateAction = (String?, String?, Throwable?) -> Unit

private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

private fun runOnMain(block: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        block()
    } else {
        mainHandler.post(block)
    }
}

class OIDServiceConfiguration internal constructor(internal val configuration: AuthorizationServiceConfiguration) {
    val authorizationEndpoint: String
        get() = configuration.authorizationEndpoint.toString()

    val tokenEndpoint: String
        get() = configuration.tokenEndpoint.toString()
}

object OIDAuthorizationService {
    internal var connectionBuilder: ConnectionBuilder = DefaultConnectionBuilder.INSTANCE

    fun discoverConfiguration(forIssuer: String, completion: (OIDServiceConfiguration?, Throwable?) -> Unit) {
        val issuer = runCatching { Uri.parse(forIssuer) }.getOrNull()
        if (issuer == null) {
            runOnMain { completion(null, IllegalArgumentException("Invalid issuer $forIssuer")) }
            return
        }
        AuthorizationServiceConfiguration.fetchFromIssuer(
            issuer,
            { configuration, error ->
                runOnMain { completion(configuration?.let { OIDServiceConfiguration(it) }, error) }
            },
            connectionBuilder,
        )
    }

    fun discoverConfiguration(forIssuer: java.net.URI, completion: (OIDServiceConfiguration?, Throwable?) -> Unit) {
        discoverConfiguration(forIssuer.toString(), completion)
    }

    internal fun makeService(context: android.content.Context): AuthorizationService {
        val configuration = AppAuthConfiguration.Builder().setConnectionBuilder(connectionBuilder).build()
        return AuthorizationService(context, configuration)
    }
}

class OIDAuthorizationRequest(
    configuration: OIDServiceConfiguration,
    clientId: String,
    clientSecret: String?,
    scopes: List<String>?,
    redirectURL: String,
    responseType: String,
    additionalParameters: Map<String, String>?,
) {
    internal val request: AuthorizationRequest
    internal val clientSecret: String? = clientSecret

    init {
        val builder = AuthorizationRequest.Builder(
            configuration.configuration,
            clientId,
            responseType,
            Uri.parse(redirectURL),
        )
        if (scopes != null) {
            builder.setScopes(scopes)
        }
        if (additionalParameters != null) {
            builder.setAdditionalParameters(additionalParameters)
        }
        request = builder.build()
    }

    constructor(
        configuration: OIDServiceConfiguration,
        clientId: String,
        clientSecret: String?,
        scopes: List<String>?,
        redirectURL: java.net.URI,
        responseType: String,
        additionalParameters: Map<String, String>?,
    ) : this(configuration, clientId, clientSecret, scopes, redirectURL.toString(), responseType, additionalParameters)

    val clientID: String
        get() = request.clientId

    val redirectURL: String
        get() = request.redirectUri.toString()

    val scope: String?
        get() = request.scope

    internal fun clientAuthentication(): ClientAuthentication {
        val secret = clientSecret ?: return NoClientAuthentication.INSTANCE
        return ClientSecretBasic(secret)
    }
}

class OIDExternalUserAgentIOS private constructor(internal val activity: ComponentActivity) {
    companion object {
        operator fun invoke(presenting: UIViewController): OIDExternalUserAgentIOS? {
            val activity = presenting.activity as? ComponentActivity ?: return null
            if (activity.isFinishing || activity.isDestroyed) {
                return null
            }
            return OIDExternalUserAgentIOS(activity)
        }
    }
}

interface OIDExternalUserAgentSession {
    fun cancel()

    fun cancel(completion: () -> Unit)
}

class OIDTokenResponse internal constructor(internal val response: TokenResponse) {
    val accessToken: String?
        get() = response.accessToken

    val accessTokenExpirationDate: Instant?
        get() = response.accessTokenExpirationTime?.let { Instant.ofEpochMilli(it) }

    val idToken: String?
        get() = response.idToken

    val refreshToken: String?
        get() = response.refreshToken

    val scope: String?
        get() = response.scope
}

class OIDAuthState internal constructor(internal val state: AuthState) {
    val isAuthorized: Boolean
        get() = state.isAuthorized

    val lastTokenResponse: OIDTokenResponse?
        get() = state.lastTokenResponse?.let { OIDTokenResponse(it) }

    val refreshToken: String?
        get() = state.refreshToken

    val scope: String?
        get() = state.scope

    val authorizationError: Throwable?
        get() = state.authorizationException

    fun performAction(action: OIDAuthStateAction) {
        val service = try {
            OIDAuthorizationService.makeService(AppDelegate.context)
        } catch (error: Throwable) {
            runOnMain { action(null, null, error) }
            return
        }
        try {
            state.performActionWithFreshTokens(service) { accessToken, idToken, error ->
                service.dispose()
                runOnMain { action(accessToken, idToken, error) }
            }
        } catch (error: Throwable) {
            service.dispose()
            runOnMain { action(null, null, error) }
        }
    }

    fun archivedData(): ByteArray {
        return state.jsonSerializeString().toByteArray(Charsets.UTF_8)
    }

    companion object {
        fun authState(
            byPresenting: OIDAuthorizationRequest,
            externalUserAgent: OIDExternalUserAgentIOS,
            callback: OIDAuthStateAuthorizationCallback,
        ): OIDExternalUserAgentSession {
            val session = OIDAuthStateAuthorizationSession(byPresenting, externalUserAgent.activity, callback)
            session.start()
            return session
        }

        fun unarchivedObject(from: ByteArray): OIDAuthState? {
            return try {
                OIDAuthState(AuthState.jsonDeserialize(String(from, Charsets.UTF_8)))
            } catch (error: Throwable) {
                Log.i(TAG, "Failed to read auth state: $error")
                null
            }
        }
    }
}

private class OIDAuthStateAuthorizationSession(
    private val request: OIDAuthorizationRequest,
    private val activity: ComponentActivity,
    private val callback: OIDAuthStateAuthorizationCallback,
) : OIDExternalUserAgentSession {
    private val service = OIDAuthorizationService.makeService(activity)
    private var launcher: ActivityResultLauncher<Intent>? = null
    private var completed = false

    fun start() {
        try {
            val intent = service.getAuthorizationRequestIntent(request.request)
            val key = "OIDAuthState.${UUID.randomUUID()}"
            launcher = activity.activityResultRegistry.register(
                key,
                ActivityResultContracts.StartActivityForResult(),
            ) { result ->
                authorizationCompleted(result.data)
            }
            launcher?.launch(intent)
        } catch (error: Throwable) {
            Log.i(TAG, "Failed to start authorization: $error")
            complete(null, error)
        }
    }

    private fun authorizationCompleted(data: Intent?) {
        unregister()
        if (completed) {
            return
        }
        val response = data?.let { AuthorizationResponse.fromIntent(it) }
        if (response == null) {
            val error = data?.let { AuthorizationException.fromIntent(it) }
                ?: AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW
            complete(null, error)
            return
        }
        val authState = AuthState(response, null)
        try {
            service.performTokenRequest(
                response.createTokenExchangeRequest(),
                request.clientAuthentication(),
            ) { tokenResponse, error ->
                authState.update(tokenResponse, error)
                if (tokenResponse != null) {
                    complete(OIDAuthState(authState), null)
                } else {
                    complete(null, error ?: AuthorizationException.GeneralErrors.SERVER_ERROR)
                }
            }
        } catch (error: Throwable) {
            complete(null, error)
        }
    }

    override fun cancel() {
        cancel {}
    }

    override fun cancel(completion: () -> Unit) {
        unregister()
        complete(null, AuthorizationException.GeneralErrors.PROGRAM_CANCELED_AUTH_FLOW)
        runOnMain(completion)
    }

    private fun unregister() {
        launcher?.unregister()
        launcher = null
    }

    private fun complete(authState: OIDAuthState?, error: Throwable?) {
        if (completed) {
            return
        }
        completed = true
        service.dispose()
        runOnMain { callback(authState, error) }
    }
}

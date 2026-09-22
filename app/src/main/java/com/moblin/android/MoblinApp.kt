package com.moblin.android

import android.app.Activity
import android.app.Application
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.moblin.android.various.model.Model
import com.moblin.android.view.MainView
import com.moblin.android.view.externaldisplay.ExternalDisplayView
import com.moblin.android.view.stream.CameraPreviewView
import com.moblin.android.view.stream.StreamPreviewView
import com.moblin.android.view.stream.StreamView
import java.lang.ref.WeakReference
import java.net.URI
import android.content.Context

object MoblinApp {
    var globalModel: Model? = null
}

@Composable
fun MoblinApp() {
    val model = remember {
        Model().also { MoblinApp.globalModel = it }
    }
    val show = model.show
    val toast = model.toast
    val orientation = model.orientation
    val database = model.database
    CompositionLocalProvider(LocalModel provides model) {
    Box(modifier = Modifier.background(Color.Black)) {
        MainView(
            webBrowserController = model.webBrowserController,
            streamView = {
                StreamView(
                    show = show,
                    cameraPreviewView = CameraPreviewView(model = model),
                    streamPreviewView = StreamPreviewView(model = model)
                )
            },
            createStreamWizard = model.createStreamWizard,
            toast = toast,
            orientation = orientation,
            quickButtons = database.quickButtonsGeneral,
            model = model
        )
    }
    }
}

@Composable
fun ExternalScreenContentView() {
    val model = remember {
        checkNotNull(MoblinApp.globalModel) { "MoblinApp.globalModel is not initialised" }
    }
    ExternalDisplayView(
        externalDisplay = model.externalDisplay,
        model = model
    )
}

class SceneDelegate {
    fun scene(
        sessionRole: String?,
        urlContexts: List<URI>
    ) {
        val model = MoblinApp.globalModel ?: return
        Unit
        if (sessionRole == SESSION_ROLE_WINDOW_EXTERNAL_DISPLAY_NON_INTERACTIVE) {
            Unit
        }
    }

    fun sceneDidDisconnect() {
        val model = MoblinApp.globalModel ?: return
        model.externalMonitorDisconnected()
    }

    fun scene(urlContexts: List<URI>) {
        Unit
    }

    companion object {
        const val SESSION_ROLE_WINDOW_EXTERNAL_DISPLAY_NON_INTERACTIVE =
            "UIWindowSceneSessionRoleExternalDisplayNonInteractive"
    }
}

class AppDelegate : Application() {

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        registerActivityLifecycleCallbacks(lifecycleCallbacks)
    }

    fun configurationForConnecting(sessionRole: String?): SceneDelegate =
        TODO()
    fun willFinishLaunchingWithOptions(): Boolean = true

    fun didFinishLaunchingWithOptions(): Boolean = true

    fun supportedInterfaceOrientationsFor(): Int = orientationLock

    companion object {
        lateinit var context: Context
        private var currentActivity: WeakReference<Activity>? = null

        private val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                currentActivity = WeakReference(activity)
            }

            override fun onActivityStarted(activity: Activity) = Unit

            override fun onActivityResumed(activity: Activity) {
                currentActivity = WeakReference(activity)
            }

            override fun onActivityPaused(activity: Activity) = Unit

            override fun onActivityStopped(activity: Activity) = Unit

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

            override fun onActivityDestroyed(activity: Activity) {
                if (currentActivity?.get() === activity) {
                    currentActivity = null
                }
            }
        }

        var orientationLock: Int = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            set(value) {
                field = value
                currentActivity?.get()?.requestedOrientation = value
            }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MoblinApp()
        }
    }
}

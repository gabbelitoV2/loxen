package com.moblin.android.various.model

import android.Manifest
import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.moblin.android.platform.networkextension.FakeWifi
import com.moblin.android.various.utils.fetchCurrentWiFiSsid
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ModelWiFiSsidSuite {
    private lateinit var application: Application
    private lateinit var wifi: FakeWifi
    private lateinit var model: Model

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        shadowOf(application).denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        wifi = FakeWifi(application)
        model = Model()
        runMain()
    }

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun updateCurrentSsid() {
        val method = Model::class.java.getDeclaredMethod("updateCurrentSsid")
        method.isAccessible = true
        method.invoke(model)
    }

    private fun remoteControlSsid(): String? = model.remoteControlStreamerCreateStatus(filter = null).first?.wiFiSsid

    @Test
    fun fetchCurrentWiFiSsidCompletesOnTheMainThreadAfterReturning() {
        shadowOf(application).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        wifi.connect()
        val ssids = mutableListOf<String?>()
        var thread: Thread? = null
        fetchCurrentWiFiSsid {
            thread = Thread.currentThread()
            ssids.add(it)
        }
        wifi.answer("Home")
        assertEquals(emptyList(), ssids)
        runMain()
        assertEquals(listOf<String?>("Home"), ssids)
        assertEquals(Looper.getMainLooper().thread, thread)
    }

    @Test
    fun theStreamerReportsItsWiFiNetworkToTheRemoteControlAssistant() {
        shadowOf(application).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        wifi.connect()
        updateCurrentSsid()
        assertEquals(1, wifi.answer("Home"))
        runMain()
        assertEquals("Home", model.currentWiFiSsid)
        assertEquals("Home", remoteControlSsid())
    }

    @Test
    fun withoutLocationPermissionTheSsidIsClearedLikeIos() {
        model.currentWiFiSsid = "Home"
        wifi.connect()
        updateCurrentSsid()
        assertEquals(0, wifi.pendingRequests().size)
        runMain()
        assertNull(model.currentWiFiSsid)
        assertNull(remoteControlSsid())
    }

    @Test
    fun leavingWiFiClearsTheSsid() {
        shadowOf(application).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        model.currentWiFiSsid = "Home"
        updateCurrentSsid()
        runMain()
        assertNull(model.currentWiFiSsid)
        assertNull(remoteControlSsid())
    }
}

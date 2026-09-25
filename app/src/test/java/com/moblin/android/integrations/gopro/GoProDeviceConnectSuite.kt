package com.moblin.android.integrations.gopro

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.moblin.android.platform.corebluetooth.BluetoothAuthorization
import com.moblin.android.platform.corebluetooth.identifier
import com.moblin.android.various.settings.SettingsGoProLaunchLiveStreamResolution
import com.moblin.android.various.settings.SettingsGoProLens
import kotlin.test.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBluetoothDevice
import org.robolectric.shadows.ShadowBluetoothLeScanner

@RunWith(RobolectricTestRunner::class)
class GoProDeviceConnectSuite {
    private lateinit var application: Application
    private var device: GoProDevice? = null

    @Before
    fun setUp() {
        BluetoothAuthorization.reset()
        application = ApplicationProvider.getApplicationContext()
        shadowOf(application).grantPermissions(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        shadowOf(application.packageManager).setSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE, true)
        shadowOf(adapter()).setState(BluetoothAdapter.STATE_ON)
    }

    @After
    fun tearDown() {
        device?.stopLiveStream()
        runMain()
        BluetoothAuthorization.reset()
    }

    private fun adapter(): BluetoothAdapter = application.getSystemService(BluetoothManager::class.java).adapter

    private fun scanCallbacks(): Set<ScanCallback> {
        val scanner = adapter().bluetoothLeScanner ?: return emptySet()
        return Shadow.extract<ShadowBluetoothLeScanner>(scanner).scanCallbacks
    }

    private fun connections(peripheral: BluetoothDevice): Int {
        return Shadow.extract<ShadowBluetoothDevice>(peripheral).bluetoothGatts.size
    }

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun onlyTheSelectedCameraIsConnected() {
        val selected = adapter().getRemoteDevice("AA:BB:CC:DD:EE:01")
        val other = adapter().getRemoteDevice("AA:BB:CC:DD:EE:02")
        val device = GoProDevice(application)
        this.device = device
        device.startLiveStream(
            wifiSsid = "ssid",
            wifiPassword = "password",
            rtmpUrl = "rtmp://192.168.0.2:1935/live/key",
            resolution = SettingsGoProLaunchLiveStreamResolution.r1080p,
            bitrate = 6_000_000u,
            lens = SettingsGoProLens.auto,
            deviceId = selected.identifier,
        )
        runMain()
        val callback = scanCallbacks().single()
        callback.onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, ScanResult(other, null, -40, 0))
        runMain()
        assertEquals(0, connections(other))
        assertEquals(setOf(callback), scanCallbacks())
        callback.onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, ScanResult(selected, null, -60, 0))
        runMain()
        assertEquals(1, connections(selected))
        assertEquals(0, connections(other))
        assertEquals(emptySet(), scanCallbacks())
        assertEquals(GoProDeviceState.connecting, device.getState())
    }
}

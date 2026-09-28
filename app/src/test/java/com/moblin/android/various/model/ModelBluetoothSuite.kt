package com.moblin.android.various.model

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import com.moblin.android.platform.corebluetooth.BluetoothAuthorization
import com.moblin.android.platform.corebluetooth.CBCentralManager
import com.moblin.android.platform.corebluetooth.CBManagerState
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
class ModelBluetoothSuite {
    private lateinit var application: Application
    private lateinit var model: Model
    private var controller: ActivityController<ComponentActivity>? = null
    private val permissions = arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)

    @Before
    fun setUp() {
        BluetoothAuthorization.reset()
        application = ApplicationProvider.getApplicationContext()
        shadowOf(application).denyPermissions(*permissions)
        shadowOf(application.packageManager).setSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE, true)
        shadowOf(adapter()).setState(BluetoothAdapter.STATE_ON)
        model = Model()
        runMain()
    }

    @After
    fun tearDown() {
        model.bluetoothCentralManger?.delegate = null
        model.bluetoothCentralManger = null
        controller?.pause()?.stop()?.destroy()
        BluetoothAuthorization.reset()
    }

    private fun adapter(): BluetoothAdapter = application.getSystemService(BluetoothManager::class.java).adapter

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun startActivity(): ComponentActivity {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        this.controller = controller
        BluetoothAuthorization.install(controller.get())
        return controller.get()
    }

    private fun createCentralManagerLikeSetup(): CBCentralManager {
        val central = CBCentralManager(delegate = { model.centralManagerDidUpdateState(it) }, queue = null)
        model.bluetoothCentralManger = central
        return central
    }

    private fun answerPermissionRequest(activity: ComponentActivity, granted: Boolean) {
        val request = shadowOf(activity).lastRequestedPermission
        assertEquals(permissions.toList(), request.requestedPermissions.toList())
        val result = if (granted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
        if (granted) {
            shadowOf(application).grantPermissions(*permissions)
        }
        @Suppress("DEPRECATION")
        activity.onRequestPermissionsResult(
            request.requestCode,
            request.requestedPermissions,
            IntArray(request.requestedPermissions.size) { result },
        )
        runMain()
    }

    private fun setAdapterState(state: Int) {
        shadowOf(adapter()).setState(state)
        application.sendBroadcast(
            Intent(BluetoothAdapter.ACTION_STATE_CHANGED).putExtra(BluetoothAdapter.EXTRA_STATE, state),
        )
        runMain()
    }

    @Test
    fun bluetoothIsNotAllowedUntilThePermissionPromptIsAccepted() {
        val activity = startActivity()
        val central = createCentralManagerLikeSetup()
        runMain()
        assertEquals(CBManagerState.unauthorized, central.state)
        assertFalse(model.bluetoothAllowed.value)
        answerPermissionRequest(activity, granted = true)
        assertEquals(CBManagerState.poweredOn, central.state)
        assertTrue(model.bluetoothAllowed.value)
    }

    @Test
    fun aDeniedPermissionPromptKeepsBluetoothNotAllowed() {
        val activity = startActivity()
        val central = createCentralManagerLikeSetup()
        runMain()
        answerPermissionRequest(activity, granted = false)
        assertEquals(CBManagerState.unauthorized, central.state)
        assertFalse(model.bluetoothAllowed.value)
    }

    @Test
    fun bluetoothIsAllowedWhileTurnedOffBecauseOnlyTheAuthorizationCounts() {
        shadowOf(application).grantPermissions(*permissions)
        shadowOf(adapter()).setState(BluetoothAdapter.STATE_OFF)
        val central = createCentralManagerLikeSetup()
        runMain()
        assertEquals(CBManagerState.poweredOff, central.state)
        assertTrue(model.bluetoothAllowed.value)
        setAdapterState(BluetoothAdapter.STATE_ON)
        assertEquals(CBManagerState.poweredOn, central.state)
        assertTrue(model.bluetoothAllowed.value)
    }

    @Test
    fun theNotAllowedMessageNamesLoxen() {
        assertEquals("⚠️ Loxen is not allowed to use Bluetooth", bluetoothNotAllowedMessage)
    }
}

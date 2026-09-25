package com.moblin.android.integrations.tesla

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.moblin.android.platform.corebluetooth.BluetoothAuthorization
import com.moblin.android.platform.corebluetooth.CBUUID
import com.moblin.android.platform.corebluetooth.RecordingBluetoothGatt
import com.moblin.android.platform.corebluetooth.bluetoothIdentifier
import com.moblin.android.platform.corebluetooth.gattSdkInt
import java.time.Duration
import java.util.UUID
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBluetoothDevice

internal const val teslaVin = "5YJ3E1EA7KF317000"
internal const val teslaAddress = "D4:12:FA:00:13:37"

internal class TeslaVehicleEvents : TeslaVehicleDelegate {
    val events = mutableListOf<String>()

    override fun teslaVehicleState(vehicle: TeslaVehicle, state: TeslaVehicleState) {
        events.add("state $state")
    }

    override fun teslaVehicleVehicleSecurityConnected(vehicle: TeslaVehicle) {
        events.add("vehicleSecurity")
    }

    override fun teslaVehicleInfotainmentConnected(vehicle: TeslaVehicle) {
        events.add("infotainment")
    }
}

internal class TeslaTestBench {
    lateinit var application: Application
    val fake = FakeTeslaVehicle(teslaVin)
    val peripheralId: UUID = bluetoothIdentifier(teslaAddress)

    fun setUp() {
        BluetoothAuthorization.reset()
        gattSdkInt = Build.VERSION.SDK_INT
        application = ApplicationProvider.getApplicationContext()
        shadowOf(application).grantPermissions(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        shadowOf(application.packageManager).setSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE, true)
        shadowOf(adapter()).setState(BluetoothAdapter.STATE_ON)
        Shadow.extract<ShadowBluetoothDevice>(device()).setType(BluetoothDevice.DEVICE_TYPE_LE)
    }

    fun tearDown() {
        runMain()
        gattSdkInt = Build.VERSION.SDK_INT
        BluetoothAuthorization.reset()
    }

    fun adapter(): BluetoothAdapter = application.getSystemService(BluetoothManager::class.java).adapter

    fun device(): BluetoothDevice = adapter().getRemoteDevice(teslaAddress)

    fun gatts(): List<BluetoothGatt> = Shadow.extract<ShadowBluetoothDevice>(device()).bluetoothGatts.toList()

    fun gatt(): BluetoothGatt = gatts().last()

    fun recording(gatt: BluetoothGatt = gatt()): RecordingBluetoothGatt = Shadow.extract(gatt)

    fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    fun advance(seconds: Long) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(seconds))
    }

    fun pump() {
        fake.pump { runMain() }
    }

    fun connectFake() {
        fake.connect(gatt()) { runMain() }
        pump()
    }

    fun setAdapterState(state: Int) {
        shadowOf(adapter()).setState(state)
        application.sendBroadcast(
            Intent(BluetoothAdapter.ACTION_STATE_CHANGED).putExtra(BluetoothAdapter.EXTRA_STATE, state),
        )
        runMain()
    }

    fun uuidString(id: UUID): String = CBUUID(id).uuidString

    fun connectionCalls(): List<String> {
        val fromVehicle = uuidString(teslaFromVehicleId)
        return listOf(
            "requestMtu 517",
            "discoverServices",
            "setCharacteristicNotification $fromVehicle true",
            "writeDescriptor $fromVehicle 0200",
        )
    }
}

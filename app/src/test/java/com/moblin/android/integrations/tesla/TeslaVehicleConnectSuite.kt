package com.moblin.android.integrations.tesla

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.moblin.android.platform.corebluetooth.BluetoothAuthorization
import com.moblin.android.platform.corebluetooth.identifier
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBluetoothDevice

@RunWith(RobolectricTestRunner::class)
class TeslaVehicleConnectSuite {
    private lateinit var application: Application
    private var vehicle: TeslaVehicle? = null

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
        vehicle?.stop()
        BluetoothAuthorization.reset()
    }

    private fun adapter(): BluetoothAdapter = application.getSystemService(BluetoothManager::class.java).adapter

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun bytes32(value: BigInteger): ByteArray {
        val bytes = value.toByteArray().takeLast(32).toByteArray()
        return ByteArray(32 - bytes.size) + bytes
    }

    private fun privateKeyPem(): String {
        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ECGenParameterSpec("secp256r1"))
        val keyPair = generator.generateKeyPair()
        val point = (keyPair.public as ECPublicKey).w
        val der = hex("308187020100301306072a8648ce3d020106082a8648ce3d030107046d306b0201010420") +
            bytes32((keyPair.private as ECPrivateKey).s) +
            hex("a14403420004") +
            bytes32(point.affineX) +
            bytes32(point.affineY)
        return "-----BEGIN PRIVATE KEY-----\n${Base64.getEncoder().encodeToString(der)}\n-----END PRIVATE KEY-----\n"
    }

    private fun hex(text: String): ByteArray = text.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    @Test
    fun theSelectedVehicleIsConnected() {
        val peripheral = adapter().getRemoteDevice("AA:BB:CC:DD:EE:FF")
        val vehicle = assertNotNull(
            TeslaVehicle(vin = "5YJ3E1EA7KF000000", privateKeyPem = privateKeyPem(), peripheralId = peripheral.identifier),
        )
        this.vehicle = vehicle
        val states = mutableListOf<TeslaVehicleState>()
        vehicle.delegate = object : TeslaVehicleDelegate {
            override fun teslaVehicleState(vehicle: TeslaVehicle, state: TeslaVehicleState) {
                states.add(state)
            }

            override fun teslaVehicleVehicleSecurityConnected(vehicle: TeslaVehicle) {}

            override fun teslaVehicleInfotainmentConnected(vehicle: TeslaVehicle) {}
        }
        vehicle.start()
        runMain()
        assertEquals(1, Shadow.extract<ShadowBluetoothDevice>(peripheral).bluetoothGatts.size)
        assertEquals(listOf(TeslaVehicleState.connecting), states)
    }
}

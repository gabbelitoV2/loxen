package com.moblin.android.platform.corebluetooth

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.util.Properties
import java.util.UUID
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.coroutines.CoroutineContext
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBluetoothDevice
import org.robolectric.shadows.ShadowBluetoothLeScanner
import org.w3c.dom.Element

private const val deviceAddress = "AA:BB:CC:DD:EE:FF"
private const val androidNamespace = "http://schemas.android.com/apk/res/android"

@Implements(BluetoothLeScanner::class)
class RefusingBluetoothLeScanner : ShadowBluetoothLeScanner() {
    @Implementation
    override fun startScan(filters: MutableList<ScanFilter>?, settings: ScanSettings?, callback: ScanCallback?) {
        throw SecurityException("Need android.permission.BLUETOOTH_SCAN permission for AttributionSource")
    }
}

@Implements(BluetoothDevice::class)
class RefusingBluetoothDevice : ShadowBluetoothDevice() {
    @Implementation
    override fun connectGatt(
        context: Context?,
        autoConnect: Boolean,
        callback: BluetoothGattCallback?,
        transport: Int,
    ): BluetoothGatt? {
        throw SecurityException("Need android.permission.BLUETOOTH_CONNECT permission for AttributionSource")
    }
}

private class RecordingScanCallback : ScanCallback()

@RunWith(RobolectricTestRunner::class)
class CBCentralManagerSuite {
    private lateinit var application: Application
    private var controller: ActivityController<ComponentActivity>? = null
    private val reported = mutableListOf<CBManagerState>()
    private val permissions = arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)

    @Before
    fun setUp() {
        BluetoothAuthorization.reset()
        application = ApplicationProvider.getApplicationContext()
        shadowOf(application).denyPermissions(*permissions)
        shadowOf(application.packageManager).setSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE, true)
        shadowOf(adapter()).setState(BluetoothAdapter.STATE_ON)
    }

    @After
    fun tearDown() {
        controller?.pause()?.stop()?.destroy()
        BluetoothAuthorization.reset()
    }

    private fun adapter(): BluetoothAdapter = application.getSystemService(BluetoothManager::class.java).adapter

    private fun scanCallbacks(): Set<ScanCallback> {
        val scanner = adapter().bluetoothLeScanner ?: return emptySet()
        return Shadow.extract<ShadowBluetoothLeScanner>(scanner).scanCallbacks
    }

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun startActivity(): ComponentActivity {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        this.controller = controller
        BluetoothAuthorization.install(controller.get())
        return controller.get()
    }

    private fun scanningManager(callback: ScanCallback): CBCentralManager {
        return CBCentralManager(delegate = { central ->
            reported.add(central.state)
            if (central.state == CBManagerState.poweredOn) {
                central.scanForPeripherals(withServices = null, callback = callback)
            }
        })
    }

    private fun answerPermissionRequest(activity: ComponentActivity, granted: Boolean) {
        val request = shadowOf(activity).lastRequestedPermission
        val result = if (granted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
        if (granted) {
            shadowOf(application).grantPermissions(*request.requestedPermissions)
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
    fun withoutThePermissionTheManagerIsUnauthorizedAsksLikeIosAndDoesNotScan() {
        val activity = startActivity()
        val callback = RecordingScanCallback()
        val manager = scanningManager(callback)
        assertEquals(CBManagerState.unknown, manager.state)
        assertEquals(emptyList(), reported)
        runMain()
        assertEquals(listOf(CBManagerState.unauthorized), reported)
        assertEquals(CBManagerAuthorization.notDetermined, CBCentralManager.authorization)
        assertEquals(permissions.toList(), shadowOf(activity).lastRequestedPermission.requestedPermissions.toList())
        manager.scanForPeripherals(withServices = null, callback = callback)
        assertFalse(manager.isScanning)
        assertEquals(emptySet(), scanCallbacks())
    }

    @Test
    fun grantingThePermissionReportsPoweredOnAndTheScanStarts() {
        val activity = startActivity()
        val callback = RecordingScanCallback()
        val manager = scanningManager(callback)
        runMain()
        answerPermissionRequest(activity, granted = true)
        assertEquals(listOf(CBManagerState.unauthorized, CBManagerState.poweredOn), reported)
        assertEquals(CBManagerAuthorization.allowedAlways, CBCentralManager.authorization)
        assertTrue(manager.isScanning)
        assertEquals(setOf<ScanCallback>(callback), scanCallbacks())
        manager.stopScan()
        assertFalse(manager.isScanning)
        assertEquals(emptySet(), scanCallbacks())
    }

    @Test
    fun aDeniedPermissionStaysUnauthorizedAndIsNotAskedAgain() {
        val activity = startActivity()
        scanningManager(RecordingScanCallback())
        runMain()
        answerPermissionRequest(activity, granted = false)
        val firstRequest = shadowOf(activity).lastRequestedPermission
        assertEquals(CBManagerAuthorization.denied, CBCentralManager.authorization)
        val manager = scanningManager(RecordingScanCallback())
        runMain()
        assertEquals(CBManagerState.unauthorized, manager.state)
        assertEquals(firstRequest, shadowOf(activity).lastRequestedPermission)
        assertEquals(listOf(CBManagerState.unauthorized, CBManagerState.unauthorized), reported)
    }

    @Test
    fun aRequestCancelledByAnotherPermissionRequestIsAskedAgainWhenTheAppResumes() {
        val activity = startActivity()
        scanningManager(RecordingScanCallback())
        runMain()
        val request = shadowOf(activity).lastRequestedPermission
        @Suppress("DEPRECATION")
        activity.onRequestPermissionsResult(request.requestCode, emptyArray(), IntArray(0))
        runMain()
        assertEquals(CBManagerAuthorization.notDetermined, CBCentralManager.authorization)
        assertSame(request, shadowOf(activity).lastRequestedPermission)
        controller!!.pause()
        controller!!.resume()
        runMain()
        assertNotSame(request, shadowOf(activity).lastRequestedPermission)
        assertEquals(permissions.toList(), shadowOf(activity).lastRequestedPermission.requestedPermissions.toList())
    }

    @Test
    fun aManagerCreatedBeforeTheActivityAsksOnceTheActivityIsResumed() {
        val callback = RecordingScanCallback()
        scanningManager(callback)
        runMain()
        assertEquals(listOf(CBManagerState.unauthorized), reported)
        val activity = startActivity()
        runMain()
        answerPermissionRequest(activity, granted = true)
        assertEquals(listOf(CBManagerState.unauthorized, CBManagerState.poweredOn), reported)
        assertEquals(setOf<ScanCallback>(callback), scanCallbacks())
    }

    @Test
    fun aPermissionGrantedInSettingsIsReportedWhenTheAppResumes() {
        val activity = startActivity()
        scanningManager(RecordingScanCallback())
        runMain()
        answerPermissionRequest(activity, granted = false)
        controller!!.pause()
        shadowOf(application).grantPermissions(*permissions)
        controller!!.resume()
        runMain()
        assertEquals(listOf(CBManagerState.unauthorized, CBManagerState.poweredOn), reported)
    }

    @Test
    fun bluetoothTurnedOffIsPoweredOffAndTurningItOnStartsTheScan() {
        shadowOf(application).grantPermissions(*permissions)
        shadowOf(adapter()).setState(BluetoothAdapter.STATE_OFF)
        val activity = startActivity()
        val callback = RecordingScanCallback()
        val manager = scanningManager(callback)
        runMain()
        assertEquals(listOf(CBManagerState.poweredOff), reported)
        assertNull(shadowOf(activity).lastRequestedPermission)
        manager.scanForPeripherals(withServices = null, callback = callback)
        assertFalse(manager.isScanning)
        setAdapterState(BluetoothAdapter.STATE_ON)
        assertEquals(listOf(CBManagerState.poweredOff, CBManagerState.poweredOn), reported)
        assertTrue(manager.isScanning)
        assertEquals(setOf<ScanCallback>(callback), scanCallbacks())
        setAdapterState(BluetoothAdapter.STATE_OFF)
        assertEquals(listOf(CBManagerState.poweredOff, CBManagerState.poweredOn, CBManagerState.poweredOff), reported)
        assertFalse(manager.isScanning)
    }

    @Test
    fun aManagerWithoutDelegateGetsNoMoreCallbacks() {
        shadowOf(application).grantPermissions(*permissions)
        startActivity()
        val manager = scanningManager(RecordingScanCallback())
        runMain()
        manager.stopScan()
        manager.delegate = null
        setAdapterState(BluetoothAdapter.STATE_OFF)
        assertEquals(listOf(CBManagerState.poweredOn), reported)
        assertEquals(CBManagerState.poweredOff, manager.state)
    }

    @Test
    fun theDelegateRunsOnTheQueueOfTheManager() {
        shadowOf(application).grantPermissions(*permissions)
        var dispatched = 0
        val dispatcher = object : CoroutineDispatcher() {
            override fun dispatch(context: CoroutineContext, block: Runnable) {
                dispatched += 1
                block.run()
            }
        }
        CBCentralManager(delegate = { reported.add(it.state) }, queue = CoroutineScope(dispatcher))
        runMain()
        assertEquals(1, dispatched)
        assertEquals(listOf(CBManagerState.poweredOn), reported)
    }

    @Test
    @Config(shadows = [RefusingBluetoothLeScanner::class])
    fun aScanRefusedByTheSystemIsCaught() {
        shadowOf(application).grantPermissions(*permissions)
        val callback = RecordingScanCallback()
        val manager = scanningManager(callback)
        runMain()
        assertEquals(listOf(CBManagerState.poweredOn), reported)
        assertFalse(manager.isScanning)
        manager.scanForPeripherals(withServices = listOf(UUID.randomUUID()), callback = callback)
        assertFalse(manager.isScanning)
        manager.stopScan()
    }

    @Test
    @Config(shadows = [RefusingBluetoothDevice::class])
    fun aConnectionRefusedByTheSystemIsCaught() {
        shadowOf(application).grantPermissions(*permissions)
        val manager = CBCentralManager(delegate = null)
        runMain()
        val peripheral = manager.retrievePeripherals(withIdentifiers = listOf(deviceAddress)).single()
        assertNull(manager.connect(peripheral, callback = object : BluetoothGattCallback() {}))
    }

    @Test
    fun connectingWithoutThePermissionNeverReachesTheSystem() {
        val manager = CBCentralManager(delegate = null)
        runMain()
        val peripheral = adapter().getRemoteDevice(deviceAddress)
        Shadow.extract<ShadowBluetoothDevice>(peripheral).setShouldThrowSecurityExceptions(true)
        assertNull(manager.connect(peripheral, callback = object : BluetoothGattCallback() {}))
        assertEquals(emptyList(), Shadow.extract<ShadowBluetoothDevice>(peripheral).bluetoothGatts)
    }

    @Test
    fun connectingWithThePermissionUsesLowEnergyAndCancellingClosesTheConnection() {
        shadowOf(application).grantPermissions(*permissions)
        val manager = CBCentralManager(delegate = null)
        runMain()
        val peripheral = manager.retrievePeripherals(withIdentifiers = listOf(deviceAddress.lowercase())).single()
        val gatt = manager.connect(peripheral, callback = object : BluetoothGattCallback() {})
        assertNotNull(gatt)
        manager.cancelPeripheralConnection(gatt)
        assertTrue(shadowOf(gatt).isClosed)
    }

    @Test
    fun gattAndDeviceCallsRefusedByTheSystemReturnTheFallback() {
        val peripheral = adapter().getRemoteDevice(deviceAddress)
        Shadow.extract<ShadowBluetoothDevice>(peripheral).setName("Tesla")
        Shadow.extract<ShadowBluetoothDevice>(peripheral).setShouldThrowSecurityExceptions(true)
        assertNull(bluetoothCall(null) { peripheral.name })
        assertEquals(false, bluetoothCall(false) { throw SecurityException("Need android.permission.BLUETOOTH_CONNECT") })
        bluetoothCall { throw SecurityException("Need android.permission.BLUETOOTH_CONNECT") }
        shadowOf(application).grantPermissions(Manifest.permission.BLUETOOTH_CONNECT)
        assertEquals("Tesla", bluetoothCall(null) { peripheral.name })
    }

    @Test
    fun retrievingPeripheralsIgnoresUnknownIdentifiers() {
        val manager = CBCentralManager(delegate = null)
        val peripherals = manager.retrievePeripherals(
            withIdentifiers = listOf(UUID.randomUUID().toString(), deviceAddress, "not an address"),
        )
        assertEquals(listOf(deviceAddress), peripherals.map { it.address })
    }

    @Test
    fun anIdentifierIsAUuidThatRetrievesTheSamePeripheral() {
        val manager = CBCentralManager(delegate = null)
        val identifier = adapter().getRemoteDevice(deviceAddress).identifier
        assertEquals(UUID.fromString("00000000-0000-0000-0000-AABBCCDDEEFF"), identifier)
        for (text in listOf(identifier.toString(), identifier.toString().uppercase())) {
            assertEquals(listOf(deviceAddress), manager.retrievePeripherals(withIdentifiers = listOf(text)).map { it.address })
        }
        val leadingZeros = adapter().getRemoteDevice("0A:00:00:00:00:01")
        assertEquals(
            listOf("0A:00:00:00:00:01"),
            manager.retrievePeripherals(withIdentifiers = listOf(leadingZeros.identifier.toString())).map { it.address },
        )
        assertNotEquals(identifier, leadingZeros.identifier)
    }

    @Test
    fun anAnswerDeliveredToTheRecreatedActivityIsNotAskedAgain() {
        val activity = startActivity()
        scanningManager(RecordingScanCallback())
        runMain()
        val request = shadowOf(activity).lastRequestedPermission
        val recreated = controller!!.recreate().get()
        assertNotSame(activity, recreated)
        @Suppress("DEPRECATION")
        recreated.onRequestPermissionsResult(
            request.requestCode,
            request.requestedPermissions,
            IntArray(request.requestedPermissions.size) { PackageManager.PERMISSION_DENIED },
        )
        BluetoothAuthorization.install(recreated)
        runMain()
        assertNull(shadowOf(recreated).lastRequestedPermission)
        assertEquals(CBManagerAuthorization.denied, CBCentralManager.authorization)
    }

    @Test
    fun withoutBluetoothLowEnergyTheManagerIsUnsupported() {
        shadowOf(application).grantPermissions(*permissions)
        shadowOf(application.packageManager).setSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE, false)
        CBCentralManager(delegate = { reported.add(it.state) })
        runMain()
        assertEquals(listOf(CBManagerState.unsupported), reported)
    }

    @Test
    fun androidElevenAndOlderScanWithTheLocationPermission() {
        assertEquals(listOf(Manifest.permission.ACCESS_FINE_LOCATION), bluetoothPermissions(30).toList())
        assertEquals(permissions.toList(), bluetoothPermissions(31).toList())
        assertEquals(permissions.toList(), bluetoothPermissions(35).toList())
    }

    @Test
    fun theManifestDeclaresTheBluetoothPermissionsWithoutLocationFromScans() {
        val properties = Properties()
        javaClass.classLoader!!.getResourceAsStream("com/android/tools/test_config.properties")!!.use {
            properties.load(it)
        }
        val manifest = File(properties.getProperty("android_merged_manifest").replace('\\', '/'))
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val elements = factory.newDocumentBuilder().parse(manifest).getElementsByTagName("uses-permission")
        val declared = (0 until elements.length).associate { index ->
            val element = elements.item(index) as Element
            element.getAttributeNS(androidNamespace, "name") to element
        }
        fun attribute(permission: String, name: String): String? {
            val element = assertNotNull(declared[permission], permission)
            return element.getAttributeNS(androidNamespace, name).ifEmpty { null }
        }
        assertEquals("30", attribute(Manifest.permission.BLUETOOTH, "maxSdkVersion"))
        assertEquals("30", attribute(Manifest.permission.BLUETOOTH_ADMIN, "maxSdkVersion"))
        assertNull(attribute(Manifest.permission.BLUETOOTH_CONNECT, "maxSdkVersion"))
        assertNull(attribute(Manifest.permission.BLUETOOTH_SCAN, "maxSdkVersion"))
        assertEquals("neverForLocation", attribute(Manifest.permission.BLUETOOTH_SCAN, "usesPermissionFlags"))
        assertNull(attribute(Manifest.permission.ACCESS_FINE_LOCATION, "maxSdkVersion"))
        @Suppress("DEPRECATION")
        val info = application.packageManager.getPackageInfo(application.packageName, PackageManager.GET_PERMISSIONS)
        val requested = info.requestedPermissions!!.toList()
        assertTrue(Manifest.permission.BLUETOOTH_SCAN in requested)
        assertTrue(Manifest.permission.BLUETOOTH_CONNECT in requested)
        assertFalse(Manifest.permission.BLUETOOTH in requested)
        assertFalse(Manifest.permission.BLUETOOTH_ADMIN in requested)
    }
}

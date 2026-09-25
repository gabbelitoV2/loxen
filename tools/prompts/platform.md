# Tier: platform

This file uses platform services that have Android counterparts. Use the counterpart, keep the same public API shape, and leave permission requests to the Activity layer.

- CoreBluetooth (CBCentralManager, CBPeripheral, CBService, CBCharacteristic, CBDescriptor, CBUUID and their delegates) -> the same-named shims in com.moblin.android.platform.corebluetooth (see Platform API and the shim sections). Keep the Swift calls and delegate callbacks; never use android.bluetooth for them.
- CoreLocation CLLocationManager -> android.location.LocationManager with requestLocationUpdates. Do not add Google Play Services dependencies.
- Network.framework (NWConnection, NWListener, NWPathMonitor, NWPath, NWParameters, NWEndpoint, NWInterface, NWProtocolTLS, NWProtocolUDP) -> the same-named shims in com.moblin.android.platform.network (see Platform API and the shim sections). Keep the Swift handlers and calls; never use java.net sockets or Dispatchers.IO for them.
- GameController -> android.view.InputDevice with KeyEvent and MotionEvent handling delegated to the Activity.
- Speech SFSpeechRecognizer -> android.speech.SpeechRecognizer.
- CoreHaptics -> android.os.Vibrator.
- libsrt C API -> import com.moblin.android.platform.srt.SrtNative and call its functions and constants by their C names (see Platform API and the shim sections); never declare SrtNative, external functions or System.loadLibrary.

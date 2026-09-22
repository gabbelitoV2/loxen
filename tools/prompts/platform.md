# Tier: platform

This file uses platform services that have Android counterparts. Use the counterpart, keep the same public API shape, and leave permission requests to the Activity layer.

- CoreBluetooth CBCentralManager and CBPeripheral -> android.bluetooth.BluetoothAdapter, BluetoothLeScanner, BluetoothGatt, BluetoothGattCallback. Characteristic UUIDs stay the same.
- CoreLocation CLLocationManager -> android.location.LocationManager with requestLocationUpdates. Do not add Google Play Services dependencies.
- Network.framework -> java.net sockets on Dispatchers.IO. NWPathMonitor -> android.net.ConnectivityManager.NetworkCallback.
- GameController -> android.view.InputDevice with KeyEvent and MotionEvent handling delegated to the Activity.
- Speech SFSpeechRecognizer -> android.speech.SpeechRecognizer.
- CoreHaptics -> android.os.Vibrator.
- libsrt C API -> keep the same method names on a class that calls a SrtNative object with external fun declarations and a companion System.loadLibrary("srt").

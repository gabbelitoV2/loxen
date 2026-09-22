package com.moblin.android.integrations.catprinter

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Log
import com.moblin.android.various.AudioPlayer
import com.moblin.android.various.BluetoothScanner
import com.moblin.android.various.SimpleTimer
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val catPrinterLogTag = "CatPrinter"

private val catPrinterDispatchQueue = CoroutineScope(Dispatchers.IO + SupervisorJob())

val catPrinterWidthPixels = 384

interface CatPrinterDelegate {
    fun catPrinterState(catPrinter: CatPrinter, state: CatPrinterState)
}

enum class CatPrinterState {
    disconnected,
    discovering,
    connecting,
    connected,
}

private enum class DitheringAlgorithm {
    floydSteinberg,
    atkinson,
}

private enum class JobState {
    idle,
    waitingForReady,
    waitingForPrintResponse,
    writingChunks,
    failed,
}

private class CurrentJob(
    val data: ByteArray,
    val mtu: Int,
    val feedPaperDelay: Double?,
    val printMode: CatPrinterPrintMode,
) {
    var offset: Int = 0
    var state: JobState = JobState.idle

    fun setState(state: JobState) {
        if (state == this.state) {
            return
        }
        Log.d(catPrinterLogTag, "cat-printer: Job state change ${this.state} -> $state")
        this.state = state
    }

    fun nextChunk(): ByteArray? {
        if (offset >= data.size) {
            return null
        }
        val chunk = data.copyOfRange(offset, minOf(offset + mtu, data.size))
        if (chunk.isEmpty()) {
            return null
        }
        offset += chunk.size
        return chunk
    }
}

private val catPrinterServices = listOf(UUID.fromString("0000af30-0000-1000-8000-00805f9b34fb"))

val catPrinterScanner = BluetoothScanner(
    context = TODO("BluetoothScanner needs a Context, which is not available here"),
    serviceIds = catPrinterServices,
)

private val printCharacteristicId = UUID.fromString("0000ae01-0000-1000-8000-00805f9b34fb")
private val notifyCharacteristicId = UUID.fromString("0000ae02-0000-1000-8000-00805f9b34fb")
private val dataCharacteristicId = UUID.fromString("0000ae03-0000-1000-8000-00805f9b34fb")
private val clientCharacteristicConfigurationId =
    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

private data class PrintJob(
    val image: Bitmap,
    val feedPaperDelay: Double?,
    val printMode: CatPrinterPrintMode,
)

class CatPrinter : BluetoothGattCallback() {
    private var state: CatPrinterState = CatPrinterState.disconnected
    private var centralManager: BluetoothGatt? = null
    private var peripheral: BluetoothDevice? = null
    private var printCharacteristic: BluetoothGattCharacteristic? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null
    private var dataCharacteristic: BluetoothGattCharacteristic? = null
    private var printJobs: ArrayDeque<PrintJob> = ArrayDeque()
    private var currentJob: CurrentJob? = null
    private var deviceId: String? = null
    private val ditheringAlgorithm: DitheringAlgorithm = DitheringAlgorithm.atkinson
    var delegate: CatPrinterDelegate? = null
    private var tryWriteNextChunkTimer = SimpleTimer(queue = Dispatchers.IO)
    private var jobCompleteTimer = SimpleTimer(queue = Dispatchers.IO)
    private var feedPaperTimer = SimpleTimer(queue = Dispatchers.IO)
    private var audioPlayer: AudioPlayer? = null
    private var meowSoundEnabled: Boolean = false

    fun start(deviceId: String?, meowSoundEnabled: Boolean) {
        catPrinterDispatchQueue.launch {
            this@CatPrinter.meowSoundEnabled = meowSoundEnabled
            startInternal(deviceId = deviceId)
        }
    }

    fun stop() {
        catPrinterDispatchQueue.launch {
            stopInternal()
        }
    }

    fun setMeowSoundEnabled(meowSoundEnabled: Boolean) {
        catPrinterDispatchQueue.launch {
            this@CatPrinter.meowSoundEnabled = meowSoundEnabled
        }
    }

    fun print(image: Bitmap, feedPaperDelay: Double? = null) {
        catPrinterDispatchQueue.launch {
            printInternal(image = image, feedPaperDelay = feedPaperDelay)
        }
    }

    fun getState(): CatPrinterState {
        return state
    }

    private fun maximumWriteValueLength(peripheral: BluetoothDevice): Int =
        TODO("no Android counterpart for CBPeripheral.maximumWriteValueLength, use BluetoothGatt.requestMtu")

    private fun startInternal(deviceId: String?) {
        this.deviceId = deviceId
        reset()
        reconnect()
    }

    private fun stopInternal() {
        reset()
    }

    private fun isMxw01(): Boolean {
        return peripheral?.name == "MXW01"
    }

    private fun printInternal(image: Bitmap, feedPaperDelay: Double?) {
        if (printJobs.size >= 50) {
            return
        }
        printJobs.addLast(
            PrintJob(
                image = image,
                feedPaperDelay = feedPaperDelay,
                printMode = CatPrinterPrintMode.blackAndWhite,
            ),
        )
        tryPrintNext()
    }

    private fun tryPrintNext() {
        val peripheral = peripheral
        if (peripheral == null) {
            reconnect()
            return
        }
        if (currentJob != null) {
            return
        }
        val printJob = printJobs.removeFirstOrNull() ?: return
        val image: Array<UByteArray>
        try {
            image = processImage(image = printJob.image, printMode = printJob.printMode)
        } catch (error: Exception) {
            Log.i(catPrinterLogTag, "cat-printer: $error")
            return
        }
        if (isMxw01()) {
            tryPrintNextMxw01(printJob = printJob, image = image, peripheral = peripheral)
        } else {
            tryPrintNextDefault(printJob = printJob, image = image, peripheral = peripheral)
        }
        if (meowSoundEnabled) {
            playMeowSound()
        }
    }

    private fun tryPrintNextMxw01(
        printJob: PrintJob,
        image: Array<UByteArray>,
        peripheral: BluetoothDevice,
    ) {
        val data = catPrinterPackPrintImageCommandsMxw01(
            image = image,
            printMode = printJob.printMode,
        )
        currentJob = CurrentJob(
            data = data,
            mtu = maximumWriteValueLength(peripheral),
            feedPaperDelay = printJob.feedPaperDelay,
            printMode = printJob.printMode,
        )
        val printCharacteristic = printCharacteristic
        val currentJob = currentJob
        if (printCharacteristic == null || currentJob == null) {
            reconnect()
            return
        }
        currentJob.setState(state = JobState.waitingForReady)
        send(command = CatPrinterCommandMxw01.statusRequest, peripheral, printCharacteristic)
        startJobCompleteTimer()
    }

    private fun tryPrintNextDefault(
        printJob: PrintJob,
        image: Array<UByteArray>,
        peripheral: BluetoothDevice,
    ) {
        val data = catPrinterPackPrintImageCommands(
            image = image.map { row ->
                catPrinterEncodeImageRow(imageRow = row, printMode = printJob.printMode)
            },
            feedPaper = printJob.feedPaperDelay == null,
            printMode = printJob.printMode,
        )
        currentJob = CurrentJob(
            data = data,
            mtu = maximumWriteValueLength(peripheral),
            feedPaperDelay = printJob.feedPaperDelay,
            printMode = printJob.printMode,
        )
        stopFeedPaperTimer()
        val printCharacteristic = printCharacteristic
        val currentJob = currentJob
        if (printCharacteristic == null || currentJob == null) {
            reconnect()
            return
        }
        send(command = CatPrinterCommand.GetDeviceState(), peripheral, printCharacteristic)
        currentJob.setState(state = JobState.waitingForReady)
    }

    private fun playMeowSound() {
        TODO("no Android counterpart for Bundle.main resource lookup and AudioPlayer construction")
    }

    private fun send(
        command: CatPrinterCommand,
        peripheral: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
    ) {
        send(data = command.pack(), peripheral, characteristic)
    }

    private fun send(
        command: CatPrinterCommandMxw01,
        peripheral: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
    ) {
        send(data = command.pack(), peripheral, characteristic)
    }

    private fun send(
        data: ByteArray,
        peripheral: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
    ) {
        characteristic.value = data
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        centralManager?.writeCharacteristic(characteristic)
    }

    private fun tryWriteNextChunk() {
        if (isMxw01()) {
            tryWriteNextChunkMxw01()
        } else {
            tryWriteNextChunkDefault()
        }
    }

    private fun tryWriteNextChunkMxw01() {
        val peripheral = peripheral
        val dataCharacteristic = dataCharacteristic
        if (peripheral == null || dataCharacteristic == null) {
            reconnect()
            return
        }
        val chunk = currentJob?.nextChunk()
        if (chunk != null) {
            send(data = chunk, peripheral, dataCharacteristic)
            startTryWriteNextChunkTimer()
        }
    }

    private fun tryWriteNextChunkDefault() {
        val peripheral = peripheral
        val printCharacteristic = printCharacteristic
        if (peripheral == null || printCharacteristic == null) {
            reconnect()
            return
        }
        val chunk = currentJob?.nextChunk()
        if (chunk != null) {
            send(data = chunk, peripheral, printCharacteristic)
            startTryWriteNextChunkTimer()
        } else {
            val feedPaperDelay = currentJob?.feedPaperDelay
            if (feedPaperDelay != null && printJobs.isEmpty()) {
                stopFeedPaperTimer()
                startFeedPaperTimer(delay = feedPaperDelay)
            }
            currentJob = null
            tryPrintNext()
        }
    }

    private fun processImage(image: Bitmap, printMode: CatPrinterPrintMode): Array<UByteArray> {
        var image = makeMonochrome(image = image)
        image = scaleToPrinterWidth(image = image)
        var pixels = convertToPixels(image = image)
        if (printMode == CatPrinterPrintMode.blackAndWhite) {
            pixels = when (ditheringAlgorithm) {
                DitheringAlgorithm.floydSteinberg -> {
                    val dithered = FloydSteinbergDithering().apply(
                        image = pixels.map { row -> row.toList() },
                    )
                    Array(dithered.size) { row -> dithered[row].toUByteArray() }
                }
                DitheringAlgorithm.atkinson -> {
                    val dithered = AtkinsonDithering().apply(image = pixels.toMutableList())
                    Array(dithered.size) { row -> dithered[row] }
                }
            }
        }
        return when (printMode) {
            CatPrinterPrintMode.blackAndWhite -> Array(pixels.size) { row ->
                UByteArray(pixels[row].size) { column ->
                    ((255 - pixels[row][column].toInt()) / 128).toUByte()
                }
            }
            CatPrinterPrintMode.grayscale -> Array(pixels.size) { row ->
                UByteArray(pixels[row].size) { column ->
                    ((255 - pixels[row][column].toInt()) / 16).toUByte()
                }
            }
        }
    }

    private fun makeMonochrome(image: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0.0f) })
        canvas.drawBitmap(image, 0.0f, 0.0f, paint)
        return output
    }

    private fun scaleToPrinterWidth(image: Bitmap): Bitmap {
        val scale = catPrinterWidthPixels.toFloat() / image.width.toFloat()
        val height = max(1, (image.height * scale).roundToInt())
        return Bitmap.createScaledBitmap(image, catPrinterWidthPixels, height, true)
    }

    private fun convertToPixels(image: Bitmap): Array<UByteArray> {
        val width = image.width
        val height = image.height
        val pixels = IntArray(width * height)
        image.getPixels(pixels, 0, width, 0, 0, width, height)
        return Array(height) { row ->
            UByteArray(width) { column ->
                val pixel = pixels[row * width + column]
                val alpha = (pixel ushr 24) and 0xFF
                if (alpha != 255) {
                    255.toUByte()
                } else {
                    ((pixel ushr 16) and 0xFF).toUByte()
                }
            }
        }
    }

    private fun reset() {
        centralManager = null
        peripheral = null
        printCharacteristic = null
        notifyCharacteristic = null
        dataCharacteristic = null
        printJobs.clear()
        currentJob = null
        stopTryWriteNextChunkTimer()
        stopJobCompleteTimer()
        stopFeedPaperTimer()
        setState(state = CatPrinterState.disconnected)
    }

    private fun reconnect() {
        peripheral = null
        printCharacteristic = null
        notifyCharacteristic = null
        dataCharacteristic = null
        currentJob = null
        setState(state = CatPrinterState.discovering)
        stopTryWriteNextChunkTimer()
        stopJobCompleteTimer()
        stopFeedPaperTimer()
        centralManager = TODO("CBCentralManager has no counterpart, BluetoothGatt needs a Context")
    }

    private fun setState(state: CatPrinterState) {
        if (state == this.state) {
            return
        }
        Log.d(catPrinterLogTag, "cat-printer: State change ${this.state} -> $state")
        this.state = state
        delegate?.catPrinterState(catPrinter = this, state = state)
    }

    private fun startTryWriteNextChunkTimer() {
        tryWriteNextChunkTimer.startSingleShot(timeout = 0.1) {
            tryWriteNextChunk()
        }
    }

    private fun stopTryWriteNextChunkTimer() {
        tryWriteNextChunkTimer.stop()
    }

    private fun startJobCompleteTimer() {
        jobCompleteTimer.startSingleShot(timeout = 60.0) {
            currentJob = null
            tryPrintNext()
        }
    }

    private fun stopJobCompleteTimer() {
        jobCompleteTimer.stop()
    }

    private fun startFeedPaperTimer(delay: Double) {
        feedPaperTimer.startSingleShot(timeout = delay) {
            feedPaper()
        }
    }

    private fun stopFeedPaperTimer() {
        feedPaperTimer.stop()
    }

    private fun feedPaper() {
        val peripheral = peripheral
        val printCharacteristic = printCharacteristic
        if (peripheral == null || printCharacteristic == null) {
            return
        }
        send(
            command = CatPrinterCommand.FeedPaper(pixels = catPrinterFeedPaperPixels),
            peripheral,
            printCharacteristic,
        )
    }

    private fun connect(central: BluetoothGatt) {
        val deviceId = deviceId
        if (deviceId == null) {
            Log.i(catPrinterLogTag, "cat-printer: Device not found")
            return
        }
        peripheral = TODO("BluetoothAdapter.getRemoteDevice(address) requires a BluetoothAdapter and Context")
        setState(state = CatPrinterState.connecting)
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        when (newState) {
            BluetoothProfile.STATE_CONNECTED -> gatt.discoverServices()
            BluetoothProfile.STATE_DISCONNECTED -> reconnect()
            else -> Unit
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            return
        }
        val service = gatt.services.firstOrNull()
        if (service == null) {
            return
        }
        for (characteristic in service.characteristics) {
            when (characteristic.uuid) {
                printCharacteristicId -> printCharacteristic = characteristic
                notifyCharacteristicId -> {
                    notifyCharacteristic = characteristic
                    gatt.setCharacteristicNotification(characteristic, true)
                    setNotifyValue(gatt = gatt, characteristic = characteristic)
                }
                dataCharacteristicId -> dataCharacteristic = characteristic
                else -> Unit
            }
        }
        if (printCharacteristic != null && notifyCharacteristic != null && dataCharacteristic != null) {
            setState(state = CatPrinterState.connected)
            tryPrintNext()
        }
    }

    private fun setNotifyValue(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        val descriptor = characteristic.getDescriptor(clientCharacteristicConfigurationId) ?: return
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt.writeDescriptor(descriptor)
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
    ) {
        if (isMxw01()) {
            handleMessageMxw01(characteristic = characteristic)
        } else {
            handleMessageDefault(characteristic = characteristic)
        }
    }

    private fun handleMessageMxw01(characteristic: BluetoothGattCharacteristic) {
        val value = characteristic.value ?: return
        val command = CatPrinterCommandMxw01.fromData(data = value) ?: return
        val currentJob = currentJob ?: return
        when (currentJob.state) {
            JobState.waitingForReady ->
                handleMessageMxw01WaitingForReady(command = command, currentJob = currentJob)
            JobState.waitingForPrintResponse ->
                handleMessageMxw01WaitingForPrintResponse(command = command, currentJob = currentJob)
            JobState.writingChunks -> handleMessageMxw01WritingChunks(command = command)
            else -> Unit
        }
    }

    private fun handleMessageMxw01WaitingForReady(
        command: CatPrinterCommandMxw01,
        currentJob: CurrentJob,
    ) {
        val peripheral = peripheral
        val printCharacteristic = printCharacteristic
        if (peripheral == null || printCharacteristic == null) {
            return
        }
        when (command) {
            is CatPrinterCommandMxw01.statusResponse -> {
                currentJob.setState(state = JobState.waitingForPrintResponse)
                val bytesPerLine: Int = when (currentJob.printMode) {
                    CatPrinterPrintMode.blackAndWhite -> catPrinterWidthPixels / 8
                    CatPrinterPrintMode.grayscale -> catPrinterWidthPixels / 2
                }
                val lineCount = (currentJob.data.size / bytesPerLine).toUShort()
                send(
                    command = CatPrinterCommandMxw01.printRequest(
                        printMode = currentJob.printMode,
                        count = lineCount,
                    ),
                    peripheral,
                    printCharacteristic,
                )
            }
            else -> Unit
        }
    }

    private fun handleMessageMxw01WaitingForPrintResponse(
        command: CatPrinterCommandMxw01,
        currentJob: CurrentJob,
    ) {
        when (command) {
            is CatPrinterCommandMxw01.printResponse -> {
                if (command.status.toInt() == 0) {
                    currentJob.setState(state = JobState.writingChunks)
                    tryWriteNextChunk()
                } else {
                    currentJob.setState(state = JobState.failed)
                }
            }
            else -> Unit
        }
    }

    private fun handleMessageMxw01WritingChunks(command: CatPrinterCommandMxw01) {
        when (command) {
            is CatPrinterCommandMxw01.printCompleteIndication -> {
                stopJobCompleteTimer()
                currentJob = null
                tryPrintNext()
            }
            else -> Unit
        }
    }

    private fun handleMessageDefault(characteristic: BluetoothGattCharacteristic) {
        val value = characteristic.value ?: return
        val currentJob = currentJob ?: return
        val command = CatPrinterCommand.fromData(data = value) ?: return
        when (currentJob.state) {
            JobState.idle -> Unit
            JobState.waitingForReady -> {
                when (command) {
                    is CatPrinterCommand.GetDeviceState -> {
                        currentJob.setState(state = JobState.writingChunks)
                        tryWriteNextChunk()
                    }
                    else -> Unit
                }
            }
            JobState.writingChunks -> {
                when (command) {
                    is CatPrinterCommand.WritePacing -> tryWriteNextChunk()
                    else -> Unit
                }
            }
            else -> Unit
        }
    }
}

fun catPrinterEncodeImageRow(imageRow: UByteArray, printMode: CatPrinterPrintMode): ByteArray {
    return when (printMode) {
        CatPrinterPrintMode.blackAndWhite -> {
            val data = ByteArray(imageRow.size / 8)
            for (byteIndex in 0 until data.size) {
                var byte: Int = 0
                for (bitIndex in 0 until 8) {
                    if (imageRow[8 * byteIndex + bitIndex].toInt() == 1) {
                        byte = byte or (1 shl bitIndex)
                    }
                }
                data[byteIndex] = byte.toByte()
            }
            data
        }
        CatPrinterPrintMode.grayscale -> {
            val data = ByteArray(imageRow.size / 2)
            for (byteIndex in 0 until data.size) {
                data[byteIndex] = (
                    (imageRow[2 * byteIndex].toInt() shl 4) or
                        imageRow[2 * byteIndex + 1].toInt()
                    ).toByte()
            }
            data
        }
    }
}

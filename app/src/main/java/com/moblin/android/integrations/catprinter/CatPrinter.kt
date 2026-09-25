package com.moblin.android.integrations.catprinter

import android.util.Log
import com.moblin.android.platform.Bundle
import com.moblin.android.platform.corebluetooth.CBCentralManager
import com.moblin.android.platform.corebluetooth.CBCentralManagerDelegate
import com.moblin.android.platform.corebluetooth.CBCharacteristic
import com.moblin.android.platform.corebluetooth.CBCharacteristicWriteType
import com.moblin.android.platform.corebluetooth.CBManagerState
import com.moblin.android.platform.corebluetooth.CBPeripheral
import com.moblin.android.platform.corebluetooth.CBPeripheralDelegate
import com.moblin.android.platform.corebluetooth.CBService
import com.moblin.android.platform.corebluetooth.CBUUID
import com.moblin.android.platform.coregraphics.bitsPerComponent
import com.moblin.android.platform.coregraphics.bitsPerPixel
import com.moblin.android.platform.coregraphics.dataProvider
import com.moblin.android.platform.coreimage.CIColor
import com.moblin.android.platform.coreimage.CIContext
import com.moblin.android.platform.coreimage.CIFilter
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.various.AudioPlayer
import com.moblin.android.various.BluetoothScanner
import com.moblin.android.various.SimpleTimer
import com.moblin.android.videoeffects.scaled
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch

private val catPrinterDispatchQueue: CoroutineDispatcher =
    Executors.newSingleThreadExecutor().asCoroutineDispatcher()

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

    @JvmName("updateState")
    fun setState(state: JobState) {
        if (state == this.state) {
            return
        }
        Log.d("CurrentJob", "cat-printer: Job state change ${this.state} -> $state")
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

private val catPrinterServices = listOf(
    CBUUID(string = "0000af30-0000-1000-8000-00805f9b34fb"),
)

val catPrinterScanner = BluetoothScanner(serviceIds = catPrinterServices)

private val printCharacteristicId = CBUUID(string = "AE01")
private val notifyCharacteristicId = CBUUID(string = "AE02")
private val dataCharacteristicId = CBUUID(string = "AE03")

private data class PrintJob(
    val image: CIImage,
    val feedPaperDelay: Double?,
    val printMode: CatPrinterPrintMode,
)

class CatPrinter : CBCentralManagerDelegate, CBPeripheralDelegate {
    private var state: CatPrinterState = CatPrinterState.disconnected
    private var centralManager: CBCentralManager? by CBCentralManager.holder()
    private var peripheral: CBPeripheral? = null
    private var printCharacteristic: CBCharacteristic? = null
    private var notifyCharacteristic: CBCharacteristic? = null
    private var dataCharacteristic: CBCharacteristic? = null
    private val context = CIContext()
    private var printJobs: MutableList<PrintJob> = mutableListOf()
    private var currentJob: CurrentJob? = null
    private var deviceId: UUID? = null
    private val ditheringAlgorithm: DitheringAlgorithm = DitheringAlgorithm.atkinson
    var delegate: CatPrinterDelegate? = null
    private var tryWriteNextChunkTimer = SimpleTimer(queue = catPrinterDispatchQueue)
    private var jobCompleteTimer = SimpleTimer(queue = catPrinterDispatchQueue)
    private var feedPaperTimer = SimpleTimer(queue = catPrinterDispatchQueue)
    private var audioPlayer: AudioPlayer? = null
    private var meowSoundEnabled: Boolean = false

    fun start(deviceId: UUID?, meowSoundEnabled: Boolean) {
        CoroutineScope(catPrinterDispatchQueue).launch {
            this@CatPrinter.meowSoundEnabled = meowSoundEnabled
            startInternal(deviceId = deviceId)
        }
    }

    fun stop() {
        CoroutineScope(catPrinterDispatchQueue).launch {
            stopInternal()
        }
    }

    fun setMeowSoundEnabled(meowSoundEnabled: Boolean) {
        CoroutineScope(catPrinterDispatchQueue).launch {
            this@CatPrinter.meowSoundEnabled = meowSoundEnabled
        }
    }

    fun print(image: CIImage, feedPaperDelay: Double? = null) {
        CoroutineScope(catPrinterDispatchQueue).launch {
            printInternal(image = image, feedPaperDelay = feedPaperDelay)
        }
    }

    @JvmName("currentState")
    fun getState(): CatPrinterState {
        return state
    }

    private fun startInternal(deviceId: UUID?) {
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

    private fun printInternal(image: CIImage, feedPaperDelay: Double?) {
        if (printJobs.size >= 50) {
            return
        }
        printJobs.add(
            PrintJob(
                image = image,
                feedPaperDelay = feedPaperDelay,
                printMode = CatPrinterPrintMode.blackAndWhite,
            )
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
        val image = try {
            processImage(image = printJob.image, printMode = printJob.printMode)
        } catch (error: Exception) {
            Log.i("CatPrinter", "cat-printer: $error")
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
        image: MutableList<UByteArray>,
        peripheral: CBPeripheral,
    ) {
        val data = catPrinterPackPrintImageCommandsMxw01(
            image = image,
            printMode = printJob.printMode,
        )
        currentJob = CurrentJob(
            data = data,
            mtu = peripheral.maximumWriteValueLength(`for` = CBCharacteristicWriteType.withoutResponse),
            feedPaperDelay = printJob.feedPaperDelay,
            printMode = printJob.printMode,
        )
        val printCharacteristic = printCharacteristic
        val currentJob = currentJob
        if (printCharacteristic == null || currentJob == null) {
            reconnect()
            return
        }
        currentJob.setState(JobState.waitingForReady)
        send(CatPrinterCommandMxw01.StatusRequest, peripheral, printCharacteristic)
        startJobCompleteTimer()
    }

    private fun tryPrintNextDefault(
        printJob: PrintJob,
        image: MutableList<UByteArray>,
        peripheral: CBPeripheral,
    ) {
        val data = catPrinterPackPrintImageCommands(
            image = image,
            feedPaper = printJob.feedPaperDelay == null,
            printMode = printJob.printMode,
        )
        currentJob = CurrentJob(
            data = data,
            mtu = peripheral.maximumWriteValueLength(`for` = CBCharacteristicWriteType.withoutResponse),
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
        send(CatPrinterCommand.GetDeviceState(), peripheral, printCharacteristic)
        currentJob.setState(JobState.waitingForReady)
    }

    private fun playMeowSound() {
        val soundUrl = Bundle.url("Alerts.bundle/Nya", "mp3") ?: return
        audioPlayer = runCatching { AudioPlayer(contentsOf = soundUrl) }.getOrNull()
        audioPlayer?.play()
    }

    private fun send(
        command: CatPrinterCommand,
        peripheral: CBPeripheral,
        characteristic: CBCharacteristic,
    ) {
        send(data = command.pack(), peripheral = peripheral, characteristic = characteristic)
    }

    private fun send(
        command: CatPrinterCommandMxw01,
        peripheral: CBPeripheral,
        characteristic: CBCharacteristic,
    ) {
        send(data = command.pack(), peripheral = peripheral, characteristic = characteristic)
    }

    private fun send(
        data: ByteArray,
        peripheral: CBPeripheral,
        characteristic: CBCharacteristic,
    ) {
        peripheral.writeValue(data, `for` = characteristic, type = CBCharacteristicWriteType.withoutResponse)
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
            send(data = chunk, peripheral = peripheral, characteristic = dataCharacteristic)
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
            send(data = chunk, peripheral = peripheral, characteristic = printCharacteristic)
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

    private fun processImage(image: CIImage, printMode: CatPrinterPrintMode): MutableList<UByteArray> {
        var image = makeMonochrome(image = image)
        image = scaleToPrinterWidth(image = image)
        var pixels = convertToPixels(image = image)
        if (printMode == CatPrinterPrintMode.blackAndWhite) {
            when (ditheringAlgorithm) {
                DitheringAlgorithm.floydSteinberg -> {
                    pixels = FloydSteinbergDithering()
                        .apply(pixels.map { it.toList() })
                        .map { it.toUByteArray() }
                        .toMutableList()
                }
                DitheringAlgorithm.atkinson -> {
                    pixels = AtkinsonDithering().apply(pixels)
                }
            }
        }
        return when (printMode) {
            CatPrinterPrintMode.blackAndWhite -> pixels
                .map { row -> row.map { ((255 - it.toInt()) / 128).toUByte() }.toUByteArray() }
                .toMutableList()
            CatPrinterPrintMode.grayscale -> pixels
                .map { row -> row.map { ((255 - it.toInt()) / 16).toUByte() }.toUByteArray() }
                .toMutableList()
        }
    }

    private fun makeMonochrome(image: CIImage): CIImage {
        val filter = CIFilter.colorMonochrome()
        filter.inputImage = image
        filter.color = CIColor(red = 0.9, green = 0.9, blue = 0.9)
        filter.intensity = 1f
        return filter.outputImage ?: image
    }

    private fun scaleToPrinterWidth(image: CIImage): CIImage {
        val scale = catPrinterWidthPixels.toDouble() / image.extent.width
        return image.scaled(x = scale, y = scale)
    }

    private fun convertToPixels(image: CIImage): MutableList<UByteArray> {
        val cgImage = context.createCGImage(image, from = image.extent)
            ?: throw CatPrinterError("Failed to create core graphics image")
        val data = cgImage.dataProvider?.data ?: throw CatPrinterError("Failed to get data")
        var length = data.size
        if (cgImage.bitsPerComponent != 8) {
            throw CatPrinterError("Expected 8 bits per component, but got ${cgImage.bitsPerComponent}")
        }
        if (cgImage.bitsPerPixel != 32) {
            throw CatPrinterError("Expected 32 bits per pixel, but got ${cgImage.bitsPerPixel}")
        }
        val widthPixels = image.extent.width.toInt()
        length = minOf(length, 4 * (image.extent.width * image.extent.height).toInt())
        val pixels = mutableListOf<UByteArray>()
        for (rowOffset in (0 until length) step (4 * widthPixels)) {
            val row = mutableListOf<UByte>()
            for (columnOffset in (0 until (4 * widthPixels)) step 4) {
                if ((data[rowOffset + columnOffset + 3].toInt() and 0xFF) != 255) {
                    row.add(255.toUByte())
                } else {
                    row.add(data[rowOffset + columnOffset].toUByte())
                }
            }
            pixels.add(row.toUByteArray())
        }
        return pixels
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
        setState(CatPrinterState.disconnected)
    }

    private fun reconnect() {
        peripheral = null
        printCharacteristic = null
        notifyCharacteristic = null
        dataCharacteristic = null
        currentJob = null
        setState(CatPrinterState.discovering)
        stopTryWriteNextChunkTimer()
        stopJobCompleteTimer()
        stopFeedPaperTimer()
        centralManager = CBCentralManager(
            delegate = this,
            queue = CoroutineScope(catPrinterDispatchQueue),
        )
    }

    private fun setState(state: CatPrinterState) {
        if (state == this.state) {
            return
        }
        Log.d("CatPrinter", "cat-printer: State change ${this.state} -> $state")
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
            CatPrinterCommand.FeedPaper(pixels = catPrinterFeedPaperPixels),
            peripheral,
            printCharacteristic,
        )
    }

    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        when (central.state) {
            CBManagerState.poweredOn -> connect(central)
            else -> {}
        }
    }

    private fun connect(central: CBCentralManager) {
        val deviceId = deviceId
        val peripheral = if (deviceId != null) {
            central.retrievePeripherals(withIdentifiers = listOf(deviceId)).firstOrNull()
        } else {
            null
        }
        if (peripheral == null) {
            Log.i("CatPrinter", "cat-printer: Device not found")
            return
        }
        this.peripheral = peripheral
        peripheral.delegate = this
        central.connect(peripheral)
        setState(CatPrinterState.connecting)
    }

    override fun centralManagerDidFailToConnect(
        central: CBCentralManager,
        peripheral: CBPeripheral,
        error: Throwable?,
    ) {
    }

    override fun centralManagerDidConnect(central: CBCentralManager, peripheral: CBPeripheral) {
        peripheral.discoverServices(null)
    }

    override fun centralManagerDidDisconnectPeripheral(
        central: CBCentralManager,
        peripheral: CBPeripheral,
        error: Throwable?,
    ) {
        reconnect()
    }

    override fun peripheralDidDiscoverServices(peripheral: CBPeripheral, error: Throwable?) {
        val service = peripheral.services?.firstOrNull()
        if (service != null) {
            peripheral.discoverCharacteristics(null, `for` = service)
        }
    }

    override fun peripheralDidDiscoverCharacteristicsFor(
        peripheralArg: CBPeripheral,
        service: CBService,
        error: Throwable?,
    ) {
        for (characteristic in service.characteristics.orEmpty()) {
            when (characteristic.uuid) {
                printCharacteristicId -> printCharacteristic = characteristic
                notifyCharacteristicId -> {
                    notifyCharacteristic = characteristic
                    this.peripheral?.setNotifyValue(true, `for` = characteristic)
                }
                dataCharacteristicId -> dataCharacteristic = characteristic
                else -> {}
            }
        }
        if (printCharacteristic != null && notifyCharacteristic != null && dataCharacteristic != null) {
            setState(CatPrinterState.connected)
            tryPrintNext()
        }
    }

    override fun peripheralDidUpdateValueFor(
        peripheral: CBPeripheral,
        characteristic: CBCharacteristic,
        error: Throwable?,
    ) {
        if (isMxw01()) {
            handleMessageMxw01(characteristic = characteristic)
        } else {
            handleMessageDefault(characteristic = characteristic)
        }
    }

    private fun handleMessageMxw01(characteristic: CBCharacteristic) {
        val value = characteristic.value ?: return
        val command = CatPrinterCommandMxw01(value) ?: return
        val currentJob = currentJob ?: return
        when (currentJob.state) {
            JobState.waitingForReady -> handleMessageMxw01WaitingForReady(command, currentJob)
            JobState.waitingForPrintResponse -> handleMessageMxw01WaitingForPrintResponse(command, currentJob)
            JobState.writingChunks -> handleMessageMxw01WritingChunks(command)
            else -> {}
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
        if (command is CatPrinterCommandMxw01.StatusResponse) {
            currentJob.setState(JobState.waitingForPrintResponse)
            val bytesPerLine = when (currentJob.printMode) {
                CatPrinterPrintMode.blackAndWhite -> catPrinterWidthPixels / 8
                CatPrinterPrintMode.grayscale -> catPrinterWidthPixels / 2
            }
            val lineCount = (currentJob.data.size / bytesPerLine).toUShort()
            send(
                CatPrinterCommandMxw01.PrintRequest(
                    printMode = currentJob.printMode,
                    count = lineCount,
                ),
                peripheral,
                printCharacteristic,
            )
        }
    }

    private fun handleMessageMxw01WaitingForPrintResponse(
        command: CatPrinterCommandMxw01,
        currentJob: CurrentJob,
    ) {
        if (command is CatPrinterCommandMxw01.PrintResponse) {
            if (command.status == 0.toUByte()) {
                currentJob.setState(JobState.writingChunks)
                tryWriteNextChunk()
            } else {
                currentJob.setState(JobState.failed)
            }
        }
    }

    private fun handleMessageMxw01WritingChunks(command: CatPrinterCommandMxw01) {
        if (command is CatPrinterCommandMxw01.PrintCompleteIndication) {
            stopJobCompleteTimer()
            currentJob = null
            tryPrintNext()
        }
    }

    private fun handleMessageDefault(characteristic: CBCharacteristic) {
        val value = characteristic.value ?: return
        val currentJob = currentJob ?: return
        val command = CatPrinterCommand(value) ?: return
        when (currentJob.state) {
            JobState.idle -> {}
            JobState.waitingForReady -> when (command) {
                is CatPrinterCommand.GetDeviceState -> {
                    currentJob.setState(JobState.writingChunks)
                    tryWriteNextChunk()
                }
                else -> {}
            }
            JobState.writingChunks -> when (command) {
                is CatPrinterCommand.WritePacing -> tryWriteNextChunk()
                else -> {}
            }
            else -> {}
        }
    }
}

fun catPrinterEncodeImageRow(imageRow: UByteArray, printMode: CatPrinterPrintMode): ByteArray {
    return when (printMode) {
        CatPrinterPrintMode.blackAndWhite -> {
            val data = ByteArray(imageRow.size / 8)
            for (byteIndex in 0 until data.size) {
                var byte = 0
                for (bitIndex in 0 until 8) {
                    if (imageRow[8 * byteIndex + bitIndex] == 1.toUByte()) {
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
                data[byteIndex] = (((imageRow[2 * byteIndex].toInt() shl 4) or
                    imageRow[2 * byteIndex + 1].toInt()) and 0xFF).toByte()
            }
            data
        }
    }
}

private class CatPrinterError(message: String) : Exception(message) {
    override fun toString(): String {
        return message ?: ""
    }
}

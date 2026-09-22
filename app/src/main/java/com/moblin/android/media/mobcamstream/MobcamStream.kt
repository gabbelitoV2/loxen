package com.moblin.android.media.mobcamstream

import android.media.MediaFormat
import android.util.Log
import com.moblin.android.common.various.appVersion
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.codec.audio.AudioEncoderDelegate
import com.moblin.android.media.haishinkit.codec.video.VideoEncoder
import com.moblin.android.media.haishinkit.codec.video.VideoEncoderDelegate
import com.moblin.android.media.haishinkit.mpeg.MpegTsAudioConfig
import com.moblin.android.media.haishinkit.mpeg.avc.MpegTsVideoConfigAvc
import com.moblin.android.media.haishinkit.mpeg.hevc.MpegTsVideoConfigHevc
import com.moblin.android.media.haishinkit.util.BitrateStats
import com.moblin.android.various.SimpleTimer
import com.moblin.android.various.network.DefaultTcpPorts
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

private const val TAG = "MobcamStream"

private val mobcamStreamQueue = Executors.newSingleThreadExecutor { runnable ->
    Thread(runnable, "com.eerimoq.mobcam-stream")
}.asCoroutineDispatcher()

private val mobcamStreamScope = CoroutineScope(SupervisorJob() + mobcamStreamQueue)

private const val congestionThresholeBytes = 10_000_000

interface MobcamStreamDelegate {
    fun mobcamStreamOnConnected()
    fun mobcamStreamOnDisconnected(reason: String)
    fun <T> mobcamStreamStartEncoding(delegate: T) where T : AudioEncoderDelegate, T : VideoEncoderDelegate
    fun <T> mobcamStreamStopEncoding(delegate: T) where T : AudioEncoderDelegate, T : VideoEncoderDelegate
}

class MobcamStream(private val delegate: MobcamStreamDelegate?) :
    AudioEncoderDelegate,
    VideoEncoderDelegate
{
    private var listener: ServerSocket? = null
    private var listenerJob: Job? = null
    private var connection: Socket? = null
    private var receiveJob: Job? = null
    private var writerChannel: Channel<ByteArray>? = null
    private var writerJob: Job? = null
    private var reader = MobcamStreamMessageReader()
    private var connectedAt: Long? = null
    private var acceptedAt: Long? = null
    private var encoding = false
    private var port = 0
    private var bitrateStats = BitrateStats()
    private val outstandingByteCount = AtomicInteger(0)
    @Volatile private var congestedAt: Long? = null
    private var dropUntilSync = false
    private var audioSupported = false
    private val periodicTimer = SimpleTimer(queue = mobcamStreamQueue)
    private var deviceName = ""

    fun start(port: Int, deviceName: String) {
        mobcamStreamScope.launch {
            this@MobcamStream.port = port
            this@MobcamStream.deviceName = deviceName
            setupListener()
            setupPeriodicTimer()
        }
    }

    fun stop() {
        mobcamStreamScope.launch {
            periodicTimer.stop()
            closeConnection("Stopping")
            stopListener()
        }
    }

    fun getSpeed(): ULong = runBlocking(mobcamStreamQueue) {
        (8 * bitrateStats.update().speed.toLong()).toULong()
    }

    fun getTotalByteCount(): Long = runBlocking(mobcamStreamQueue) {
        bitrateStats.totalBytes.toLong()
    }

    private fun stopListener() {
        listenerJob?.cancel()
        listenerJob = null
        runCatching { listener?.close() }
        listener = null
    }

    private fun setupListener() {
        stopListener()
        val serverSocket = try {
            ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress("127.0.0.1", port))
            }
        } catch (e: Exception) {
            Log.i(TAG, "mobcam-stream: Failed to create listener with error $e")
            return
        }
        listener = serverSocket
        handleListenerStateChange("started")
        listenerJob = mobcamStreamScope.launch(Dispatchers.IO) {
            while (isActive) {
                val socket = try {
                    serverSocket.accept()
                } catch (e: Exception) {
                    withContext(mobcamStreamQueue) {
                        handleListenerStateChange("failed")
                    }
                    return@launch
                }
                withContext(mobcamStreamQueue) {
                    handleNewListenerConnection(socket)
                }
            }
        }
    }

    private fun setupPeriodicTimer() {
        periodicTimer.startPeriodic(interval = 1) {
            if (listener == null || listener?.isClosed == true) {
                setupListener()
            }
            acceptedAt?.let {
                if (connectedAt == null && System.nanoTime() - it > 5_000_000_000L) {
                    closeConnection("Hello timeout")
                }
            }
            congestedAt?.let {
                if (System.nanoTime() - it > 5_000_000_000L) {
                    closeConnection("Host is not reading")
                }
            }
        }
    }

    private fun handleListenerStateChange(state: String) {
        Log.i(TAG, "mobcam-stream: Listener state change to $state")
    }

    private fun handleNewListenerConnection(connection: Socket) {
        if (this.connection != null) {
            closeConnection("Another host connected")
        }
        Log.i(TAG, "mobcam-stream: Host connected")
        this.connection = connection
        reader = MobcamStreamMessageReader()
        acceptedAt = System.nanoTime()
        setupWriter(connection)
        receive(connection)
    }

    private fun setupWriter(connection: Socket) {
        val channel = Channel<ByteArray>(Channel.UNLIMITED)
        writerChannel = channel
        writerJob = mobcamStreamScope.launch(Dispatchers.IO) {
            try {
                val output = connection.getOutputStream()
                for (data in channel) {
                    output.write(data)
                    output.flush()
                    outstandingByteCount.addAndGet(-data.size)
                    if (outstandingByteCount.get() < congestionThresholeBytes) {
                        congestedAt = null
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.i(TAG, "mobcam-stream: Send failed with error $e")
            }
        }
    }

    private fun handleConnectionStateChange(connection: Socket, state: String) {
        if (connection !== this.connection) {
            return
        }
        when (state) {
            "failed" -> closeConnection("Connection failed")
            "cancelled" -> {}
            else -> {}
        }
    }

    private fun receive(connection: Socket) {
        receiveJob = mobcamStreamScope.launch(Dispatchers.IO) {
            try {
                val input = connection.getInputStream()
                val buffer = ByteArray(65536)
                while (isActive) {
                    val length = input.read(buffer)
                    if (length < 0) {
                        withContext(mobcamStreamQueue) {
                            closeConnection("Host closed the connection")
                        }
                        return@launch
                    }
                    if (length > 0) {
                        val data = buffer.copyOf(length)
                        val ok = withContext(mobcamStreamQueue) {
                            handleReceived(data)
                        }
                        if (!ok) {
                            return@launch
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                withContext(mobcamStreamQueue) {
                    handleConnectionStateChange(connection, "failed")
                }
            }
        }
    }

    private fun handleReceived(data: ByteArray): Boolean {
        reader.append(data)
        try {
            while (true) {
                val message = reader.read() ?: break
                handleMessage(message.first, message.second)
            }
        } catch (e: Exception) {
            closeConnection("Protocol error $e")
            return false
        }
        return true
    }

    private fun handleMessage(type: MobcamStreamMessageType, payload: ByteArray) {
        when (type) {
            MobcamStreamMessageType.HOST_HELLO -> handleHostHello(payload)
            else -> Log.i(TAG, "mobcam-stream: Ignoring message type $type")
        }
    }

    private fun handleHostHello(payload: ByteArray) {
        if (connectedAt != null) {
            return
        }
        unpackMobcamStreamHostHello(payload)
        connectedAt = System.nanoTime()
        val info = MobcamStreamDeviceInfo(name = deviceName, version = appVersion())
        send(packMobcamStreamDeviceHello(info))
        Log.i(TAG, "mobcam-stream: Host said hello")
        delegate?.mobcamStreamOnConnected()
        encoding = true
        delegate?.mobcamStreamStartEncoding(this)
    }

    private fun closeConnection(reason: String) {
        val connection = this.connection ?: return
        Log.i(TAG, "mobcam-stream: Closing connection. $reason.")
        if (encoding) {
            encoding = false
            delegate?.mobcamStreamStopEncoding(this)
        }
        receiveJob?.cancel()
        receiveJob = null
        writerChannel?.close()
        writerChannel = null
        writerJob?.cancel()
        writerJob = null
        runCatching { connection.close() }
        this.connection = null
        val wasConnected = connectedAt != null
        connectedAt = null
        acceptedAt = null
        congestedAt = null
        outstandingByteCount.set(0)
        dropUntilSync = false
        audioSupported = false
        if (wasConnected) {
            delegate?.mobcamStreamOnDisconnected(reason)
        }
    }

    private fun send(data: ByteArray) {
        if (this.connection == null) {
            return
        }
        val channel = writerChannel ?: return
        outstandingByteCount.addAndGet(data.size)
        bitrateStats.add(bytesTransferred = data.size)
        val result = channel.trySend(data)
        if (result.isFailure) {
            outstandingByteCount.addAndGet(-data.size)
        }
    }

    private fun isCongested(): Boolean {
        if (outstandingByteCount.get() <= congestionThresholeBytes) {
            return false
        }
        if (congestedAt == null) {
            congestedAt = System.nanoTime()
            Log.i(TAG, "mobcam-stream: Host is falling behind. Dropping video frames.")
        }
        return true
    }

    private fun toMicroseconds(presentationTimeUs: Long): ULong? {
        if (presentationTimeUs < 0) {
            return null
        }
        return presentationTimeUs.toULong()
    }

    private fun handleVideoEncoderOutputFormat(formatDescription: MediaFormat) {
        if (connectedAt == null) {
            return
        }
        val width = formatDescription.getInteger(MediaFormat.KEY_WIDTH)
        val height = formatDescription.getInteger(MediaFormat.KEY_HEIGHT)
        when (formatDescription.getString(MediaFormat.KEY_MIME)) {
            "video/avc" -> {
                val record = MpegTsVideoConfigAvc.getAvcC(formatDescription) ?: return
                send(
                    packMobcamStreamVideoConfig(
                        codec = MobcamStreamVideoCodec.H264,
                        width = width.toUShort(),
                        height = height.toUShort(),
                        configurationRecord = record,
                    )
                )
            }
            "video/hevc" -> {
                val record = MpegTsVideoConfigHevc.getHvcC(formatDescription) ?: return
                send(
                    packMobcamStreamVideoConfig(
                        codec = MobcamStreamVideoCodec.HEVC,
                        width = width.toUShort(),
                        height = height.toUShort(),
                        configurationRecord = record,
                    )
                )
            }
            else -> Log.i(TAG, "mobcam-stream: Unsupported video codec ${formatDescription.getString(MediaFormat.KEY_MIME)}")
        }
    }

    private fun handleVideoEncoderOutputSampleBuffer(sampleBuffer: MediaSample) {
        if (connectedAt == null) {
            return
        }
        val presentationTimeStamp = toMicroseconds(sampleBuffer.presentationTimeUs) ?: return
        val isSync = sampleBuffer.isKeyFrame
        if (isCongested()) {
            dropUntilSync = true
            return
        }
        if (dropUntilSync) {
            if (!isSync) {
                return
            }
            dropUntilSync = false
        }
        val buffer = sampleBuffer.data
        if (buffer.isEmpty()) {
            return
        }
        send(
            packMobcamStreamVideoFrame(
                presentationTimeStamp = presentationTimeStamp,
                isSync = isSync,
                units = buffer,
            )
        )
    }

    private fun handleAudioEncoderOutputFormat(format: MediaFormat) {
        if (connectedAt == null) {
            return
        }
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        audioSupported = true
        when (format.getString(MediaFormat.KEY_MIME)) {
            "audio/mp4a-latm" -> {
                val config = MpegTsAudioConfig(formatDescription = format)
                send(
                    packMobcamStreamAudioConfig(
                        codec = MobcamStreamAudioCodec.AAC,
                        sampleRate = sampleRate.toUInt(),
                        channels = channels.toUByte(),
                        configurationRecord = config.encode(),
                    )
                )
            }
            "audio/opus" -> {
                send(
                    packMobcamStreamAudioConfig(
                        codec = MobcamStreamAudioCodec.OPUS,
                        sampleRate = sampleRate.toUInt(),
                        channels = channels.toUByte(),
                        configurationRecord = packMobcamStreamOpusHead(
                            sampleRate = sampleRate.toUInt(),
                            channels = channels.toUByte(),
                        ),
                    )
                )
            }
            else -> {
                audioSupported = false
                Log.i(TAG, "mobcam-stream: Only AAC and Opus audio is supported. Streaming video only.")
            }
        }
    }

    private fun handleAudioEncoderOutputBuffer(buffer: ByteArray, presentationTimeUs: Long) {
        if (connectedAt == null || !audioSupported || buffer.isEmpty()) {
            return
        }
        val presentationTimeStamp = toMicroseconds(presentationTimeUs) ?: return
        send(
            packMobcamStreamAudioFrame(
                presentationTimeStamp = presentationTimeStamp,
                unit = buffer,
            )
        )
    }

    override fun audioEncoderOutputFormat(format: MediaFormat) {
        mobcamStreamScope.launch {
            handleAudioEncoderOutputFormat(format)
        }
    }

    override fun audioEncoderOutputBuffer(buffer: ByteArray, presentationTimeUs: Long) {
        mobcamStreamScope.launch {
            handleAudioEncoderOutputBuffer(buffer, presentationTimeUs)
        }
    }

    override fun videoEncoderOutputFormat(encoder: VideoEncoder, formatDescription: MediaFormat) {
        mobcamStreamScope.launch {
            handleVideoEncoderOutputFormat(formatDescription)
        }
    }

    override fun videoEncoderOutputSampleBuffer(
        encoder: VideoEncoder,
        sampleBuffer: MediaSample,
        presentationTimeUs: Long,
    ) {
        mobcamStreamScope.launch {
            handleVideoEncoderOutputSampleBuffer(sampleBuffer)
        }
    }
}

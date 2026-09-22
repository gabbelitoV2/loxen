package com.moblin.android.media.haishinkit.srt

import android.util.Log
import com.moblin.android.media.haishinkit.media.AudioVideoEncoderDelegate
import com.moblin.android.media.haishinkit.media.Processor
import com.moblin.android.media.haishinkit.media.processorControlQueue
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.mpeg.MpegTsWriter
import com.moblin.android.media.haishinkit.mpeg.MpegTsWriterDelegate
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import kotlinx.coroutines.launch
import com.moblin.android.media.haishinkit.codec.audio.AudioEncoderDelegate
import com.moblin.android.media.haishinkit.codec.video.VideoEncoderDelegate

private const val TAG = "SrtStreamOfficial"

private const val SRT_INVALID_SOCK = -1
private const val SRT_ERROR = -1
private const val SRTO_SNDDATA = 17
private const val SRT_ECONNREJ = 1003

private fun srt_startup() {
    TODO("srt_startup")
}

private fun srt_cleanup() {
    TODO("srt_cleanup")
}

private fun srt_close(socket: Int) {
    TODO("srt_close")
}

private fun srt_bstats(socket: Int, perf: CBytePerfMon, instant: Int) {
    TODO("srt_bstats")
}

private fun srt_getsockflag(socket: Int, option: Int, value: IntArray, size: IntArray): Int {
    TODO("srt_getsockflag")
}

private fun srt_create_socket(): Int {
    TODO("srt_create_socket")
}

private fun srt_send_callback(socket: Int, callback: (ByteArray?, Int, ByteArray?, Int) -> Int) {
    TODO("srt_send_callback")
}

private fun srt_connect(socket: Int, addr: InetSocketAddress): Int {
    TODO("srt_connect")
}

private fun srt_getlasterror_str(): String? {
    TODO("srt_getlasterror_str")
}

private fun srt_getlasterror(errno: Int?): Int {
    TODO("srt_getlasterror")
}

private fun srt_getrejectreason(socket: Int): Int {
    TODO("srt_getrejectreason")
}

private fun srt_rejectreason_str(reason: Int): String? {
    TODO("srt_rejectreason_str")
}

private fun srt_sendmsg2(socket: Int, buffer: ByteArray, length: Int): Int {
    TODO("srt_sendmsg2")
}

interface SrtStreamOfficialDelegate {
    fun srtStreamOfficialError()
}

private enum class ReadyState(val rawValue: UByte) {
    initialized(0u),
    publishing(1u);

    companion object {
        fun fromRawValue(rawValue: UByte): ReadyState? = entries.firstOrNull { it.rawValue == rawValue }
    }
}

private class SendHook(var closure: ((ByteArray) -> Boolean)? = null)

private class MpegTsWriterEncoderDelegate(
    private val writer: MpegTsWriter,
) : AudioVideoEncoderDelegate, AudioEncoderDelegate by writer, VideoEncoderDelegate by writer

open class SrtStreamOfficial(
    private val processor: Processor,
    timecodesEnabled: Boolean,
    delegate: SrtStreamOfficialDelegate,
) : MpegTsWriterDelegate {
    private val writer: MpegTsWriter = MpegTsWriter(timecodesEnabled = timecodesEnabled, newSrt = false)
    private val encoderDelegate: AudioVideoEncoderDelegate = MpegTsWriterEncoderDelegate(writer)
    private var sendHook = SendHook(null)
    private var options: MutableMap<SrtSocketOption, String> = mutableMapOf()
    private var perf = CBytePerfMon()
    private var socket: Int = SRT_INVALID_SOCK
    private val srtStreamDelegate: SrtStreamOfficialDelegate? = delegate
    private var readyState: ReadyState = ReadyState.initialized
        set(value) {
            if (field == value) {
                return
            }
            val oldValue = field
            field = value
            Log.i(TAG, "srt: State change $oldValue -> $value")
            when (oldValue) {
                ReadyState.publishing -> {
                    Log.i(TAG, "srt: Stop publishing")
                    processorPipelineQueue.launch {
                        writer.stopRunning()
                        processor.stopEncoding(encoderDelegate)
                    }
                }
                else -> Unit
            }
            when (value) {
                ReadyState.publishing -> {
                    Log.i(TAG, "srt: Start publishing")
                    processorPipelineQueue.launch {
                        processor.startEncoding(encoderDelegate)
                        writer.startRunning()
                    }
                }
                else -> Unit
            }
        }

    init {
        writer.delegate = this
        srt_startup()
    }

    protected fun finalize() {
        srt_cleanup()
    }

    @Throws(IOException::class)
    fun open(uri: URI?, sendHook: (ByteArray) -> Boolean) {
        if (uri == null || uri.scheme != "srt") {
            return
        }
        val host = uri.host ?: return
        val port = uri.port
        if (port == -1) {
            return
        }
        this.sendHook = SendHook(sendHook)
        socket = SRT_INVALID_SOCK
        val options = SrtSocketOption.from(uri.toString()).toMutableMap()
        options[SrtSocketOption.sndsyn] = "0"
        connect(sockaddrIn(host, port.coerceIn(0, 0xFFFF).toUShort()), options)
    }

    fun close() {
        processorControlQueue.launch {
            readyState = ReadyState.initialized
            if (socket == SRT_INVALID_SOCK) {
                return@launch
            }
            srt_close(socket)
            socket = SRT_INVALID_SOCK
        }
    }

    fun getPerformanceData(): SrtPerformanceData {
        if (socket == SRT_INVALID_SOCK) {
            return SrtPerformanceData.zero
        }
        srt_bstats(socket, perf, 1)
        return SrtPerformanceData(perf)
    }

    fun getSndData(): Int {
        if (socket == SRT_INVALID_SOCK) {
            return SRT_ERROR
        }
        val sndData = IntArray(1)
        val size = IntArray(1)
        size[0] = Int.SIZE_BYTES
        val result = srt_getsockflag(socket, SRTO_SNDDATA, sndData, size)
        if (result == SRT_ERROR) {
        }
        return sndData[0]
    }

    private fun sockaddrIn(host: String, port: UShort): InetSocketAddress {
        val address = runCatching { InetAddress.getByName(host) }.getOrNull()
        if (address is Inet4Address) {
            return InetSocketAddress(address, port.toInt())
        }
        return InetSocketAddress.createUnresolved(host, port.toInt())
    }

    private fun sendCallback(buf1: ByteArray?, size1: Int, buf2: ByteArray?, size2: Int): Int {
        if (buf1 == null || buf2 == null) {
            return -1
        }
        val data = ByteArray(size1 + size2)
        System.arraycopy(buf1, 0, data, 0, size1)
        System.arraycopy(buf2, 0, data, size1, size2)
        return if (sendHook.closure?.invoke(data) == true) {
            size1 + size2
        } else {
            -1
        }
    }

    @Throws(IOException::class)
    private fun connect(addr: InetSocketAddress, options: Map<SrtSocketOption, String>) {
        if (socket != SRT_INVALID_SOCK) {
            return
        }
        socket = srt_create_socket()
        if (socket == SRT_INVALID_SOCK) {
            throw IOException(makeSocketError())
        }
        srt_send_callback(socket, ::sendCallback)
        this.options = options.toMutableMap()
        if (!configure(SrtSocketOption.Binding.pre)) {
            throw IOException(makeSocketError())
        }
        val result = srt_connect(socket, addr)
        if (result == SRT_ERROR) {
            throw IOException(makeSocketError())
        }
        if (!configure(SrtSocketOption.Binding.post)) {
            throw IOException(makeSocketError())
        }
        readyState = ReadyState.publishing
    }

    private fun configure(binding: SrtSocketOption.Binding): Boolean {
        val failures = SrtSocketOption.configure(socket, binding, options)
        if (failures.isNotEmpty()) {
            Log.i(TAG, "srt: configure failures: $failures")
            return false
        }
        return true
    }

    private fun makeSocketError(): String {
        val lastError = srt_getlasterror_str()
        if (lastError == null) {
            return "Last error not set"
        }
        var message = lastError
        when (srt_getlasterror(null)) {
            SRT_ECONNREJ -> {
                val rejectReason = srt_rejectreason_str(srt_getrejectreason(socket))
                if (rejectReason != null) {
                    message += ": " + rejectReason
                }
            }
        }
        return message
    }

    override fun writer(writer: MpegTsWriter, doOutput: ByteArray, containsAudio: Boolean) {
        val sent = if (doOutput.isEmpty()) {
            Log.i(TAG, "srt: error buffer size ${doOutput.size}")
            SRT_ERROR
        } else {
            srt_sendmsg2(socket, doOutput, doOutput.size)
        }
        if (sent != doOutput.size) {
            processorControlQueue.launch {
                readyState = ReadyState.initialized
                srtStreamDelegate?.srtStreamOfficialError()
            }
        }
    }

    override fun writer(writer: MpegTsWriter, doOutputPointer: ByteArray, count: Int) {
        if (doOutputPointer.isEmpty()) {
            return
        }
        if (srt_sendmsg2(socket, doOutputPointer, count) != count) {
            processorControlQueue.launch {
                readyState = ReadyState.initialized
                srtStreamDelegate?.srtStreamOfficialError()
            }
        }
    }
}

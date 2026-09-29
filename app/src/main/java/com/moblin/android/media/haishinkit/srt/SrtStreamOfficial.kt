package com.moblin.android.media.haishinkit.srt

import com.moblin.android.platform.log.Log
import com.moblin.android.media.haishinkit.media.Processor
import com.moblin.android.media.haishinkit.media.processorControlQueue
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.mpeg.MpegTsWriter
import com.moblin.android.media.haishinkit.mpeg.MpegTsWriterDelegate
import com.moblin.android.platform.srt.SrtError
import com.moblin.android.platform.srt.SrtNative
import com.moblin.android.platform.srt.SrtSendHook
import java.io.IOException
import java.net.URI
import kotlinx.coroutines.launch

private const val TAG = "SrtStreamOfficial"

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

private class SendHook(var closure: ((ByteArray) -> Boolean)?)

open class SrtStreamOfficial(
    private val processor: Processor,
    timecodesEnabled: Boolean,
    delegate: SrtStreamOfficialDelegate,
) : MpegTsWriterDelegate {
    private val writer: MpegTsWriter = MpegTsWriter(timecodesEnabled = timecodesEnabled, newSrt = false)
    private var sendHook = SendHook(closure = null)
    private var options: Map<SrtSocketOption, String> = emptyMap()
    private var perf = CBytePerfMon()
    private var socket: Int = SrtNative.SRT_INVALID_SOCK
    private val srtStreamDelegate: SrtStreamOfficialDelegate? = delegate

    private var readyState: ReadyState = ReadyState.initialized
        set(value) {
            val oldValue = field
            field = value
            if (oldValue == value) {
                return
            }
            Log.i(TAG, "srt: State change $oldValue -> $value")
            when (oldValue) {
                ReadyState.publishing -> {
                    Log.i(TAG, "srt: Stop publishing")
                    processorPipelineQueue.launch {
                        writer.stopRunning()
                        processor.stopEncoding(writer)
                    }
                }
                else -> Unit
            }
            when (value) {
                ReadyState.publishing -> {
                    Log.i(TAG, "srt: Start publishing")
                    processorPipelineQueue.launch {
                        processor.startEncoding(writer)
                        writer.startRunning()
                    }
                }
                else -> Unit
            }
        }

    init {
        writer.delegate = this
        SrtNative.srt_startup()
    }

    protected fun finalize() {
        SrtNative.srt_cleanup()
    }

    @Throws(IOException::class)
    open fun open(uri: URI?, sendHook: (ByteArray) -> Boolean) {
        if (uri == null || uri.scheme != "srt") {
            return
        }
        val host = uri.host ?: return
        val port = uri.port
        if (port == -1) {
            return
        }
        this.sendHook = SendHook(closure = sendHook)
        socket = SrtNative.SRT_INVALID_SOCK
        val options = SrtSocketOption.from(uri = uri).toMutableMap()
        options[SrtSocketOption.sndsyn] = "0"
        connect(sockaddrIn(host, port = port.coerceIn(0, 0xFFFF)), options)
    }

    open fun close() {
        processorControlQueue.launch {
            readyState = ReadyState.initialized
            if (socket == SrtNative.SRT_INVALID_SOCK) {
                return@launch
            }
            SrtNative.srt_close(socket)
            socket = SrtNative.SRT_INVALID_SOCK
        }
    }

    open fun getPerformanceData(): SrtPerformanceData {
        if (socket == SrtNative.SRT_INVALID_SOCK) {
            return SrtPerformanceData.zero
        }
        SrtNative.srt_bstats(socket, perf, 1)
        return SrtPerformanceData(mon = perf)
    }

    open fun getSndData(): Int {
        if (socket == SrtNative.SRT_INVALID_SOCK) {
            return SrtNative.SRT_ERROR
        }
        val sndData = IntArray(1)
        val size = intArrayOf(Int.SIZE_BYTES)
        val result = SrtNative.srt_getsockflag(socket, SrtNative.SRTO_SNDDATA, sndData, size)
        if (result == SrtNative.SRT_ERROR) {
            Log.d(TAG, "srt: Failed to get snddata")
        }
        return sndData[0]
    }

    private fun sockaddrIn(host: String, port: Int): ByteArray {
        return SrtNative.sockaddrIn(host, port)
    }

    @Throws(IOException::class)
    private fun connect(addr: ByteArray, options: Map<SrtSocketOption, String>) {
        if (socket != SrtNative.SRT_INVALID_SOCK) {
            return
        }
        socket = SrtNative.srt_create_socket()
        if (socket == SrtNative.SRT_INVALID_SOCK) {
            throw SrtError(makeSocketError())
        }
        val context = sendHook
        SrtNative.srt_send_callback(socket, SrtSendHook { data ->
            context.closure?.invoke(data) ?: false
        })
        this.options = options
        if (!configure(SrtSocketOption.Binding.pre)) {
            throw SrtError(makeSocketError())
        }
        val result = SrtNative.srt_connect(socket, addr, addr.size)
        if (result == SrtNative.SRT_ERROR) {
            throw SrtError(makeSocketError())
        }
        if (!configure(SrtSocketOption.Binding.post)) {
            throw SrtError(makeSocketError())
        }
        readyState = ReadyState.publishing
    }

    private fun configure(binding: SrtSocketOption.Binding): Boolean {
        val failures = SrtSocketOption.configure(socket, binding = binding, options = options)
        if (failures.isNotEmpty()) {
            Log.i(TAG, "srt: configure failures: $failures")
            return false
        }
        return true
    }

    private fun makeSocketError(): String {
        val lastError = SrtNative.srt_getlasterror_str()
        var message = lastError
        when (SrtNative.srt_getlasterror(null)) {
            SrtNative.SRT_ECONNREJ -> {
                val rejectReason = SrtNative.srt_rejectreason_str(SrtNative.srt_getrejectreason(socket))
                message += ": " + rejectReason
            }
            else -> Unit
        }
        return message
    }

    override fun writer(writer: MpegTsWriter, doOutput: ByteArray, containsAudio: Boolean) {
        val sent = if (doOutput.isEmpty()) {
            Log.i(TAG, "srt: error buffer size ${doOutput.size}")
            SrtNative.SRT_ERROR
        } else {
            SrtNative.srt_sendmsg2(socket, doOutput, doOutput.size)
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
        if (SrtNative.srt_sendmsg2(socket, doOutputPointer, count) != count) {
            processorControlQueue.launch {
                readyState = ReadyState.initialized
                srtStreamDelegate?.srtStreamOfficialError()
            }
        }
    }
}

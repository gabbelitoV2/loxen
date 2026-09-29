package com.moblin.android.platform.network

import android.util.Log
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.SequenceInputStream
import java.net.Socket
import java.util.Collections
import java.util.WeakHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "OffMainSocket"
private const val maximumRequestSize = 64 * 1024
private const val requestTimeoutMillis = 10_000

object OffMainSocket {
    private val requests = Collections.synchronizedMap(WeakHashMap<Socket, ByteArray>())

    fun readRequest(
        socket: Socket,
        ioScope: CoroutineScope,
        mainScope: CoroutineScope,
        accepting: () -> Boolean,
        block: () -> Unit,
    ) {
        ioScope.launch {
            val request = try {
                readRequestHead(socket)
            } catch (error: IOException) {
                null
            }
            if (request == null) {
                runCatching { socket.close() }
                return@launch
            }
            requests[socket] = request
            mainScope.launch {
                if (accepting()) {
                    block()
                } else {
                    requests.remove(socket)
                    runCatching { socket.close() }
                }
            }
        }
    }

    fun input(socket: Socket): InputStream {
        val request = requests.remove(socket) ?: return socket.getInputStream()
        return SequenceInputStream(ByteArrayInputStream(request), socket.getInputStream())
    }

    fun output(socket: Socket): OutputStream = QueuedSocketOutputStream(socket)

    private fun readRequestHead(socket: Socket): ByteArray? {
        val input = socket.getInputStream()
        val head = ByteArrayOutputStream()
        socket.soTimeout = requestTimeoutMillis
        var lines = 0
        var lineLength = 0
        while (head.size() < maximumRequestSize) {
            val byte = input.read()
            if (byte < 0) {
                return null
            }
            head.write(byte)
            if (byte == '\n'.code) {
                if (lineLength == 0 && lines > 0) {
                    socket.soTimeout = 0
                    return head.toByteArray()
                }
                lines += 1
                lineLength = 0
            } else if (byte != '\r'.code) {
                lineLength += 1
            }
        }
        return null
    }
}

internal class QueuedSocketOutputStream(private val socket: Socket) : OutputStream() {
    private val output = socket.getOutputStream()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private val pending = ByteArrayOutputStream()

    @Volatile
    private var failed = false

    @Synchronized
    override fun write(b: Int) {
        pending.write(b)
    }

    @Synchronized
    override fun write(b: ByteArray, off: Int, len: Int) {
        pending.write(b, off, len)
    }

    @Synchronized
    override fun flush() {
        if (pending.size() == 0) {
            return
        }
        val data = pending.toByteArray()
        pending.reset()
        scope.launch { send(data) }
    }

    private fun send(data: ByteArray) {
        if (failed) {
            return
        }
        try {
            output.write(data)
            output.flush()
        } catch (error: Exception) {
            failed = true
            if (!socket.isClosed) {
                Log.i(TAG, "Send failed: ${error.message}")
                runCatching { socket.close() }
            }
        }
    }
}

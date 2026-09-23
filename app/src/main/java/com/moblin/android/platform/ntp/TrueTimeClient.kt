package com.moblin.android.platform.ntp

import android.os.SystemClock
import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ReferenceTime internal constructor(private val epochMillisAtElapsed: Long, private val elapsedAtSync: Long) {
    fun now(): Instant = Instant.ofEpochMilli(epochMillisAtElapsed + (SystemClock.elapsedRealtime() - elapsedAtSync))
}

class TrueTimeClient private constructor() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    @Volatile
    var referenceTime: ReferenceTime? = null
        private set

    fun start(pool: List<String>) {
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                for (host in pool.filter { it.isNotBlank() }) {
                    val result = runCatching { query(host) }
                    val time = result.getOrNull()
                    if (time != null) {
                        referenceTime = time
                        break
                    }
                    Log.i(TAG, "NTP query to $host failed: ${result.exceptionOrNull()}")
                }
                delay(if (referenceTime == null) 5_000 else 600_000)
            }
        }
    }

    fun pause() {
        job?.cancel()
        job = null
    }

    private fun query(host: String): ReferenceTime {
        DatagramSocket().use { socket ->
            socket.soTimeout = 3_000
            val request = ByteArray(48)
            request[0] = 0x1B
            val address = InetAddress.getByName(host)
            val sendElapsed = SystemClock.elapsedRealtime()
            val sendEpoch = System.currentTimeMillis()
            socket.send(DatagramPacket(request, request.size, address, 123))
            val response = ByteArray(48)
            val packet = DatagramPacket(response, response.size)
            socket.receive(packet)
            val receiveElapsed = SystemClock.elapsedRealtime()
            val receiveEpoch = sendEpoch + (receiveElapsed - sendElapsed)
            val serverReceive = readTimestamp(response, 32)
            val serverTransmit = readTimestamp(response, 40)
            val offset = ((serverReceive - sendEpoch) + (serverTransmit - receiveEpoch)) / 2
            return ReferenceTime(receiveEpoch + offset, receiveElapsed)
        }
    }

    private fun readTimestamp(buffer: ByteArray, offset: Int): Long {
        var seconds = 0L
        var fraction = 0L
        for (i in 0 until 4) {
            seconds = (seconds shl 8) or (buffer[offset + i].toLong() and 0xff)
            fraction = (fraction shl 8) or (buffer[offset + 4 + i].toLong() and 0xff)
        }
        return (seconds - NTP_EPOCH_OFFSET_SECONDS) * 1000 + (fraction * 1000 shr 32)
    }

    companion object {
        private const val TAG = "TrueTimeClient"
        private const val NTP_EPOCH_OFFSET_SECONDS = 2_208_988_800L
        val sharedInstance = TrueTimeClient()
    }
}

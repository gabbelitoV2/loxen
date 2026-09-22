package com.moblin.android.media.wifiaware

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "WiFiAwareSender"

private data class Receiver(
    val connection: DatagramSocket,
    val senderTask: Job,
    val receiverTask: Job,
)

class WiFiAwareSender private constructor() {
    companion object {
        val shared = WiFiAwareSender()
    }

    private val connections = ConcurrentHashMap<String, Receiver>()
    private val scope = CoroutineScope(Dispatchers.IO)

    suspend fun browse() {
        Log.i(TAG, "wifi-aware-sender: Start")
        browseWiFiAware { deviceId, connection ->
            connections[deviceId]?.senderTask?.cancel()
            connections[deviceId]?.receiverTask?.cancel()
            val senderTask = scope.launch {
                Log.i(TAG, "wifi-aware-sender: Sender task started")
                try {
                    while (isActive) {
                        Log.i(TAG, "wifi-aware-sender: Sending data")
                        val data = byteArrayOf(1, 2, 3, 4)
                        connection.send(DatagramPacket(data,
                                                       data.size,
                                                       connection.remoteSocketAddress))
                        delay(5_000)
                    }
                } catch (e: CancellationException) {
                    Log.i(TAG, "wifi-aware-sender: Sender task error: $e")
                    throw e
                } catch (e: Exception) {
                    Log.i(TAG, "wifi-aware-sender: Sender task error: $e")
                } finally {
                    if (connections[deviceId]?.connection === connection) {
                        Log.i(TAG, "wifi-aware-sender: Removing connection")
                        connections[deviceId]?.senderTask?.cancel()
                        connections[deviceId]?.receiverTask?.cancel()
                        connections.remove(deviceId)
                    }
                    Log.i(TAG, "wifi-aware-sender: Sender task stopped")
                }
            }
            val receiverTask = scope.launch {
                Log.i(TAG, "wifi-aware-sender: Receiver task started")
                while (isActive) {
                    val buffer = ByteArray(1500)
                    val packet = DatagramPacket(buffer, buffer.size)
                    val received = runCatching {
                        connection.receive(packet)
                        packet
                    }.getOrNull() ?: break
                    if (received.length == 0) {
                        break
                    }
                    Log.i(TAG, "wifi-aware-sender: Got data: " +
                        received.data.copyOf(received.length).toHexString())
                }
                Log.i(TAG, "wifi-aware-sender: Receiver task stopped")
            }
            connections[deviceId] = Receiver(connection = connection,
                                             senderTask = senderTask,
                                             receiverTask = receiverTask)
            Log.i(TAG, "wifi-aware-sender: Number of connections: ${connections.size}")
        }
        for (receiver in connections.values) {
            receiver.senderTask.cancel()
            receiver.receiverTask.cancel()
        }
        connections.clear()
        Log.i(TAG, "wifi-aware-sender: Stop")
    }

    private suspend fun browseWiFiAware(
        onEndpoint: (deviceId: String, connection: DatagramSocket) -> Unit,
    ) {
        Unit
    }
}

private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

package com.moblin.android.various.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.test.core.app.ApplicationProvider
import com.moblin.android.platform.network.NWInterface
import com.moblin.android.runMainTest
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.util.Collections
import kotlin.concurrent.thread
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNetwork

@RunWith(RobolectricTestRunner::class)
class HttpClientInterfaceTypeSuite {
    private lateinit var server: ServerSocket
    private val requestLines = Collections.synchronizedList(mutableListOf<String>())
    private val bodies = Collections.synchronizedList(mutableListOf<ByteArray>())
    private val requested = mutableListOf<NWInterface.InterfaceType>()
    private val seen = mutableSetOf<ConnectivityManager.NetworkCallback>()
    private val cellular: Network = ShadowNetwork.newInstance(301)
    private val wifi: Network = ShadowNetwork.newInstance(302)
    private var available = mapOf(NWInterface.InterfaceType.cellular to cellular, NWInterface.InterfaceType.wifi to wifi)

    private val connectivityManager: ConnectivityManager
        get() = ApplicationProvider.getApplicationContext<Context>().getSystemService(ConnectivityManager::class.java)

    @Before
    fun setUp() {
        server = ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"))
        thread(isDaemon = true) {
            while (!server.isClosed) {
                val socket = runCatching { server.accept() }.getOrNull() ?: break
                socket.use {
                    val input = it.getInputStream()
                    val head = readHead(input)
                    val lines = head.split("\r\n")
                    val length = lines.firstOrNull { line -> line.lowercase().startsWith("content-length:") }
                        ?.substringAfter(":")?.trim()?.toInt() ?: 0
                    val body = ByteArray(length)
                    var offset = 0
                    while (offset < length) {
                        val count = input.read(body, offset, length - offset)
                        if (count < 0) {
                            break
                        }
                        offset += count
                    }
                    requestLines.add(lines.first())
                    bodies.add(body)
                    it.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 2\r\n\r\nok".toByteArray())
                    it.getOutputStream().flush()
                }
            }
        }
        seen.addAll(shadowOf(connectivityManager).networkCallbacks)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun readHead(input: InputStream): String {
        val head = ByteArrayOutputStream()
        while (true) {
            val byte = input.read()
            if (byte < 0) {
                break
            }
            head.write(byte)
            if (head.toString(Charsets.UTF_8.name()).endsWith("\r\n\r\n")) {
                break
            }
        }
        return head.toString(Charsets.UTF_8.name()).removeSuffix("\r\n\r\n")
    }

    private fun callbackType(callback: ConnectivityManager.NetworkCallback): NWInterface.InterfaceType? {
        val field = callback.javaClass.declaredFields.firstOrNull { it.type == NWInterface.InterfaceType::class.java }
            ?: return null
        field.isAccessible = true
        return field.get(callback) as? NWInterface.InterfaceType
    }

    private fun CoroutineScope.provideNetworks(): Job {
        return launch {
            while (isActive) {
                for (callback in shadowOf(connectivityManager).networkCallbacks.toList()) {
                    if (!seen.add(callback)) {
                        continue
                    }
                    val type = callbackType(callback) ?: continue
                    requested.add(type)
                    available[type]?.let { callback.onAvailable(it) }
                }
                delay(5)
            }
        }
    }

    private suspend fun upload(body: ByteArray): ByteArray? {
        val request = Request.Builder()
            .url("http://127.0.0.1:${server.localPort}/upload?x=1")
            .post(ByteArray(0).toRequestBody())
            .header("Content-Type", "multipart/form-data; boundary=b")
            .build()
        val result = CompletableDeferred<ByteArray?>()
        httpCall(request = request, body = body) { data -> result.complete(data) }
        return withTimeout(20_000) { result.await() }
    }

    @Test
    fun triesCellularThenWifiAndRemembersTheInterfaceThatWorked() = runMainTest {
        val provider = provideNetworks()
        val body = "image".toByteArray()
        assertContentEquals("ok".toByteArray(), upload(body))
        assertEquals(listOf(NWInterface.InterfaceType.cellular), requested)
        assertEquals(1, shadowOf(cellular).boundSocketCount())
        assertEquals("POST /upload?x=1 HTTP/1.1", requestLines.single())
        assertContentEquals(body, bodies.single())
        available = mapOf(NWInterface.InterfaceType.wifi to wifi)
        assertContentEquals("ok".toByteArray(), upload(body))
        assertEquals(
            listOf(NWInterface.InterfaceType.cellular, NWInterface.InterfaceType.cellular, NWInterface.InterfaceType.wifi),
            requested,
        )
        assertEquals(1, shadowOf(wifi).boundSocketCount())
        available = mapOf(NWInterface.InterfaceType.cellular to cellular, NWInterface.InterfaceType.wifi to wifi)
        assertContentEquals("ok".toByteArray(), upload(body))
        assertEquals(NWInterface.InterfaceType.wifi, requested.last())
        assertEquals(4, requested.size)
        assertEquals(2, shadowOf(wifi).boundSocketCount())
        assertEquals(1, shadowOf(cellular).boundSocketCount())
        assertEquals(3, requestLines.size)
        provider.cancel()
    }
}

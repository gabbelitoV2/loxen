package com.moblin.android.media.haishinkit.rtmp

import com.moblin.android.common.various.makeRtmpStreamKey
import com.moblin.android.common.various.makeRtmpUri
import com.moblin.android.media.haishinkit.rtmp.amf.AsObject
import com.moblin.android.media.haishinkit.rtmp.amf.AsValue
import com.moblin.android.media.haishinkit.rtmp.message.RtmpCommandMessage
import com.moblin.android.media.haishinkit.rtmp.message.RtmpCommandName
import com.moblin.android.media.haishinkit.rtmp.message.RtmpMessageType
import kotlinx.coroutines.Dispatchers
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RtmpSuite {
    @Test
    fun twitchUrl() {
        val url = "rtmp://foo.com/app/live_asefwefwefwef"
        val streamUrl = makeRtmpUri(url)
        val streamKey = makeRtmpStreamKey(url)
        assertEquals("rtmp://foo.com/app", streamUrl)
        assertEquals("live_asefwefwefwef", streamKey)
    }

    @Test
    fun kickUrl() {
        val url = "rtmp://foo.com/foobar"
        val streamUrl = makeRtmpUri(url)
        val streamKey = makeRtmpStreamKey(url)
        assertEquals("rtmp://foo.com", streamUrl)
        assertEquals("foobar", streamKey)
    }

    @Test
    fun bilibiliUrl() {
        val url = "rtmp://foo.com/live/?foo=bar&a=b"
        val streamUrl = makeRtmpUri(url)
        val streamKey = makeRtmpStreamKey(url)
        assertEquals("rtmp://foo.com/live", streamUrl)
        assertEquals("?foo=bar&a=b", streamKey)
    }

    private fun makeConnectResultChunkData(): ByteArray {
        return RtmpChunk(
            message = RtmpCommandMessage(
                streamId = 0u,
                transactionId = 1,
                commandType = RtmpMessageType.amf0Command,
                commandName = RtmpCommandName.result,
                commandObject = null,
                arguments = mutableListOf(
                    AsValue.Object(
                        mutableMapOf(
                            "code" to AsValue.String("NetConnection.Connect.Success"),
                        ),
                    ),
                ),
            ),
        ).encode()
    }

    @Test
    fun connectResultInOneRead() {
        val connection = RtmpConnection(name = "test", queue = Dispatchers.Unconfined)
        var arguments: List<AsValue>? = null
        connection.callCompletions[1] = { arguments = it }
        assertTrue(connection.socketDataReceived(makeConnectResultChunkData()).isEmpty())
        assertEquals(1, arguments?.size)
    }

    @Test
    fun connectResultSplitAfterChunkHeader() {
        val connection = RtmpConnection(name = "test", queue = Dispatchers.Unconfined)
        var arguments: List<AsValue>? = null
        connection.callCompletions[1] = { arguments = it }
        val data = makeConnectResultChunkData()
        val buffer = connection.socketDataReceived(data.copyOfRange(0, 12))
        assertTrue(buffer.contentEquals(data.copyOfRange(0, 12)))
        assertTrue(connection.socketDataReceived(buffer + data.copyOfRange(12, data.size)).isEmpty())
        assertEquals(1, arguments?.size)
    }

    @Test
    fun twitcastingUrl() {
        val url = "rtmp://foo.com/live/g:3234234?key=1234"
        val streamUrl = makeRtmpUri(url)
        val streamKey = makeRtmpStreamKey(url)
        assertEquals("rtmp://foo.com/live", streamUrl)
        assertEquals("g:3234234?key=1234", streamKey)
    }
}

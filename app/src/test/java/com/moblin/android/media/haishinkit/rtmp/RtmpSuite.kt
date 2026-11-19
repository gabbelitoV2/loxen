package com.moblin.android.media.haishinkit.rtmp

import com.moblin.android.common.various.makeRtmpStreamKey
import com.moblin.android.common.various.makeRtmpUri
import com.moblin.android.media.haishinkit.media.Processor
import com.moblin.android.media.haishinkit.rtmp.amf.AsObject
import com.moblin.android.media.haishinkit.rtmp.amf.AsValue
import com.moblin.android.media.haishinkit.rtmp.RtmpCommandMessage
import com.moblin.android.media.haishinkit.rtmp.RtmpCommandName
import com.moblin.android.media.haishinkit.rtmp.RtmpMessageType
import com.moblin.android.various.settings.SettingsStreamColorRange
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
        val rtmpStream = makeRtmpStream()
        var arguments: List<AsValue>? = null
        rtmpStream.callCompletions[1] = { arguments = it }
        assertTrue(rtmpStream.socketDataReceived(rtmpStream.socket, makeConnectResultChunkData()).isEmpty())
        assertEquals(1, arguments?.size)
    }

    @Test
    fun connectResultSplitAfterChunkHeader() {
        val rtmpStream = makeRtmpStream()
        var arguments: List<AsValue>? = null
        rtmpStream.callCompletions[1] = { arguments = it }
        val data = makeConnectResultChunkData()
        val buffer = rtmpStream.socketDataReceived(rtmpStream.socket, data.copyOfRange(0, 12))
        assertTrue(buffer.contentEquals(data.copyOfRange(0, 12)))
        assertTrue(rtmpStream.socketDataReceived(rtmpStream.socket, buffer + data.copyOfRange(12, data.size))
            .isEmpty())
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

private fun makeRtmpStream(): RtmpStream {
    return makeRtmpStream(RtmpModelMock())
}

private fun makeRtmpStream(modelMock: RtmpModelMock): RtmpStream {
    return RtmpStream(
        name = "test",
        processor = Processor(delegate = modelMock, colorRange = SettingsStreamColorRange.full),
        delegate = modelMock,
        queue = Dispatchers.Unconfined,
    )
}

private fun makeCommandChunkData(
    transactionId: Int,
    commandName: RtmpCommandName,
    arguments: List<AsValue>,
): ByteArray {
    return RtmpChunk(
        message = RtmpCommandMessage(
            streamId = 0u,
            transactionId = transactionId,
            commandType = RtmpMessageType.amf0Command,
            commandName = commandName,
            commandObject = null,
            arguments = arguments,
        ),
    ).encode()
}

private fun makeStatusChunkData(code: String): ByteArray {
    return makeCommandChunkData(
        transactionId = 0,
        commandName = RtmpCommandName.onStatus,
        arguments = listOf(
            AsValue.Object(
                mapOf("code" to AsValue.String(code)),
            ),
        ),
    )
}

@RunWith(RobolectricTestRunner::class)
class RtmpStreamEventsSuite {
    @Test
    fun disconnectsAreReportedInOrderWhileReading() {
        val modelMock = RtmpModelMock()
        val rtmpStream = makeRtmpStream(modelMock)
        val data = makeStatusChunkData("NetConnection.Connect.Failed") +
            makeStatusChunkData("NetConnection.Connect.Closed")
        assertTrue(rtmpStream.socketDataReceived(rtmpStream.socket, data).isEmpty())
        assertEquals(
            listOf(RtmpDisconnectReason.connectFailed, RtmpDisconnectReason.serverClosed),
            modelMock.getDisconnectedReasons(),
        )
    }

    @Test
    fun disconnectFromCurrentSocketIsReported() {
        val modelMock = RtmpModelMock()
        val rtmpStream = makeRtmpStream(modelMock)
        rtmpStream.socketDisconnected(rtmpStream.socket, RtmpStatusCode.connectFailed)
        assertEquals(listOf(RtmpDisconnectReason.connectFailed), modelMock.getDisconnectedReasons())
    }

    @Test
    fun disconnectFromOldSocketIsIgnored() {
        val modelMock = RtmpModelMock()
        val rtmpStream = makeRtmpStream(modelMock)
        rtmpStream.socketDisconnected(
            RtmpSocket(name = "old", queue = Dispatchers.Unconfined),
            RtmpStatusCode.connectFailed,
        )
        assertTrue(modelMock.getDisconnectedReasons().isEmpty())
    }

    @Test
    fun dataFromOldSocketIsIgnored() {
        val modelMock = RtmpModelMock()
        val rtmpStream = makeRtmpStream(modelMock)
        rtmpStream.socketDataReceived(
            RtmpSocket(name = "old", queue = Dispatchers.Unconfined),
            makeStatusChunkData("NetConnection.Connect.Failed"),
        )
        assertTrue(modelMock.getDisconnectedReasons().isEmpty())
    }

    @Test
    fun messagesAfterCloseCommandAreIgnored() {
        val modelMock = RtmpModelMock()
        val rtmpStream = makeRtmpStream(modelMock)
        val data = makeCommandChunkData(transactionId = 0, commandName = RtmpCommandName.close, arguments = listOf()) +
            makeStatusChunkData("NetConnection.Connect.Failed")
        rtmpStream.socketDataReceived(rtmpStream.socket, data)
        assertTrue(modelMock.getDisconnectedReasons().isEmpty())
    }

    @Test
    fun messagesAfterConnectClosedStatusAreIgnored() {
        val modelMock = RtmpModelMock()
        val rtmpStream = makeRtmpStream(modelMock)
        val data = makeStatusChunkData("NetConnection.Connect.Closed") +
            makeStatusChunkData("NetConnection.Connect.Failed")
        rtmpStream.socketDataReceived(rtmpStream.socket, data)
        assertEquals(listOf(RtmpDisconnectReason.serverClosed), modelMock.getDisconnectedReasons())
    }
}

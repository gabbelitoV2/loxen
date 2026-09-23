package com.moblin.android.obs

import com.moblin.android.MessageQueue
import com.moblin.android.areEqual
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private sealed interface CompletionResult<out Value> {
    class Success<out Value>(val value: Value) : CompletionResult<Value>

    class Failure(val message: String) : CompletionResult<Nothing>
}

private class Completion<Value> {
    private val results = MessageQueue<CompletionResult<Value>>()

    fun onSuccess(value: Value) {
        results.put(CompletionResult.Success(value))
    }

    fun onError(message: String) {
        results.put(CompletionResult.Failure(message))
    }

    suspend fun get(): Value {
        val result = results.get()
        return when (result) {
            is CompletionResult.Success -> result.value
            is CompletionResult.Failure -> throw IllegalStateException(result.message)
        }
    }

    suspend fun getError(): String? {
        val result = results.get()
        return when (result) {
            is CompletionResult.Success -> null
            is CompletionResult.Failure -> result.message
        }
    }
}

private fun Completion<Unit>.onSuccess() {
    onSuccess(Unit)
}

private class Delegate : ObsWebsocketDelegate {
    val connected = MessageQueue<Unit>()
    val sceneChanges = MessageQueue<String>()
    val muteChanges = MessageQueue<Pair<String, Boolean>>()
    val streamStatuses = MessageQueue<Pair<Boolean, ObsOutputState?>>()
    val recordStatuses = MessageQueue<Pair<Boolean, ObsOutputState?>>()
    val audioVolumes = MessageQueue<List<Pair<String, List<Float>>>>()

    override fun obsWebsocketConnected() {
        connected.put(Unit)
    }

    override fun obsWebsocketSceneChanged(sceneName: String) {
        sceneChanges.put(sceneName)
    }

    override fun obsWebsocketInputMuteStateChangedEvent(inputName: String, muted: Boolean) {
        muteChanges.put(inputName to muted)
    }

    override fun obsWebsocketStreamStatusChanged(active: Boolean, state: ObsOutputState?) {
        streamStatuses.put(active to state)
    }

    override fun obsWebsocketRecordStatusChanged(active: Boolean, state: ObsOutputState?) {
        recordStatuses.put(active to state)
    }

    override fun obsWebsocketAudioVolume(volumes: List<ObsAudioInputVolume>) {
        audioVolumes.put(volumes.map { it.name to it.volumes })
    }
}

private class Connection(
    val server: ObsWebSocketServerMock,
    val delegate: Delegate,
    val obs: ObsWebSocket
) {
    companion object {
        suspend fun make(serverPassword: String? = null, clientPassword: String = ""): Connection {
            val server = ObsWebSocketServerMock(serverPassword)
            val delegate = Delegate()
            val obs = ObsWebSocket(server.url(), clientPassword, delegate)
            obs.start()
            return Connection(server, delegate, obs)
        }

        suspend fun makeConnected(): Connection {
            val connection = make()
            connection.server.connect()
            connection.delegate.connected.get()
            return connection
        }
    }

    suspend fun expectRequest(type: String, data: String? = null): ObsMockRequest {
        val request = server.receiveRequest()
        assertEquals(type, request.type)
        assertEquals(data, request.data)
        return request
    }
}

@RunWith(RobolectricTestRunner::class)
class ObsWebSocketSuite {
    @Test
    fun connectWithoutAuthentication() = runBlocking<Unit> {
        val connection = Connection.make()
        assertFalse(connection.obs.isConnected())
        connection.server.acceptConnection()
        connection.server.sendHello()
        val identify = connection.server.receiveIdentify()
        assertEquals(1, identify.rpcVersion)
        assertNull(identify.authentication)
        assertFalse(connection.obs.isConnected())
        connection.server.sendIdentified()
        connection.delegate.connected.get()
        assertTrue(connection.obs.isConnected())
        connection.obs.stop()
        assertFalse(connection.obs.isConnected())
    }

    @Test
    fun connectWithAuthentication() = runBlocking<Unit> {
        val connection = Connection.make(serverPassword = "secret", clientPassword = "secret")
        connection.server.acceptConnection()
        connection.server.sendHello()
        val identify = connection.server.receiveIdentify()
        assertEquals("0xGVPQbDiHRgqezuAY0t8iJy5bjIOgG42ebJNd0yDps=", identify.authentication)
        assertEquals(connection.server.expectedAuthentication(), identify.authentication)
        connection.server.sendIdentified()
        connection.delegate.connected.get()
        assertTrue(connection.obs.isConnected())
        connection.obs.stop()
    }

    @Test
    fun wrongPassword() = runBlocking<Unit> {
        val connection = Connection.make(serverPassword = "secret", clientPassword = "wrong")
        connection.server.acceptConnection()
        connection.server.sendHello()
        val identify = connection.server.receiveIdentify()
        assertNotNull(identify.authentication)
        assertNotEquals(connection.server.expectedAuthentication(), identify.authentication)
        connection.server.close(code = 4009.toUShort())
        connection.server.acceptConnection()
        assertFalse(connection.obs.isConnected())
        assertEquals("Disconnected", connection.obs.connectionErrorMessage)
        connection.obs.stop()
    }

    @Test
    fun reconnectAfterServerDisconnect() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        connection.server.disconnect()
        connection.server.connect()
        connection.delegate.connected.get()
        assertTrue(connection.obs.isConnected())
        assertEquals("Disconnected", connection.obs.connectionErrorMessage)
        connection.obs.stop()
    }

    @Test
    fun restart() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        connection.obs.stop()
        connection.obs.start()
        connection.server.connect()
        connection.delegate.connected.get()
        assertTrue(connection.obs.isConnected())
        connection.obs.stop()
    }

    @Test
    fun requestWhenNotConnected() = runBlocking<Unit> {
        val connection = Connection.make()
        val completion = Completion<ObsSceneList>()
        connection.obs.getSceneList(onSuccess = completion::onSuccess, onError = completion::onError)
        assertEquals("Not connected to server", completion.getError())
        connection.obs.stop()
    }

    @Test
    fun getSceneList() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        val completion = Completion<ObsSceneList>()
        connection.obs.getSceneList(onSuccess = completion::onSuccess, onError = completion::onError)
        val request = connection.expectRequest(type = "GetSceneList")
        assertEquals("1", request.id)
        connection.server.respond(
            request,
            data = """{"currentProgramSceneName":"Main","currentPreviewSceneName":null,"scenes":[{"sceneName":"Camera","sceneIndex":0,"sceneUuid":"a"},{"sceneName":"Main","sceneIndex":1,"sceneUuid":"b"}]}"""
        )
        val sceneList = completion.get()
        assertEquals("Main", sceneList.current)
        assertEquals(listOf("Main", "Camera"), sceneList.scenes)
        connection.obs.stop()
    }

    @Test
    fun requestIdsIncrement() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        val first = Completion<ObsSceneList>()
        val second = Completion<ObsSceneList>()
        connection.obs.getSceneList(onSuccess = first::onSuccess, onError = first::onError)
        connection.obs.getSceneList(onSuccess = second::onSuccess, onError = second::onError)
        val firstRequest = connection.expectRequest(type = "GetSceneList")
        val secondRequest = connection.expectRequest(type = "GetSceneList")
        assertEquals("1", firstRequest.id)
        assertEquals("2", secondRequest.id)
        connection.server.respond(secondRequest, data = """{"currentProgramSceneName":"B","scenes":[]}""")
        connection.server.respond(firstRequest, data = """{"currentProgramSceneName":"A","scenes":[]}""")
        assertEquals("A", first.get().current)
        assertEquals("B", second.get().current)
        connection.obs.stop()
    }

    @Test
    fun requestErrorWithComment() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        val completion = Completion<ObsSceneList>()
        connection.obs.getSceneList(onSuccess = completion::onSuccess, onError = completion::onError)
        val request = connection.expectRequest(type = "GetSceneList")
        connection.server.respond(request, errorCode = 600, comment = "No scenes")
        assertEquals("Operation failed with resourceNotFound (No scenes)", completion.getError())
        connection.obs.stop()
    }

    @Test
    fun requestErrorWithoutComment() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        val completion = Completion<ObsSceneList>()
        connection.obs.getSceneList(onSuccess = completion::onSuccess, onError = completion::onError)
        val request = connection.expectRequest(type = "GetSceneList")
        connection.server.respond(request, errorCode = 205)
        assertEquals("Operation failed with genericError", completion.getError())
        connection.obs.stop()
    }

    @Test
    fun requestErrorWithUnknownCode() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        val completion = Completion<ObsSceneList>()
        connection.obs.getSceneList(onSuccess = completion::onSuccess, onError = completion::onError)
        val request = connection.expectRequest(type = "GetSceneList")
        connection.server.respond(request, errorCode = 999)
        assertEquals("Operation failed with unknown", completion.getError())
        connection.obs.stop()
    }

    @Test
    fun responseDataMissing() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        val completion = Completion<ObsSceneList>()
        connection.obs.getSceneList(onSuccess = completion::onSuccess, onError = completion::onError)
        val request = connection.expectRequest(type = "GetSceneList")
        connection.server.respond(request)
        assertEquals("Response data missing", completion.getError())
        connection.obs.stop()
    }

    @Test
    fun responseDataMalformed() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        val completion = Completion<ObsSceneList>()
        connection.obs.getSceneList(onSuccess = completion::onSuccess, onError = completion::onError)
        val request = connection.expectRequest(type = "GetSceneList")
        connection.server.respond(request, data = """{"scenes":[]}""")
        assertEquals("JSON decode failed", completion.getError())
        connection.obs.stop()
    }

    @Test
    fun getSceneItemList() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        val completion = Completion<List<GetSceneItemListItem>>()
        connection.obs.getSceneItemList(
            sceneName = "Main",
            onSuccess = completion::onSuccess,
            onError = completion::onError
        )
        val request = connection.expectRequest(
            type = "GetSceneItemList",
            data = """{"sceneName":"Main"}"""
        )
        connection.server.respond(
            request,
            data = """{"sceneItems":[{"sourceName":"Group","inputKind":null,"isGroup":true,"sceneItemEnabled":true,"sceneItemId":1},{"sourceName":"Mic","inputKind":"coreaudio_input_capture","isGroup":null,"sceneItemEnabled":false,"sceneItemId":2}]}"""
        )
        val items = completion.get()
        assertEquals(2, items.size)
        assertEquals("Group", items[0].sourceName)
        assertNull(items[0].inputKind)
        assertTrue(items[0].sceneItemEnabled)
        assertEquals("Mic", items[1].sourceName)
        assertEquals("coreaudio_input_capture", items[1].inputKind)
        assertFalse(items[1].sceneItemEnabled)
        connection.obs.stop()
    }

    @Test
    fun getSpecialInputs() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        val completion = Completion<GetSpecialInputsResponse>()
        connection.obs.getSpecialInputs(onSuccess = completion::onSuccess, onError = completion::onError)
        val request = connection.expectRequest(type = "GetSpecialInputs")
        connection.server.respond(
            request,
            data = """{"desktop1":"Desktop Audio","desktop2":null,"mic1":"Mic/Aux","mic2":null,"mic3":"Headset","mic4":null}"""
        )
        assertEquals(listOf("Mic/Aux", "Headset"), completion.get().mics())
        connection.obs.stop()
    }

    @Test
    fun getInputList() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        val completion = Completion<List<String>>()
        connection.obs.getInputList(onSuccess = completion::onSuccess, onError = completion::onError)
        val request = connection.expectRequest(type = "GetInputList")
        connection.server.respond(
            request,
            data = """{"inputs":[{"inputName":"Mic","inputKind":"coreaudio_input_capture","inputUuid":"a"},{"inputName":"Media","inputKind":"ffmpeg_source","inputUuid":"b"}]}"""
        )
        assertEquals(listOf("Mic", "Media"), completion.get())
        connection.obs.stop()
    }

    @Test
    fun setCurrentProgramScene() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        val completion = Completion<Unit>()
        connection.obs.setCurrentProgramScene(
            name = "Camera",
            onSuccess = { completion.onSuccess() },
            onError = completion::onError
        )
        val request = connection.expectRequest(
            type = "SetCurrentProgramScene",
            data = """{"sceneName":"Camera"}"""
        )
        connection.server.respond(request)
        completion.get()
        connection.obs.stop()
    }

    @Test
    fun setMediaSourceSettings() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        val completion = Completion<Unit>()
        connection.obs.setMediaSourceSettings(
            name = "Media",
            input = "srt://127.0.0.1:9000",
            onSuccess = { completion.onSuccess() },
            onError = completion::onError
        )
        val request = connection.expectRequest(
            type = "SetInputSettings",
            data = """{"inputName":"Media","inputSettings":{"input":"srt://127.0.0.1:9000"}}"""
        )
        connection.server.respond(request)
        completion.get()
        connection.obs.stop()
    }

    @Test
    fun setInputSettings() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        val completion = Completion<Unit>()
        connection.obs.setInputSettings(
            inputName = "Media",
            onSuccess = { completion.onSuccess() },
            onError = completion::onError
        )
        val request = connection.expectRequest(
            type = "SetInputSettings",
            data = """{"inputName":"Media","inputSettings":{}}"""
        )
        connection.server.respond(request)
        completion.get()
        connection.obs.stop()
    }

    @Test
    fun getStreamStatus() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        val completion = Completion<ObsStreamStatus>()
        connection.obs.getStreamStatus(onSuccess = completion::onSuccess, onError = completion::onError)
        val request = connection.expectRequest(type = "GetStreamStatus")
        connection.server.respond(
            request,
            data = """{"outputActive":true,"outputReconnecting":false,"outputTimecode":"00:01:00.000","outputDuration":60000,"outputCongestion":0,"outputBytes":1000,"outputSkippedFrames":0,"outputTotalFrames":1800}"""
        )
        assertTrue(completion.get().active)
        connection.obs.stop()
    }

    @Test
    fun getRecordStatus() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        val completion = Completion<ObsRecordStatus>()
        connection.obs.getRecordStatus(onSuccess = completion::onSuccess, onError = completion::onError)
        val request = connection.expectRequest(type = "GetRecordStatus")
        connection.server.respond(
            request,
            data = """{"outputActive":false,"outputPaused":false,"outputTimecode":"00:00:00.000","outputDuration":0,"outputBytes":0}"""
        )
        assertFalse(completion.get().active)
        connection.obs.stop()
    }

    @Test
    fun startStream() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        val completion = Completion<Unit>()
        connection.obs.startStream(onSuccess = { completion.onSuccess() }, onError = completion::onError)
        val request = connection.expectRequest(type = "StartStream")
        connection.server.respond(request)
        completion.get()
        connection.obs.stop()
    }

    @Test
    fun startStreamAlreadyStreaming() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        val completion = Completion<Unit>()
        connection.obs.startStream(onSuccess = { completion.onSuccess() }, onError = completion::onError)
        val request = connection.expectRequest(type = "StartStream")
        connection.server.respond(request, errorCode = 500, comment = "Output is running")
        assertEquals("Already streaming", completion.getError())
        connection.obs.stop()
    }

    @Test
    fun startStreamOtherError() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        val completion = Completion<Unit>()
        connection.obs.startStream(onSuccess = { completion.onSuccess() }, onError = completion::onError)
        val request = connection.expectRequest(type = "StartStream")
        connection.server.respond(request, errorCode = 207)
        assertEquals("Operation failed with notReady", completion.getError())
        connection.obs.stop()
    }

    @Test
    fun stopStream() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        val completion = Completion<Unit>()
        connection.obs.stopStream(onSuccess = { completion.onSuccess() }, onError = completion::onError)
        val request = connection.expectRequest(type = "StopStream")
        connection.server.respond(request)
        completion.get()
        connection.obs.stop()
    }

    @Test
    fun stopStreamNotStreaming() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        val completion = Completion<Unit>()
        connection.obs.stopStream(onSuccess = { completion.onSuccess() }, onError = completion::onError)
        val request = connection.expectRequest(type = "StopStream")
        connection.server.respond(request, errorCode = 501)
        assertEquals("Not streaming", completion.getError())
        connection.obs.stop()
    }

    @Test
    fun startRecord() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        val completion = Completion<Unit>()
        connection.obs.startRecord(onSuccess = { completion.onSuccess() }, onError = completion::onError)
        val request = connection.expectRequest(type = "StartRecord")
        connection.server.respond(request)
        completion.get()
        connection.obs.stop()
    }

    @Test
    fun startRecordAlreadyRecording() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        val completion = Completion<Unit>()
        connection.obs.startRecord(onSuccess = { completion.onSuccess() }, onError = completion::onError)
        val request = connection.expectRequest(type = "StartRecord")
        connection.server.respond(request, errorCode = 500)
        assertEquals("Already recording", completion.getError())
        connection.obs.stop()
    }

    @Test
    fun stopRecord() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        val completion = Completion<Unit>()
        connection.obs.stopRecord(onSuccess = { completion.onSuccess() }, onError = completion::onError)
        val request = connection.expectRequest(type = "StopRecord")
        connection.server.respond(request)
        completion.get()
        connection.obs.stop()
    }

    @Test
    fun stopRecordNotRecording() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        val completion = Completion<Unit>()
        connection.obs.stopRecord(onSuccess = { completion.onSuccess() }, onError = completion::onError)
        val request = connection.expectRequest(type = "StopRecord")
        connection.server.respond(request, errorCode = 501)
        assertEquals("Not recording", completion.getError())
        connection.obs.stop()
    }

    @Test
    fun getSourceScreenshot() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        val completion = Completion<ByteArray>()
        connection.obs.getSourceScreenshot(
            name = "Main",
            onSuccess = completion::onSuccess,
            onError = completion::onError
        )
        val request = connection.expectRequest(
            type = "GetSourceScreenshot",
            data = """{"imageCompressionQuality":30,"imageFormat":"jpg","imageWidth":640,"sourceName":"Main"}"""
        )
        connection.server.respond(request, data = """{"imageData":"data:image/jpg;base64,SlBFRw=="}""")
        assertContentEquals("JPEG".toByteArray(), completion.get())
        connection.obs.stop()
    }

    @Test
    fun getSourceScreenshotBadBase64() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        val completion = Completion<ByteArray>()
        connection.obs.getSourceScreenshot(
            name = "Main",
            onSuccess = completion::onSuccess,
            onError = completion::onError
        )
        val request = connection.server.receiveRequest()
        connection.server.respond(request, data = """{"imageData":"data:image/jpg;base64,!!!"}""")
        assertEquals("Base64 decode failed", completion.getError())
        connection.obs.stop()
    }

    @Test
    fun setInputAudioSyncOffset() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        val completion = Completion<Unit>()
        connection.obs.setInputAudioSyncOffset(
            name = "Mic",
            offsetInMs = -250,
            onSuccess = { completion.onSuccess() },
            onError = completion::onError
        )
        val request = connection.expectRequest(
            type = "SetInputAudioSyncOffset",
            data = """{"inputAudioSyncOffset":-250,"inputName":"Mic"}"""
        )
        connection.server.respond(request)
        completion.get()
        connection.obs.stop()
    }

    @Test
    fun getInputAudioSyncOffset() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        val completion = Completion<Int>()
        connection.obs.getInputAudioSyncOffset(
            name = "Mic",
            onSuccess = completion::onSuccess,
            onError = completion::onError
        )
        val request = connection.expectRequest(
            type = "GetInputAudioSyncOffset",
            data = """{"inputName":"Mic"}"""
        )
        connection.server.respond(request, data = """{"inputAudioSyncOffset":150}""")
        assertEquals(150, completion.get())
        connection.obs.stop()
    }

    @Test
    fun setInputMute() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        val completion = Completion<Unit>()
        connection.obs.setInputMute(
            inputName = "Mic",
            muted = true,
            onSuccess = { completion.onSuccess() },
            onError = completion::onError
        )
        val request = connection.expectRequest(
            type = "SetInputMute",
            data = """{"inputMuted":true,"inputName":"Mic"}"""
        )
        connection.server.respond(request)
        completion.get()
        connection.obs.stop()
    }

    @Test
    fun getInputMuteBatch() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        val completion = Completion<List<Boolean?>>()
        connection.obs.getInputMuteBatch(
            inputNames = listOf("Mic", "Missing", "Desktop"),
            onSuccess = completion::onSuccess,
            onError = completion::onError
        )
        val batch = connection.server.receiveRequestBatch()
        assertEquals("4", batch.id)
        assertEquals(listOf("GetInputMute", "GetInputMute", "GetInputMute"), batch.requests.map { it.type })
        assertEquals(listOf("1", "2", "3"), batch.requests.map { it.id })
        assertEquals(
            listOf(
                """{"inputName":"Mic"}""",
                """{"inputName":"Missing"}""",
                """{"inputName":"Desktop"}"""
            ),
            batch.requests.map { it.data }
        )
        connection.server.respond(
            batch,
            results = listOf(
                ObsMockResult.Success(data = """{"inputMuted":true}"""),
                ObsMockResult.Failure(code = 600, comment = "No source was found by the name of `Missing`."),
                ObsMockResult.Success(data = """{"inputMuted":false}""")
            )
        )
        assertEquals(listOf(true, null, false), completion.get())
        connection.obs.stop()
    }

    @Test
    fun getInputMuteBatchMalformedResult() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        val completion = Completion<List<Boolean?>>()
        connection.obs.getInputMuteBatch(
            inputNames = listOf("Mic"),
            onSuccess = completion::onSuccess,
            onError = completion::onError
        )
        val batch = connection.server.receiveRequestBatch()
        connection.server.respond(batch, results = listOf(ObsMockResult.Success(data = """{"muted":true}""")))
        assertEquals(listOf<Boolean?>(null), completion.get())
        connection.obs.stop()
    }

    @Test
    fun getMediaSourcesSettingsBatch() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        val completion = Completion<List<Pair<String, Boolean>?>>()
        connection.obs.getMediaSourcesSettingsBatch(
            inputNames = listOf("Remote", "Local", "Missing"),
            onSuccess = { settings ->
                completion.onSuccess(settings.map { it?.let { source -> source.input to source.isLocalFile } })
            },
            onError = completion::onError
        )
        val batch = connection.server.receiveRequestBatch()
        assertEquals(
            listOf("GetInputSettings", "GetInputSettings", "GetInputSettings"),
            batch.requests.map { it.type }
        )
        assertEquals(
            listOf(
                """{"inputName":"Remote"}""",
                """{"inputName":"Local"}""",
                """{"inputName":"Missing"}"""
            ),
            batch.requests.map { it.data }
        )
        connection.server.respond(
            batch,
            results = listOf(
                ObsMockResult.Success(
                    data = """{"inputKind":"ffmpeg_source","inputSettings":{"input":"srt://example.com:9000","is_local_file":false}}"""
                ),
                ObsMockResult.Success(
                    data = """{"inputKind":"ffmpeg_source","inputSettings":{"input":"","local_file":"/tmp/a.mp4","is_local_file":true}}"""
                ),
                ObsMockResult.Failure(code = 600)
            )
        )
        val settings = completion.get()
        assertEquals(3, settings.size)
        assertEquals("srt://example.com:9000", settings[0]?.first)
        assertEquals(false, settings[0]?.second)
        assertEquals("", settings[1]?.first)
        assertEquals(true, settings[1]?.second)
        assertNull(settings[2])
        connection.obs.stop()
    }

    @Test
    fun audioVolumeSubscription() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        connection.obs.startAudioVolume()
        assertEquals(0x107FF, connection.server.receiveReidentify().toInt())
        connection.obs.stopAudioVolume()
        assertEquals(0x7FF, connection.server.receiveReidentify().toInt())
        connection.obs.stop()
    }

    @Test
    fun sceneChangedEvent() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        connection.server.sendEvent(
            type = "CurrentProgramSceneChanged",
            intent = 4,
            data = """{"sceneName":"Camera","sceneUuid":"a"}"""
        )
        assertEquals("Camera", connection.delegate.sceneChanges.get())
        connection.obs.stop()
    }

    @Test
    fun streamStateChangedEvents() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        for ((outputActive, outputState, expected) in listOf(
            Triple(false, "OBS_WEBSOCKET_OUTPUT_STARTING", ObsOutputState.starting),
            Triple(true, "OBS_WEBSOCKET_OUTPUT_STARTED", ObsOutputState.started),
            Triple(true, "OBS_WEBSOCKET_OUTPUT_RECONNECTING", ObsOutputState.stopped),
            Triple(true, "OBS_WEBSOCKET_OUTPUT_RECONNECTED", ObsOutputState.stopped),
            Triple(true, "OBS_WEBSOCKET_OUTPUT_STOPPING", ObsOutputState.stopping),
            Triple(false, "OBS_WEBSOCKET_OUTPUT_STOPPED", ObsOutputState.stopped)
        )) {
            connection.server.sendEvent(
                type = "StreamStateChanged",
                intent = 64,
                data = """{"outputActive":$outputActive,"outputState":"$outputState"}"""
            )
            val (active, state) = connection.delegate.streamStatuses.get()
            assertEquals(outputActive, active)
            assertEquals(expected, state)
        }
        connection.obs.stop()
    }

    @Test
    fun recordStateChangedEvents() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        for ((outputActive, outputState, expected) in listOf(
            Triple(false, "OBS_WEBSOCKET_OUTPUT_STARTING", ObsOutputState.starting),
            Triple(true, "OBS_WEBSOCKET_OUTPUT_STARTED", ObsOutputState.started),
            Triple(true, "OBS_WEBSOCKET_OUTPUT_PAUSED", ObsOutputState.started),
            Triple(true, "OBS_WEBSOCKET_OUTPUT_RESUMED", ObsOutputState.started),
            Triple(true, "OBS_WEBSOCKET_OUTPUT_STOPPING", ObsOutputState.stopping),
            Triple(false, "OBS_WEBSOCKET_OUTPUT_STOPPED", ObsOutputState.stopped)
        )) {
            connection.server.sendEvent(
                type = "RecordStateChanged",
                intent = 64,
                data = """{"outputActive":$outputActive,"outputState":"$outputState","outputPath":null}"""
            )
            val (active, state) = connection.delegate.recordStatuses.get()
            assertEquals(outputActive, active)
            assertEquals(expected, state)
        }
        connection.obs.stop()
    }

    @Test
    fun inputMuteStateChangedEvent() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        connection.server.sendEvent(
            type = "InputMuteStateChanged",
            intent = 8,
            data = """{"inputName":"Mic","inputUuid":"a","inputMuted":true}"""
        )
        val (inputName, muted) = connection.delegate.muteChanges.get()
        assertEquals("Mic", inputName)
        assertTrue(muted)
        connection.obs.stop()
    }

    @Test
    fun inputVolumeMetersEvent() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        connection.server.sendEvent(
            type = "InputVolumeMeters",
            intent = 65536,
            data = """{"inputs":[{"inputName":"Mic","inputUuid":"a","inputLevelsMul":[[1.0,1.0,1.0],[0.1,0.2,0.3]]},{"inputName":"Silent","inputUuid":"b","inputLevelsMul":[]},{"inputName":"Odd","inputUuid":"c","inputLevelsMul":[[],[0.5,0.5,0.5]]}]}"""
        )
        val volumes = connection.delegate.audioVolumes.get()
        assertEquals(3, volumes.size)
        assertEquals("Mic", volumes[0].first)
        assertTrue(areEqual(volumes[0].second.toFloatArray(), floatArrayOf(0f, -20f), epsilon = 0.001f))
        assertEquals("Silent", volumes[1].first)
        assertEquals(emptyList<Float>(), volumes[1].second)
        assertEquals("Odd", volumes[2].first)
        assertTrue(areEqual(volumes[2].second.toFloatArray(), floatArrayOf(-6.0206f), epsilon = 0.001f))
        connection.obs.stop()
    }

    @Test
    fun ignoresUnexpectedMessages() = runBlocking<Unit> {
        val connection = Connection.makeConnected()
        connection.server.send(text = "not json")
        connection.server.send(text = "[1, 2, 3]")
        connection.server.send(text = """{"op":"x","d":{}}""")
        connection.server.send(text = """{"op":6}""")
        connection.server.send(op = 42, data = "{}")
        connection.server.send(op = 8, data = """{"requestId":1,"requests":[]}""")
        connection.server.sendEvent(type = "SceneCreated", intent = 4, data = """{"sceneName":"New"}""")
        connection.server.sendEvent(type = "CurrentProgramSceneChanged", intent = 4, data = """{"name":"Bad"}""")
        connection.server.sendEvent(type = "CurrentProgramSceneChanged", intent = 4)
        connection.server.sendEvent(type = "StreamStateChanged", intent = 64, data = "{}")
        connection.server.sendEvent(type = "InputVolumeMeters", intent = 65536, data = """{"inputs":{}}""")
        connection.server.respond(ObsMockRequest(type = "GetSceneList", id = "99", data = null), data = "{}")
        connection.server.respond(ObsMockRequestBatch(id = "99", requests = emptyList()), results = emptyList())
        connection.server.sendEvent(
            type = "CurrentProgramSceneChanged",
            intent = 4,
            data = """{"sceneName":"Good"}"""
        )
        assertEquals("Good", connection.delegate.sceneChanges.get())
        assertTrue(connection.obs.isConnected())
        connection.obs.stop()
    }
}

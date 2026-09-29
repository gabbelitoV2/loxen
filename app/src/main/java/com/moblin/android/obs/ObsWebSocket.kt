package com.moblin.android.obs

import android.content.Context
import com.moblin.android.platform.log.Log
import com.moblin.android.localized
import com.moblin.android.various.network.WebSocketClient
import com.moblin.android.various.network.WebSocketClientDelegate
import java.net.URI
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID
import kotlin.math.log10
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.json.JSONArray
import org.json.JSONObject

const val obsMinimumAudioDelay = -950
const val obsMaximumAudioDelay = 20000

private const val TAG = "ObsWebSocket"

private const val rpcVersion = 1

private val json = Json { ignoreUnknownKeys = true }

private fun isLoopback(url: String): Boolean {
    val host = runCatching { URI(url).host }.getOrNull() ?: return false
    return host == "localhost" || host == "127.0.0.1" || host == "::1"
}

private fun defaultApplicationContext(): Context {
    val activityThread = Class.forName("android.app.ActivityThread")
    val method = activityThread.getMethod("currentApplication")
    return method.invoke(null) as Context
}

private enum class EventSubscription(val rawValue: ULong) {
    general(0x1uL),
    config(0x2uL),
    scenes(0x4uL),
    inputs(0x8uL),
    transitions(0x10uL),
    filters(0x20uL),
    outputs(0x40uL),
    sceneItems(0x80uL),
    mediaInputs(0x100uL),
    vendors(0x200uL),
    ui(0x400uL),
    inputVolumeMeters(0x10000uL),
    inputActiveStateChanged(0x20000uL),
    inputShowStateChanged(0x40000uL),
    sceneItemTransformChanged(0x80000uL),
    ;

    companion object {
        fun all(): ULong {
            return general.rawValue or
                config.rawValue or
                scenes.rawValue or
                inputs.rawValue or
                transitions.rawValue or
                filters.rawValue or
                outputs.rawValue or
                sceneItems.rawValue or
                mediaInputs.rawValue or
                vendors.rawValue or
                ui.rawValue
        }
    }
}

private fun mulToDb(mul: Float): Float {
    return 20 * log10(mul)
}

private enum class OpCode(val rawValue: Int) {
    hello(0),
    identify(1),
    identified(2),
    reidentify(3),
    event(5),
    request(6),
    requestResponse(7),
    requestBatch(8),
    requestBatchResponse(9),
    ;

    companion object {
        fun fromRawValue(value: Int): OpCode? = entries.firstOrNull { it.rawValue == value }
    }
}

private enum class RequestStatus(val rawValue: Int) {
    unknown(0),
    noError(10),
    success(100),
    missingRequestType(203),
    unknownRequestType(204),
    genericError(205),
    unsupportedRequestBatchExecutionType(206),
    notReady(207),
    missingRequestField(300),
    missingRequestData(301),
    invalidRequestField(400),
    invalidRequestFieldType(401),
    requestFieldOutOfRange(402),
    requestFieldEmpty(403),
    tooManyRequestFields(404),
    outputRunning(500),
    outputNotRunning(501),
    outputPaused(502),
    outputNotPaused(503),
    outputDisabled(504),
    studioModeActive(505),
    studioModeNotActive(506),
    resourceNotFound(600),
    resourceAlreadyExists(601),
    invalidResourceType(602),
    notEnoughResources(603),
    invalidResourceState(604),
    invalidInputKind(605),
    resourceNotConfigurable(606),
    invalidFilterKind(607),
    resourceCreationFailed(700),
    resourceActionFailed(701),
    requestProcessingFailed(702),
    cannotAct(703),
    ;

    companion object {
        fun fromRawValue(value: Int): RequestStatus? = entries.firstOrNull { it.rawValue == value }
    }
}

@Serializable
private data class ResponseRequestStatus(
    val result: Boolean,
    val code: Int,
    val comment: String? = null,
)

private enum class RequestType(val rawValue: String) {
    getSceneList("GetSceneList"),
    getSceneItemList("GetSceneItemList"),
    setCurrentProgramScene("SetCurrentProgramScene"),
    getStreamStatus("GetStreamStatus"),
    startStream("StartStream"),
    stopStream("StopStream"),
    getRecordStatus("GetRecordStatus"),
    startRecord("StartRecord"),
    stopRecord("StopRecord"),
    getSourceScreenshot("GetSourceScreenshot"),
    getVersion("GetVersion"),
    setInputAudioSyncOffset("SetInputAudioSyncOffset"),
    getInputAudioSyncOffset("GetInputAudioSyncOffset"),
    setSceneItemEnabled("SetSceneItemEnabled"),
    getSceneItemId("GetSceneItemId"),
    setInputSettings("SetInputSettings"),
    setInputMute("SetInputMute"),
    getInputMute("GetInputMute"),
    getInputList("GetInputList"),
    getInputSettings("GetInputSettings"),
    getSpecialInputs("GetSpecialInputs"),
}

private enum class EventType(val rawValue: String) {
    mediaInputPlaybackStarted("MediaInputPlaybackStarted"),
    mediaInputPlaybackEnded("MediaInputPlaybackEnded"),
    currentProgramSceneChanged("CurrentProgramSceneChanged"),
    streamStateChanged("StreamStateChanged"),
    recordStateChanged("RecordStateChanged"),
    inputVolumeMeters("InputVolumeMeters"),
    inputAudioSyncOffsetChanged("InputAudioSyncOffsetChanged"),
    inputMuteStateChanged("InputMuteStateChanged"),
    ;

    companion object {
        fun fromRawValue(value: String): EventType? = entries.firstOrNull { it.rawValue == value }
    }
}

@Serializable
private data class Identify(
    val rpcVersion: Int,
    val authentication: String? = null,
)

@Serializable
private data class Identified(
    val negotiatedRpcVersion: Int,
)

@Serializable
private data class Reidentify(
    val eventSubscriptions: ULong,
)

@Serializable
private data class HelloAuthentication(
    val challenge: String,
    val salt: String,
)

@Serializable
private data class Hello(
    val authentication: HelloAuthentication? = null,
)

@Serializable
data class GetSceneListResponseScene(
    val sceneName: String,
)

@Serializable
data class GetSceneListResponse(
    val currentProgramSceneName: String,
    val scenes: List<GetSceneListResponseScene>,
)

@Serializable
data class GetSceneItemList(
    val sceneName: String,
)

@Serializable
data class GetSceneItemListItem(
    val sourceName: String,
    val inputKind: String? = null,
    val sceneItemEnabled: Boolean,
)

@Serializable
data class GetSceneItemListResponse(
    val sceneItems: List<GetSceneItemListItem>,
)

@Serializable
data class GetSpecialInputsResponse(
    val desktop1: String? = null,
    val desktop2: String? = null,
    val mic1: String? = null,
    val mic2: String? = null,
    val mic3: String? = null,
    val mic4: String? = null,
) {
    fun mics(): List<String> {
        return listOfNotNull(mic1, mic2, mic3, mic4)
    }
}

@Serializable
data class GetInputListResponseInput(
    val inputName: String,
)

@Serializable
data class GetInputListResponse(
    val inputs: List<GetInputListResponseInput>,
)

@Serializable
data class SetCurrentProgramSceneRequest(
    val sceneName: String,
)

@Serializable
data class SetItemUrlInputSettings(
    val input: String,
)

@Serializable
data class SetItemUrlRequest(
    val inputName: String,
    val inputSettings: SetItemUrlInputSettings,
)

@Serializable
data class GetStreamStatusResponse(
    val outputActive: Boolean,
)

@Serializable
data class GetRecordStatusResponse(
    val outputActive: Boolean,
)

@Serializable
data class GetSourceScreenshot(
    val sourceName: String,
    val imageFormat: String,
    val imageWidth: Int,
    val imageCompressionQuality: Int,
)

@Serializable
data class GetSourceScreenshotResponse(
    val imageData: String,
)

@Serializable
data class SetInputAudioSyncOffset(
    val inputName: String,
    val inputAudioSyncOffset: Int,
)

@Serializable
data class GetInputAudioSyncOffset(
    val inputName: String,
)

@Serializable
data class GetInputAudioSyncOffsetResponse(
    val inputAudioSyncOffset: Int,
)

@Serializable
class InputSettings

@Serializable
data class SetInputSettings(
    val inputName: String,
    val inputSettings: InputSettings,
)

@Serializable
data class GetInputSettingsRequest(
    val inputName: String,
)

@Serializable
data class ObsMediaSourceInputSettings(
    val input: String,
    val is_local_file: Boolean,
)

@Serializable
data class GetInputSettingsResponse(
    val inputSettings: ObsMediaSourceInputSettings,
)

data class ObsMediaSourceSettings(
    val input: String,
    val isLocalFile: Boolean,
)

@Serializable
data class SetInputMute(
    val inputName: String,
    val inputMuted: Boolean,
)

@Serializable
data class GetInputMute(
    val inputName: String,
)

@Serializable
data class GetInputMuteResponse(
    val inputMuted: Boolean,
)

@Serializable
data class SceneChangedEvent(
    val sceneName: String,
)

@Serializable
data class InputMuteStateChangedEvent(
    val inputName: String,
    val inputMuted: Boolean,
)

enum class ObsOutputState(val rawValue: String) {
    starting("OBS_WEBSOCKET_OUTPUT_STARTING"),
    started("OBS_WEBSOCKET_OUTPUT_STARTED"),
    stopping("OBS_WEBSOCKET_OUTPUT_STOPPING"),
    stopped("OBS_WEBSOCKET_OUTPUT_STOPPED"),
    ;

    companion object {
        fun fromRawValue(value: String): ObsOutputState? = entries.firstOrNull { it.rawValue == value }
    }
}

@Serializable
data class StreamStateChangedEvent(
    val outputActive: Boolean,
    val outputState: String,
)

@Serializable
data class RecordStateChangedEvent(
    val outputActive: Boolean,
    val outputState: String,
)

@Serializable
data class InputVolumeMeter(
    val inputName: String,
    val inputLevelsMul: List<List<Float>>,
)

@Serializable
data class InputVolumeMeters(
    val inputs: List<InputVolumeMeter>,
)

data class ObsAudioInputVolume(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    var volumes: MutableList<Float> = mutableListOf(),
)

private fun packMessage(op: OpCode, data: ByteArray): String {
    val dataString = data.toString(Charsets.UTF_8)
    return "{\"op\": ${op.rawValue}, \"d\": $dataString}"
}

private fun unpackMessage(message: String): Pair<OpCode?, ByteArray> {
    val jsonResult = runCatching { JSONObject(message) }.getOrNull()
        ?: throw IllegalStateException("JSON decode failed")
    val opValue = jsonResult.opt("op") as? Int ?: throw IllegalStateException("OP not an integer")
    val op = OpCode.fromRawValue(opValue) ?: return Pair(null, ByteArray(0))
    val data = jsonResult.opt("d") as? JSONObject ?: throw IllegalStateException("No data")
    return Pair(op, data.toString().toByteArray(Charsets.UTF_8))
}

private fun unpackEvent(data: ByteArray): Triple<EventType?, Int, ByteArray?> {
    val event = runCatching { JSONObject(data.toString(Charsets.UTF_8)) }.getOrNull()
        ?: throw IllegalStateException("JSON decode failed")
    val typeValue = event.opt("eventType") as? String
        ?: throw IllegalStateException("Event type not a string")
    val type = EventType.fromRawValue(typeValue)
    if (type == null) {
        Log.d(TAG, "obs-websocket: Unsupported event $typeValue")
        return Triple(null, 0, null)
    }
    val intent = event.opt("eventIntent") as? Int
        ?: throw IllegalStateException("Event intent not an integer")
    var eventDataBytes: ByteArray? = null
    val eventData = event.opt("eventData")
    if (eventData != null) {
        val dataDict = eventData as? JSONObject
            ?: throw IllegalStateException("Event data not a dictionary")
        eventDataBytes = dataDict.toString().toByteArray(Charsets.UTF_8)
    }
    return Triple(type, intent, eventDataBytes)
}

private fun unpackRequestResponse(data: ByteArray): Triple<String, ResponseRequestStatus, ByteArray?> {
    val response = runCatching { JSONObject(data.toString(Charsets.UTF_8)) }.getOrNull()
        ?: throw IllegalStateException("JSON decode failed")
    val requestId = response.opt("requestId") as? String
        ?: throw IllegalStateException("Request response request id not a string")
    val statusDict = response.opt("requestStatus") as? JSONObject
        ?: throw IllegalStateException("Request response status not an object")
    val status = json.decodeFromString(ResponseRequestStatus.serializer(), statusDict.toString())
    var responseData: ByteArray? = null
    val dataJson = response.opt("responseData")
    if (dataJson != null) {
        val dataDict = dataJson as? JSONObject
            ?: throw IllegalStateException("Request response data not an object")
        responseData = dataDict.toString().toByteArray(Charsets.UTF_8)
    }
    return Triple(requestId, status, responseData)
}

private fun unpackRequestBatchResponse(
    data: ByteArray,
): Pair<String, List<Pair<ResponseRequestStatus, ByteArray?>>> {
    val response = runCatching { JSONObject(data.toString(Charsets.UTF_8)) }.getOrNull()
        ?: throw IllegalStateException("JSON decode failed")
    val requestId = response.opt("requestId") as? Int
        ?: throw IllegalStateException("Request batch response request id not a string")
    val resultsList = response.opt("results") as? JSONArray
        ?: throw IllegalStateException("Request batch response results missing")
    val results = mutableListOf<Pair<ResponseRequestStatus, ByteArray?>>()
    for (index in 0 until resultsList.length()) {
        val resultData = resultsList.getJSONObject(index).toString().toByteArray(Charsets.UTF_8)
        val (_, status, resultBytes) = unpackRequestResponse(resultData)
        results.add(Pair(status, resultBytes))
    }
    return Pair(requestId.toString(), results)
}

private class Request(
    val onSuccess: (ByteArray?) -> Unit,
    val onError: (RequestStatus, String?) -> Unit,
)

private class BatchRequest(
    val onComplete: (List<Pair<ResponseRequestStatus, ByteArray?>>) -> Unit,
)

data class ObsSceneList(
    val current: String,
    val scenes: List<String>,
)

data class ObsStreamStatus(
    val active: Boolean,
    val state: ObsOutputState? = null,
)

data class ObsRecordStatus(
    val active: Boolean,
)

interface ObsWebsocketDelegate {
    fun obsWebsocketConnected()
    fun obsWebsocketSceneChanged(sceneName: String)
    fun obsWebsocketInputMuteStateChangedEvent(inputName: String, muted: Boolean)
    fun obsWebsocketStreamStatusChanged(active: Boolean, state: ObsOutputState?)
    fun obsWebsocketRecordStatusChanged(active: Boolean, state: ObsOutputState?)
    fun obsWebsocketAudioVolume(volumes: List<ObsAudioInputVolume>)
}

class ObsWebSocket(
    private val context: Context,
    private val url: String,
    private val password: String,
    val delegate: ObsWebsocketDelegate?,
) : WebSocketClientDelegate {
    constructor(
        url: String,
        password: String,
        delegate: ObsWebsocketDelegate?,
    ) : this(defaultApplicationContext(), url, password, delegate)

    private var webSocket: WebSocketClient = WebSocketClient(context, url, isLoopback(url))
    private var nextId: Int = 0
    private val requests = mutableMapOf<String, Request>()
    private val batchRequests = mutableMapOf<String, BatchRequest>()
    var connectionErrorMessage: String = ""
    private var connected = false
    private val mainScope = CoroutineScope(Dispatchers.Main)

    fun start() {
        Log.d(TAG, "obs-websocket: start")
        startInternal()
    }

    fun stop() {
        Log.d(TAG, "obs-websocket: stop")
        stopInternal()
    }

    private fun startInternal() {
        stopInternal()
        webSocket = WebSocketClient(context, url, isLoopback(url))
        webSocket.delegate = this
        webSocket.start()
    }

    fun stopInternal() {
        webSocket.stop()
        connected = false
    }

    fun isConnected(): Boolean {
        return connected
    }

    fun startAudioVolume() {
        sendReidentify(
            EventSubscription.all() or EventSubscription.inputVolumeMeters.rawValue,
        )
    }

    fun stopAudioVolume() {
        sendReidentify(EventSubscription.all())
    }

    fun getSceneList(onSuccess: (ObsSceneList) -> Unit, onError: (String) -> Unit) {
        performRequest<GetSceneListResponse>(
            type = RequestType.getSceneList,
            onError = onError,
        ) { response ->
            onSuccess(
                ObsSceneList(
                    current = response.currentProgramSceneName,
                    scenes = response.scenes.reversed().map { it.sceneName },
                ),
            )
        }
    }

    fun getSceneItemList(
        sceneName: String,
        onSuccess: (List<GetSceneItemListItem>) -> Unit,
        onError: (String) -> Unit,
    ) {
        performRequest<GetSceneItemListResponse>(
            type = RequestType.getSceneItemList,
            request = json.encodeToString(GetSceneItemList(sceneName = sceneName)),
            onError = onError,
        ) { response ->
            onSuccess(response.sceneItems)
        }
    }

    fun getSpecialInputs(
        onSuccess: (GetSpecialInputsResponse) -> Unit,
        onError: (String) -> Unit,
    ) {
        performRequest<GetSpecialInputsResponse>(
            type = RequestType.getSpecialInputs,
            onError = onError,
            onSuccess = onSuccess,
        )
    }

    fun getInputList(onSuccess: (List<String>) -> Unit, onError: (String) -> Unit) {
        performRequest<GetInputListResponse>(
            type = RequestType.getInputList,
            onError = onError,
        ) { response ->
            onSuccess(response.inputs.map { it.inputName })
        }
    }

    fun setCurrentProgramScene(
        name: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        performRequest(
            type = RequestType.setCurrentProgramScene,
            request = json.encodeToString(SetCurrentProgramSceneRequest(sceneName = name)),
            onError = onError,
            onSuccess = onSuccess,
        )
    }

    fun setMediaSourceSettings(
        name: String,
        input: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {},
    ) {
        performRequest(
            type = RequestType.setInputSettings,
            request = json.encodeToString(
                SetItemUrlRequest(
                    inputName = name,
                    inputSettings = SetItemUrlInputSettings(input = input),
                ),
            ),
            onError = onError,
            onSuccess = onSuccess,
        )
    }

    fun getStreamStatus(
        onSuccess: (ObsStreamStatus) -> Unit,
        onError: (String) -> Unit,
    ) {
        performRequest<GetStreamStatusResponse>(
            type = RequestType.getStreamStatus,
            onError = onError,
        ) { response ->
            onSuccess(ObsStreamStatus(active = response.outputActive))
        }
    }

    fun getRecordStatus(
        onSuccess: (ObsRecordStatus) -> Unit,
        onError: (String) -> Unit,
    ) {
        performRequest<GetRecordStatusResponse>(
            type = RequestType.getRecordStatus,
            onError = onError,
        ) { response ->
            onSuccess(ObsRecordStatus(active = response.outputActive))
        }
    }

    fun startStream(onSuccess: () -> Unit, onError: (String) -> Unit) {
        performRequest(
            type = RequestType.startStream,
            errorMessages = mapOf(RequestStatus.outputRunning to "Already streaming"),
            onError = onError,
            onSuccess = onSuccess,
        )
    }

    fun stopStream(onSuccess: () -> Unit, onError: (String) -> Unit) {
        performRequest(
            type = RequestType.stopStream,
            errorMessages = mapOf(RequestStatus.outputNotRunning to "Not streaming"),
            onError = onError,
            onSuccess = onSuccess,
        )
    }

    fun startRecord(onSuccess: () -> Unit, onError: (String) -> Unit) {
        performRequest(
            type = RequestType.startRecord,
            errorMessages = mapOf(RequestStatus.outputRunning to "Already recording"),
            onError = onError,
            onSuccess = onSuccess,
        )
    }

    fun stopRecord(onSuccess: () -> Unit, onError: (String) -> Unit) {
        performRequest(
            type = RequestType.stopRecord,
            errorMessages = mapOf(RequestStatus.outputNotRunning to "Not recording"),
            onError = onError,
            onSuccess = onSuccess,
        )
    }

    fun getSourceScreenshot(
        name: String,
        onSuccess: (ByteArray) -> Unit,
        onError: (String) -> Unit,
    ) {
        val request = GetSourceScreenshot(
            sourceName = name,
            imageFormat = "jpg",
            imageWidth = 640,
            imageCompressionQuality = 30,
        )
        performRequest<GetSourceScreenshotResponse>(
            type = RequestType.getSourceScreenshot,
            request = json.encodeToString(request),
            onError = onError,
        ) { response ->
            val imageData = response.imageData
            val image = runCatching {
                Base64.getDecoder().decode(imageData.drop(22))
            }.getOrNull()
            if (image != null) {
                onSuccess(image)
            } else {
                onError("Base64 decode failed")
            }
        }
    }

    fun setInputAudioSyncOffset(
        name: String,
        offsetInMs: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        performRequest(
            type = RequestType.setInputAudioSyncOffset,
            request = json.encodeToString(
                SetInputAudioSyncOffset(inputName = name, inputAudioSyncOffset = offsetInMs),
            ),
            onError = onError,
            onSuccess = onSuccess,
        )
    }

    fun getInputAudioSyncOffset(
        name: String,
        onSuccess: (Int) -> Unit,
        onError: (String) -> Unit,
    ) {
        performRequest<GetInputAudioSyncOffsetResponse>(
            type = RequestType.getInputAudioSyncOffset,
            request = json.encodeToString(GetInputAudioSyncOffset(inputName = name)),
            onError = onError,
        ) { response ->
            onSuccess(response.inputAudioSyncOffset)
        }
    }

    fun setInputSettings(
        inputName: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        performRequest(
            type = RequestType.setInputSettings,
            request = json.encodeToString(
                SetInputSettings(inputName = inputName, inputSettings = InputSettings()),
            ),
            onError = onError,
            onSuccess = onSuccess,
        )
    }

    fun setInputMute(
        inputName: String,
        muted: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        performRequest(
            type = RequestType.setInputMute,
            request = json.encodeToString(SetInputMute(inputName = inputName, inputMuted = muted)),
            onError = onError,
            onSuccess = onSuccess,
        )
    }

    fun getMediaSourcesSettingsBatch(
        inputNames: List<String>,
        onSuccess: (List<ObsMediaSourceSettings?>) -> Unit,
        onError: (String) -> Unit,
    ) {
        performRequestBatch<GetInputSettingsResponse>(
            type = RequestType.getInputSettings,
            requests = inputNames.map { json.encodeToString(GetInputSettingsRequest(inputName = it)) },
            onError = onError,
        ) { responses ->
            onSuccess(
                responses.map { response ->
                    response?.let {
                        ObsMediaSourceSettings(
                            input = it.inputSettings.input,
                            isLocalFile = it.inputSettings.is_local_file,
                        )
                    }
                },
            )
        }
    }

    fun getInputMuteBatch(
        inputNames: List<String>,
        onSuccess: (List<Boolean?>) -> Unit,
        onError: (String) -> Unit,
    ) {
        performRequestBatch<GetInputMuteResponse>(
            type = RequestType.getInputMute,
            requests = inputNames.map { json.encodeToString(GetInputMute(inputName = it)) },
            onError = onError,
        ) { responses ->
            onSuccess(responses.map { it?.inputMuted })
        }
    }

    private inline fun <reified Response> performRequest(
        type: RequestType,
        request: String? = null,
        noinline onError: (String) -> Unit,
        noinline onSuccess: (Response) -> Unit,
    ) {
        val responseSerializer = serializer<Response>()
        sendRequest(
            type = type,
            request = request,
            errorMessages = emptyMap(),
            onError = onError,
        ) { response ->
            if (response == null) {
                onError("Response data missing")
            } else {
                runCatching {
                    json.decodeFromString(responseSerializer, response.toString(Charsets.UTF_8))
                }
                    .onSuccess { onSuccess(it) }
                    .onFailure { onError("JSON decode failed") }
            }
        }
    }

    private fun performRequest(
        type: RequestType,
        request: String? = null,
        errorMessages: Map<RequestStatus, String> = emptyMap(),
        onError: (String) -> Unit,
        onSuccess: () -> Unit,
    ) {
        sendRequest(
            type = type,
            request = request,
            errorMessages = errorMessages,
            onError = onError,
        ) { _ ->
            onSuccess()
        }
    }

    private inline fun <reified Response> performRequestBatch(
        type: RequestType,
        requests: List<String>,
        noinline onError: (String) -> Unit,
        noinline onSuccess: (List<Response?>) -> Unit,
    ) {
        if (!isConnected()) {
            onError("Not connected to server")
            return
        }
        val responseSerializer = serializer<Response>()
        val packedRequests = mutableListOf<String>()
        for (request in requests) {
            val packed = runCatching { packRequest(type = type, requestData = request) }.getOrNull()
            if (packed == null) {
                onError("Failed to create OBS message")
                return
            }
            packedRequests.add(packed.first.toString(Charsets.UTF_8))
        }
        val requestId = getNextId()
        batchRequests[requestId] = BatchRequest(
            onComplete = { results ->
                onSuccess(
                    results.map { (status, response) ->
                        if (!status.result || response == null) {
                            null
                        } else {
                            runCatching {
                                json.decodeFromString(
                                    responseSerializer,
                                    response.toString(Charsets.UTF_8),
                                )
                            }.getOrNull()
                        }
                    },
                )
            },
        )
        val requestBatch = """
            {
              "requestId": $requestId,
              "requests": [${packedRequests.joinToString(",")}]
            }
        """.trimIndent()
        send(OpCode.requestBatch, requestBatch.toByteArray(Charsets.UTF_8))
    }

    private fun sendRequest(
        type: RequestType,
        request: String?,
        errorMessages: Map<RequestStatus, String>,
        onError: (String) -> Unit,
        onSuccess: (ByteArray?) -> Unit,
    ) {
        if (!isConnected()) {
            onError("Not connected to server")
            return
        }
        val packed = runCatching { packRequest(type = type, requestData = request) }.getOrNull()
        if (packed == null) {
            onError("Failed to create OBS message")
            return
        }
        val (requestBytes, requestId) = packed
        requests[requestId] = Request(
            onSuccess = onSuccess,
            onError = { code, comment ->
                val message = errorMessages[code]
                when {
                    message != null -> onError(message)
                    comment != null -> onError("Operation failed with $code ($comment)")
                    else -> onError("Operation failed with $code")
                }
            },
        )
        send(OpCode.request, requestBytes)
    }

    private fun packRequest(type: RequestType, requestData: String?): Pair<ByteArray, String> {
        val requestId = getNextId()
        val request = if (requestData != null) {
            """
            {
               "requestType": "${type.rawValue}",
               "requestId": "$requestId",
               "requestData": $requestData
            }
            """.trimIndent().toByteArray(Charsets.UTF_8)
        } else {
            """
            {
               "requestType": "${type.rawValue}",
               "requestId": "$requestId"
            }
            """.trimIndent().toByteArray(Charsets.UTF_8)
        }
        return Pair(request, requestId)
    }

    private fun handleMessage(message: String) {
        val (op, data) = unpackMessage(message)
        when (op) {
            OpCode.hello -> handleHello(data)
            OpCode.identified -> handleIdentified(data)
            OpCode.event -> handleEvent(data)
            OpCode.requestResponse -> handleRequestResponse(data)
            OpCode.requestBatchResponse -> handleRequestBatchResponse(data)
            null -> Log.d(TAG, "obs-websocket: Ignoring message nil")
            else -> Log.d(TAG, "obs-websocket: Ignoring message $op")
        }
    }

    private fun handleHello(data: ByteArray) {
        val hello = json.decodeFromString(Hello.serializer(), data.toString(Charsets.UTF_8))
        var authentication: String? = null
        val helloAuthentication = hello.authentication
        if (helloAuthentication != null) {
            var concatenated = "$password${helloAuthentication.salt}"
            var hash = MessageDigest.getInstance("SHA-256")
                .digest(concatenated.toByteArray(Charsets.UTF_8))
            concatenated =
                "${Base64.getEncoder().encodeToString(hash)}${helloAuthentication.challenge}"
            hash = MessageDigest.getInstance("SHA-256").digest(concatenated.toByteArray(Charsets.UTF_8))
            authentication = Base64.getEncoder().encodeToString(hash)
        }
        sendIdentify(authentication)
    }

    private fun handleIdentified(data: ByteArray) {
        val identified = json.decodeFromString(Identified.serializer(), data.toString(Charsets.UTF_8))
        Log.d(TAG, "obs-websocket: $identified")
        connected = true
        delegate?.obsWebsocketConnected()
    }

    private fun handleEvent(data: ByteArray) {
        val (type, _, eventData) = unpackEvent(data)
        when (type) {
            EventType.mediaInputPlaybackStarted -> {}
            EventType.mediaInputPlaybackEnded -> {}
            EventType.currentProgramSceneChanged -> handleSceneChanged(eventData)
            EventType.streamStateChanged -> handleStreamChanged(eventData)
            EventType.recordStateChanged -> handleRecordChanged(eventData)
            EventType.inputVolumeMeters -> handleInputVolumeMeters(eventData)
            EventType.inputAudioSyncOffsetChanged -> handleInputAudioSyncOffsetChanged(eventData)
            EventType.inputMuteStateChanged -> handleInputMuteStateChanged(eventData)
            null -> {}
        }
    }

    private fun handleSceneChanged(data: ByteArray?) {
        if (data == null) {
            return
        }
        runCatching {
            json.decodeFromString(SceneChangedEvent.serializer(), data.toString(Charsets.UTF_8))
        }.onSuccess { decoded ->
            delegate?.obsWebsocketSceneChanged(sceneName = decoded.sceneName)
        }
    }

    private fun handleInputMuteStateChanged(data: ByteArray?) {
        if (data == null) {
            return
        }
        runCatching {
            json.decodeFromString(
                InputMuteStateChangedEvent.serializer(),
                data.toString(Charsets.UTF_8),
            )
        }.onSuccess { decoded ->
            delegate?.obsWebsocketInputMuteStateChangedEvent(
                inputName = decoded.inputName,
                muted = decoded.inputMuted,
            )
        }
    }

    private fun handleStreamChanged(data: ByteArray?) {
        if (data == null) {
            return
        }
        runCatching {
            json.decodeFromString(StreamStateChangedEvent.serializer(), data.toString(Charsets.UTF_8))
        }.onSuccess { event ->
            val state = ObsOutputState.fromRawValue(event.outputState)
            if (state != null) {
                delegate?.obsWebsocketStreamStatusChanged(event.outputActive, state)
            } else {
                delegate?.obsWebsocketStreamStatusChanged(event.outputActive, ObsOutputState.stopped)
            }
        }
    }

    private fun handleRecordChanged(data: ByteArray?) {
        if (data == null) {
            return
        }
        runCatching {
            json.decodeFromString(RecordStateChangedEvent.serializer(), data.toString(Charsets.UTF_8))
        }.onSuccess { event ->
            val state = ObsOutputState.fromRawValue(event.outputState)
            if (state != null) {
                delegate?.obsWebsocketRecordStatusChanged(event.outputActive, state)
            } else {
                delegate?.obsWebsocketRecordStatusChanged(event.outputActive, ObsOutputState.started)
            }
        }
    }

    private fun handleInputVolumeMeters(data: ByteArray?) {
        if (data == null) {
            return
        }
        runCatching {
            json.decodeFromString(InputVolumeMeters.serializer(), data.toString(Charsets.UTF_8))
        }.onSuccess { decoded ->
            val volumes = mutableListOf<ObsAudioInputVolume>()
            for (input in decoded.inputs) {
                val audioInput = ObsAudioInputVolume(name = input.inputName)
                for (channel in input.inputLevelsMul) {
                    if (channel.isNotEmpty()) {
                        audioInput.volumes.add(mulToDb(channel[0]))
                    }
                }
                volumes.add(audioInput)
            }
            delegate?.obsWebsocketAudioVolume(volumes)
        }
    }

    private fun handleInputAudioSyncOffsetChanged(data: ByteArray?) {}

    private fun handleRequestResponse(data: ByteArray) {
        val (requestId, status, responseData) = unpackRequestResponse(data)
        val request = requests.remove(requestId)
        if (request == null) {
            Log.d(TAG, "Unexpected request id in response")
            return
        }
        if (status.result) {
            request.onSuccess(responseData)
        } else {
            request.onError(
                RequestStatus.fromRawValue(status.code) ?: RequestStatus.unknown,
                status.comment,
            )
        }
    }

    private fun handleRequestBatchResponse(data: ByteArray) {
        val (requestId, results) = unpackRequestBatchResponse(data)
        val batchRequest = batchRequests.remove(requestId)
        if (batchRequest == null) {
            Log.d(TAG, "Unexpected request id in batch response")
            return
        }
        batchRequest.onComplete(results)
    }

    private fun sendIdentify(authentication: String?) {
        val identify = Identify(rpcVersion = rpcVersion, authentication = authentication)
        runCatching {
            val encoded = json.encodeToString(Identify.serializer(), identify)
            send(OpCode.identify, encoded.toByteArray(Charsets.UTF_8))
        }
    }

    private fun sendReidentify(eventSubscriptions: ULong) {
        val reidentify = Reidentify(eventSubscriptions = eventSubscriptions)
        runCatching {
            val encoded = json.encodeToString(Reidentify.serializer(), reidentify)
            send(OpCode.reidentify, encoded.toByteArray(Charsets.UTF_8))
        }
    }

    private fun getNextId(): String {
        nextId += 1
        return nextId.toString()
    }

    private fun send(op: OpCode, data: ByteArray) {
        val message = packMessage(op, data)
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "obs-websocket: Sending ${message.take(250)}")
        }
        webSocket.send(message)
    }

    override fun webSocketClientConnected(client: WebSocketClient) {}

    override fun webSocketClientDisconnected(client: WebSocketClient) {
        mainScope.launch {
            connected = false
            connectionErrorMessage = localized("Disconnected")
        }
    }

    override fun webSocketClientReceiveMessage(client: WebSocketClient, string: String) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "obs-websocket: Received ${string.take(250)}")
        }
        mainScope.launch {
            runCatching { handleMessage(string) }
                .onFailure { Log.i(TAG, "obs-websocket: Error: $it") }
        }
    }
}

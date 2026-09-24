package com.moblin.android.various

import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.net.Uri
import android.util.Log
import android.util.Size
import android.view.Surface
import com.moblin.android.common.various.defaultAudioLevel
import com.moblin.android.localized
import com.moblin.android.media.MediaSample
import com.moblin.android.media.adaptivebitrate.AdaptiveBitrate
import com.moblin.android.media.adaptivebitrate.AdaptiveBitrateDelegate
import com.moblin.android.media.adaptivebitrate.AdaptiveBitrateRistExperiment
import com.moblin.android.media.adaptivebitrate.AdaptiveBitrateSettings
import com.moblin.android.media.adaptivebitrate.AdaptiveBitrateSrtBelabox
import com.moblin.android.media.adaptivebitrate.AdaptiveBitrateSrtFight
import com.moblin.android.media.adaptivebitrate.StreamStats
import com.moblin.android.media.haishinkit.codec.audio.AudioEncoderDelegate
import com.moblin.android.media.haishinkit.codec.audio.AudioEncoderSettings
import com.moblin.android.media.haishinkit.codec.video.VideoEncoderDelegate
import com.moblin.android.media.haishinkit.codec.video.VideoEncoderSettings
import com.moblin.android.media.haishinkit.codec.video.numberOfFailedEncodings
import com.moblin.android.media.haishinkit.media.AudioVideoEncoderDelegate
import com.moblin.android.media.haishinkit.media.Processor
import com.moblin.android.media.haishinkit.media.ProcessorDelegate
import com.moblin.android.media.haishinkit.media.RecorderDataSegment
import com.moblin.android.media.haishinkit.media.processorControlQueue
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.audio.AudioUnitAttachParams
import com.moblin.android.media.haishinkit.media.video.CaptureDevice
import com.moblin.android.media.haishinkit.media.video.CaptureDevices
import com.moblin.android.media.haishinkit.media.video.PreviewView
import com.moblin.android.media.haishinkit.media.video.SceneSwitchTransition
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoUnitAttachParams
import com.moblin.android.platform.network.NWEndpoint
import com.moblin.android.media.haishinkit.rist.RistStream
import com.moblin.android.media.haishinkit.rist.RistStreamDelegate
import com.moblin.android.media.haishinkit.rtmp.RtmpConnectionCode
import com.moblin.android.media.haishinkit.rtmp.RtmpStream
import com.moblin.android.media.haishinkit.rtmp.RtmpStreamDelegate
import com.moblin.android.media.haishinkit.srt.SrtPerformanceData
import com.moblin.android.media.haishinkit.srt.SrtStreamMoblin
import com.moblin.android.media.haishinkit.srt.SrtStreamMoblinDelegate
import com.moblin.android.media.haishinkit.srt.SrtStreamOfficial
import com.moblin.android.media.haishinkit.srt.SrtStreamOfficialDelegate
import com.moblin.android.media.haishinkit.whip.WhipStream
import com.moblin.android.media.haishinkit.whip.WhipStreamDelegate
import com.moblin.android.media.mobcamstream.MobcamStream
import com.moblin.android.media.mobcamstream.MobcamStreamDelegate
import com.moblin.android.media.srtla.client.SrtlaClient
import com.moblin.android.media.srtla.client.SrtlaDelegate
import com.moblin.android.media.srtla.client.srtlaClientQueue
import com.moblin.android.media.webrtc.defaultStunServer
import com.moblin.android.various.settings.SettingsDnsLookupStrategy
import com.moblin.android.various.settings.SettingsGraphicsImplementation
import com.moblin.android.various.settings.SettingsHttpHeader
import com.moblin.android.various.settings.SettingsNetworkInterfaceName
import com.moblin.android.various.settings.SettingsStreamAudioCodec
import com.moblin.android.various.settings.SettingsStreamCodec
import com.moblin.android.various.settings.SettingsStreamMultiStreamingDestination
import com.moblin.android.various.settings.SettingsStreamProtocol
import com.moblin.android.various.settings.SettingsStreamRateControl
import com.moblin.android.various.settings.SettingsStreamResolution
import com.moblin.android.various.settings.SettingsStreamSrtAdaptiveBitrateAlgorithm
import com.moblin.android.various.settings.SettingsStreamSrtConnectionPriorities
import com.moblin.android.various.settings.SettingsStreamSrtImplementation
import com.moblin.android.various.settings.SettingsVideoStabilizationMode
import com.moblin.android.various.utils.extractSrtStreamId
import java.net.URI
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Request
import okhttp3.Response
import com.moblin.android.platform.avfoundation.AVCaptureDevice

interface MediaDelegate {
    fun mediaOnSrtConnected()
    fun mediaOnSrtDisconnected(reason: String)
    fun mediaOnRtmpConnected()
    fun mediaOnRtmpDisconnected(message: String)
    fun mediaOnRtmpDestinationConnected(destination: String)
    fun mediaOnRtmpDestinationDisconnected(destination: String)
    fun mediaOnRistConnected()
    fun mediaOnRistDisconnected()
    fun mediaOnWhipConnected()
    fun mediaOnWhipDisconnected(reason: String)
    fun mediaOnMobcamConnected()
    fun mediaOnMobcamDisconnected(reason: String)
    fun mediaOnWhipPerform(
        request: Request,
        queue: CoroutineDispatcher,
        completion: ((ByteArray?, Response?, Throwable?) -> Unit)?
    )
    fun mediaOnAudioBuffer(sampleBuffer: MediaSample)
    fun mediaOnLowFpsImage(lowFpsImage: ByteArray?, frameNumber: Long)
    fun mediaOnAttachCameraError()
    fun mediaOnCaptureSessionError(message: String)
    fun mediaOnBufferedVideoReady(cameraId: UUID)
    fun mediaOnBufferedVideoRemoved(cameraId: UUID)
    fun mediaOnEncoderResolutionChanged(resolution: Size)
    fun mediaOnRecorderInitSegment(data: ByteArray)
    fun mediaOnRecorderDataSegment(segment: RecorderDataSegment)
    fun mediaOnRecorderFinished()
    fun mediaOnNoTorch()
    fun mediaOnFps(fps: Int)
    fun mediaMoblinkStreamerDestinationAddress(address: String, port: Int)
    fun mediaMoblinkStreamerRestartTunnel(relayId: UUID)
    fun mediaSetZoomX(x: Float)
    fun mediaSetExposureBias(bias: Float)
    fun mediaSelectedFps(auto: Boolean)
    fun mediaError(error: Throwable)
}

class Media(val delegate: MediaDelegate) : ProcessorDelegate, SrtlaDelegate, AdaptiveBitrateDelegate,
    RistStreamDelegate, SrtStreamMoblinDelegate, SrtStreamOfficialDelegate, RtmpStreamDelegate,
    WhipStreamDelegate, MobcamStreamDelegate
{
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private val rtmpStreams = mutableListOf<RtmpStream>()
    private val rtmpStream: RtmpStream?
        get() = rtmpStreams.firstOrNull()

    private var srtStreamNew: SrtStreamMoblin? = null
    private var srtStreamOld: SrtStreamOfficial? = null
    private var ristStream: RistStream? = null
    private var whipStream: WhipStream? = null
    private var mobcamStream: MobcamStream? = null
    private var previewStreamHandler: PreviewStreamHandler? = null
    private var srtlaClient: SrtlaClient? = null
    var processor: Processor? = null
        private set
    private var srtTotalByteCount: Long = 0
    private var srtPreviousTotalByteCount: Long = 0
    private var srtTransportBitrate: Long = 0
    private var currentAudioLevel: Float = defaultAudioLevel
    private var numberOfAudioChannels: Int = 0
    private var audioSampleRate: Double = 0.0
    private var srtUrl: String = ""
    private var latency: Int = 2000
    private var experimental: Boolean = false
    private var overheadBandwidth: Int = 25
    private var maximumBandwidthFollowInput: Boolean = false
    private var adaptiveBitrate: AdaptiveBitrate? = null
    var srtDroppedPacketsTotal: Int = 0
    private var videoEncoderSettings = VideoEncoderSettings()
    private var audioEncoderSettings = AudioEncoderSettings()
    private var multiplier: Int = 0
    private var updateTickCount: Long = 0
    private var belaLinesAndActions: Pair<List<String>, List<String>>? = null
    private var srtConnected = false
    private var srtImplementation: SettingsStreamSrtImplementation = SettingsStreamSrtImplementation.moblin
    private var canvasSize: Size = Size(1920, 1080)
    private var limitAdaptiveBitrateByTransportBitrate: Boolean = true

    fun logStatistics() {
        srtlaClient?.logStatistics()
    }

    fun srtlaConnectionStatistics(): List<BondingConnection>? {
        return srtlaClient?.connectionStatistics()
    }

    fun ristBondingStatistics(): List<BondingConnection>? {
        return ristStream?.connectionStatistics()
    }

    fun setConnectionPriorities(connectionPriorities: SettingsStreamSrtConnectionPriorities) {
        srtlaClient?.setConnectionPriorities(connectionPriorities)
    }

    fun setAdaptiveBitrateSettings(settings: AdaptiveBitrateSettings) {
        adaptiveBitrate?.setSettings(settings)
    }

    fun stopAllNetStreams() {
        srtStopStream()
        rtmpStopStream()
        ristStopStream()
        whipStopStream()
        mobcamStopStream()
        stopPreviewStream()
        rtmpStreams.clear()
        srtStreamNew = null
        srtStreamOld = null
        ristStream = null
        whipStream = null
        mobcamStream = null
        processor = null
    }

    fun setNetStream(
        proto: SettingsStreamProtocol,
        portrait: Boolean,
        timecodesEnabled: Boolean,
        builtinAudioDelay: Double,
        attachDefaultAudio: Boolean,
        destinations: List<SettingsStreamMultiStreamingDestination>,
        srtImplementation: SettingsStreamSrtImplementation,
        limitAdaptiveBitrateByTransportBitrate: Boolean
    ) {
        this.srtImplementation = srtImplementation
        this.limitAdaptiveBitrateByTransportBitrate = limitAdaptiveBitrateByTransportBitrate
        processor?.stop()
        stopAllNetStreams()
        val processor = Processor(delegate = this)
        when (proto) {
            SettingsStreamProtocol.rtmp -> {
                rtmpStreams.add(RtmpStream(name = "Main",
                    processor = processor,
                    delegate = this,
                    queue = processorControlQueue.coroutineContext[CoroutineDispatcher]
                        ?: Dispatchers.Default))
                for (destination in destinations) {
                    if (!destination.enabled) {
                        continue
                    }
                    val rtmpStream = RtmpStream(name = destination.name,
                        processor = processor,
                        delegate = this,
                        queue = processorControlQueue.coroutineContext[CoroutineDispatcher]
                            ?: Dispatchers.Default)
                    rtmpStream.setUrl(destination.url)
                    rtmpStreams.add(rtmpStream)
                }
            }
            SettingsStreamProtocol.srt -> {
                when (srtImplementation) {
                    SettingsStreamSrtImplementation.moblin -> {
                        srtStreamNew = SrtStreamMoblin(
                            processor = processor,
                            timecodesEnabled = timecodesEnabled,
                            delegate = this
                        )
                    }
                    SettingsStreamSrtImplementation.official -> {
                        srtStreamOld = SrtStreamOfficial(
                            processor = processor,
                            timecodesEnabled = timecodesEnabled,
                            delegate = this
                        )
                    }
                }
            }
            SettingsStreamProtocol.rist -> {
                ristStream = RistStream(processor = processor, timecodesEnabled = timecodesEnabled, delegate = this)
            }
            SettingsStreamProtocol.whip -> {
                whipStream = WhipStream(delegate = this)
            }
            SettingsStreamProtocol.mobcam -> {
                mobcamStream = MobcamStream(delegate = this)
            }
        }
        this.processor = processor
        processor.setVideoOrientation(value = if (portrait) com.moblin.android.platform.avfoundation.AVCaptureVideoOrientation.portrait else com.moblin.android.platform.avfoundation.AVCaptureVideoOrientation.landscapeRight)
        if (attachDefaultAudio) {
            attachDefaultAudioDevice(builtinDelay = builtinAudioDelay)
        }
    }

    fun getAudioLevel(): Float {
        return currentAudioLevel
    }

    fun getNumberOfAudioChannels(): Int {
        return numberOfAudioChannels
    }

    fun getAudioSampleRate(): Double {
        return audioSampleRate
    }

    fun srtStartStream(
        isSrtla: Boolean,
        url: String,
        reconnectTime: Double,
        targetBitrate: Int,
        adaptiveBitrateAlgorithm: SettingsStreamSrtAdaptiveBitrateAlgorithm?,
        latency: Int,
        experimental: Boolean,
        overheadBandwidth: Int,
        maximumBandwidthFollowInput: Boolean,
        mpegtsPacketsPerPacket: Int,
        packetPadding: Boolean,
        networkInterfaceNames: List<SettingsNetworkInterfaceName>,
        connectionPriorities: SettingsStreamSrtConnectionPriorities,
        dnsLookupStrategy: SettingsDnsLookupStrategy
    ) {
        srtUrl = url
        srtInitStream(
            isSrtla = isSrtla,
            targetBitrate = targetBitrate,
            adaptiveBitrateAlgorithm = adaptiveBitrateAlgorithm,
            latency = latency,
            experimental = experimental,
            overheadBandwidth = overheadBandwidth,
            maximumBandwidthFollowInput = maximumBandwidthFollowInput,
            mpegtsPacketsPerPacket = mpegtsPacketsPerPacket,
            packetPadding = packetPadding,
            networkInterfaceNames = networkInterfaceNames,
            connectionPriorities = connectionPriorities
        )
        srtlaClient!!.start(uri = url, timeout = reconnectTime + 1, dnsLookupStrategy = dnsLookupStrategy)
        srtTransportBitrate = targetBitrate.toLong()
    }

    private fun srtInitStream(
        isSrtla: Boolean,
        targetBitrate: Int,
        adaptiveBitrateAlgorithm: SettingsStreamSrtAdaptiveBitrateAlgorithm?,
        latency: Int,
        experimental: Boolean,
        overheadBandwidth: Int,
        maximumBandwidthFollowInput: Boolean,
        mpegtsPacketsPerPacket: Int,
        packetPadding: Boolean,
        networkInterfaceNames: List<SettingsNetworkInterfaceName>,
        connectionPriorities: SettingsStreamSrtConnectionPriorities
    ) {
        srtConnected = false
        this.latency = latency
        this.experimental = experimental
        this.overheadBandwidth = overheadBandwidth
        this.maximumBandwidthFollowInput = maximumBandwidthFollowInput
        srtTotalByteCount = 0
        srtPreviousTotalByteCount = 0
        srtDroppedPacketsTotal = 0
        srtlaClient?.stop()
        srtlaClient = SrtlaClient(
            delegate = this,
            passThrough = !isSrtla,
            mpegtsPacketsPerPacket = mpegtsPacketsPerPacket,
            packetPadding = packetPadding,
            networkInterfaceNames = networkInterfaceNames,
            connectionPriorities = connectionPriorities,
            srtImplementation = srtImplementation
        )
        srtSetAdaptiveBitrateAlgorithm(
            targetBitrate = targetBitrate,
            adaptiveBitrateAlgorithm = adaptiveBitrateAlgorithm
        )
    }

    fun srtStopStream() {
        srtStreamNew?.close()
        srtStreamOld?.close()
        srtlaClient?.stop()
        srtlaClient = null
        adaptiveBitrate = null
    }

    fun addMoblink(host: String, port: Int, id: UUID, name: String) {
        srtlaClient?.addMoblink(host = host, port = port, id = id, name = name)
        ristStream?.addMoblink(endpoint = NWEndpoint.hostPort(host = NWEndpoint.Host(host), port = NWEndpoint.Port(port)), id = id, name = name)
    }

    fun removeMoblink(host: String, port: Int) {
        srtlaClient?.removeMoblink(host = host, port = port)
        ristStream?.removeMoblink(endpoint = NWEndpoint.hostPort(host = NWEndpoint.Host(host), port = NWEndpoint.Port(port)))
    }

    fun srtSetAdaptiveBitrateAlgorithm(
        targetBitrate: Int,
        adaptiveBitrateAlgorithm: SettingsStreamSrtAdaptiveBitrateAlgorithm?
    ) {
        when (adaptiveBitrateAlgorithm) {
            SettingsStreamSrtAdaptiveBitrateAlgorithm.fastIrl,
            SettingsStreamSrtAdaptiveBitrateAlgorithm.slowIrl,
            SettingsStreamSrtAdaptiveBitrateAlgorithm.customIrl -> {
                adaptiveBitrate = AdaptiveBitrateSrtFight(targetBitrate = targetBitrate, delegate = this)
                adaptiveBitrate?.setTargetBitrate(bitrate = targetBitrate)
            }
            SettingsStreamSrtAdaptiveBitrateAlgorithm.belabox -> {
                adaptiveBitrate = AdaptiveBitrateSrtBelabox(targetBitrate = targetBitrate, delegate = this)
                adaptiveBitrate?.setTargetBitrate(bitrate = targetBitrate)
            }
            null -> {
                adaptiveBitrate = null
            }
        }
    }

    fun setNetworkInterfaceNames(networkInterfaceNames: List<SettingsNetworkInterfaceName>) {
        srtlaClient?.setNetworkInterfaceNames(networkInterfaceNames)
    }

    fun getNumberOfDestinations(): Int {
        return if (rtmpStream != null) {
            rtmpStreams.count()
        } else {
            1
        }
    }

    fun updateAdaptiveBitrate(overlay: Boolean, relaxed: Boolean): Pair<List<String>, List<String>>? {
        updateTickCount += 1
        val is200MsTick = updateTickCount % 10 == 0L
        if (isSrtStreamActive()) {
            return updateAdaptiveBitrateSrt(overlay = overlay, relaxed = relaxed, is200MsTick = is200MsTick)
        } else if (is200MsTick) {
            val rtmpStream = this.rtmpStream
            val ristStream = this.ristStream
            if (rtmpStream != null) {
                return updateAdaptiveBitrateRtmp(overlay = overlay, rtmpStream = rtmpStream)
            } else if (ristStream != null) {
                return updateAdaptiveBitrateRist(overlay = overlay, ristStream = ristStream)
            }
        }
        return null
    }

    private fun updateAdaptiveBitrateSrt(
        overlay: Boolean,
        relaxed: Boolean,
        is200MsTick: Boolean
    ): Pair<List<String>, List<String>>? {
        if (!srtConnected) {
            return null
        }
        if (adaptiveBitrate is AdaptiveBitrateSrtBelabox) {
            return updateAdaptiveBitrateSrtBela(overlay = overlay, relaxed = relaxed, is200MsTick = is200MsTick)
        } else if (is200MsTick) {
            return updateAdaptiveBitrateSrtFight(overlay = overlay)
        } else {
            return null
        }
    }

    private fun getSrtStats(): SrtPerformanceData? {
        return srtStreamNew?.getPerformanceData() ?: srtStreamOld?.getPerformanceData()
    }

    private fun isSrtStreamActive(): Boolean {
        return srtStreamNew != null || srtStreamOld != null
    }

    private fun updateAdaptiveBitrateSrtBela(
        overlay: Boolean,
        relaxed: Boolean,
        is200MsTick: Boolean
    ): Pair<List<String>, List<String>>? {
        val stats = getSrtStats() ?: return null
        srtDroppedPacketsTotal = stats.pktSndDropTotal
        val adaptiveBitrate = this.adaptiveBitrate ?: return null
        val srtStreamOld = this.srtStreamOld
        val sndData: Int? = if (srtStreamOld != null) {
            srtStreamOld.getSndData()
        } else {
            stats.pktFlightSize
        }
        val sndDataValue = sndData ?: return null
        adaptiveBitrate.update(stats = StreamStats(
            rttMs = stats.msRtt,
            packetsInFlight = sndDataValue.toDouble(),
            transportBitrate = streamTransportBitrate(),
            latency = latency,
            mbpsSendRate = stats.mbpsSendRate,
            relaxed = relaxed
        ))
        if (overlay) {
            if (is200MsTick) {
                belaLinesAndActions = Pair(listOf(
                    "R: ${stats.pktRetransTotal} N: ${stats.pktRecvNakTotal} D: ${stats.pktSndDropTotal} E: $numberOfFailedEncodings",
                    "msRTT: ${stats.msRtt}",
                    "sndData: $sndDataValue",
                    "B: ${adaptiveBitrate.getCurrentBitrateInKbps()}",
                    "Bt: ${srtTransportBitrate / 1000}",
                ), adaptiveBitrate.getActionsTaken())
            }
        } else {
            belaLinesAndActions = null
        }
        return belaLinesAndActions
    }

    private fun updateAdaptiveBitrateSrtFight(overlay: Boolean): Pair<List<String>, List<String>>? {
        val stats = getSrtStats() ?: return null
        srtDroppedPacketsTotal = stats.pktSndDropTotal
        adaptiveBitrate?.update(stats = StreamStats(
            rttMs = stats.msRtt,
            packetsInFlight = stats.pktFlightSize.toDouble(),
            transportBitrate = streamTransportBitrate(),
            latency = latency,
            mbpsSendRate = stats.mbpsSendRate,
            relaxed = false
        ))
        if (!overlay) {
            return null
        }
        val adaptiveBitrate = this.adaptiveBitrate
        if (adaptiveBitrate != null) {
            return Pair(listOf(
                "R: ${stats.pktRetransTotal} N: ${stats.pktRecvNakTotal} D: ${stats.pktSndDropTotal} E: $numberOfFailedEncodings",
                "msRTT: ${stats.msRtt}",
                "pktFlightSize: ${stats.pktFlightSize}   ${adaptiveBitrate.getFastPif()}   ${adaptiveBitrate.getSmoothPif()}",
                "B: ${adaptiveBitrate.getCurrentBitrateInKbps()} /  ${adaptiveBitrate.getCurrentMaximumBitrateInKbps()}",
                "Bt: ${srtTransportBitrate / 1000}",
            ), adaptiveBitrate.getActionsTaken())
        } else {
            return Pair(listOf(
                "pktRetransTotal: ${stats.pktRetransTotal}",
                "pktRecvNAKTotal: ${stats.pktRecvNakTotal}",
                "pktSndDropTotal: ${stats.pktSndDropTotal}",
                "msRTT: ${stats.msRtt}",
                "pktFlightSize: ${stats.pktFlightSize}",
                "pktSndBuf: ${stats.pktSndBuf}",
                "Bt: ${srtTransportBitrate / 1000}",
            ), emptyList())
        }
    }

    private fun updateAdaptiveBitrateRtmp(overlay: Boolean, rtmpStream: RtmpStream): Pair<List<String>, List<String>>? {
        val stats = rtmpStream.info.stats.value
        adaptiveBitrate?.update(stats = StreamStats(
            rttMs = stats.rttMs,
            packetsInFlight = stats.packetsInFlight.toDouble(),
            transportBitrate = streamTransportBitrate(),
            latency = null,
            mbpsSendRate = null,
            relaxed = null
        ))
        if (!overlay) {
            return null
        }
        val adaptiveBitrate = this.adaptiveBitrate
        if (adaptiveBitrate != null) {
            return Pair(listOf(
                "rttMs: ${stats.rttMs}",
                "packetsInFlight: ${stats.packetsInFlight}   ${adaptiveBitrate.getFastPif()}   ${adaptiveBitrate.getSmoothPif()}",
                "B: ${adaptiveBitrate.getCurrentBitrateInKbps()} /  ${adaptiveBitrate.getCurrentMaximumBitrateInKbps()}",
            ), adaptiveBitrate.getActionsTaken())
        } else {
            return Pair(listOf(
                "rttMs: ${stats.rttMs}",
                "packetsInFlight: ${stats.packetsInFlight}",
            ), emptyList())
        }
    }

    private fun updateAdaptiveBitrateRist(overlay: Boolean, ristStream: RistStream): Pair<List<String>, List<String>>? {
        val stats = ristStream.getStats()
        var rtt = 1000.0
        for (stat in stats) {
            rtt = minOf(rtt, stat.rtt.toDouble())
        }
        adaptiveBitrate?.update(stats = StreamStats(
            rttMs = rtt,
            packetsInFlight = 10.0,
            transportBitrate = null,
            latency = null,
            mbpsSendRate = null,
            relaxed = false
        ))
        ristStream.updateConnectionsWeights()
        if (!overlay) {
            return null
        }
        val adaptiveBitrate = this.adaptiveBitrate
        if (adaptiveBitrate != null) {
            return Pair(listOf(
                "rttMs: $rtt",
                "${adaptiveBitrate.getFastPif()}   ${adaptiveBitrate.getSmoothPif()}",
                "B: ${adaptiveBitrate.getCurrentBitrateInKbps()} /  ${adaptiveBitrate.getCurrentMaximumBitrateInKbps()}",
            ), adaptiveBitrate.getActionsTaken())
        } else {
            return Pair(listOf(
                "rttMs: $rtt",
            ), emptyList())
        }
    }

    fun updateSrtTransportBitrate() {
        srtTotalByteCount = srtlaClient?.getTotalByteCount() ?: 0
        val byteCount = maxOf(srtTotalByteCount - srtPreviousTotalByteCount, 0L)
        srtTransportBitrate = (srtTransportBitrate.toDouble() * 0.7 + (8 * byteCount).toDouble() * 0.3).toLong()
        srtPreviousTotalByteCount = srtTotalByteCount
    }

    fun streamTransportBitrate(): Long? {
        if (!limitAdaptiveBitrateByTransportBitrate) {
            return null
        }
        val rtmpStream = this.rtmpStream
        val ristStream = this.ristStream
        val whipStream = this.whipStream
        val mobcamStream = this.mobcamStream
        if (rtmpStream != null) {
            return rtmpStream.info.bitrateStats.value.latestSpeed.toLong() * 8
        } else if (isSrtStreamActive()) {
            return srtTransportBitrate
        } else if (ristStream != null) {
            return (ristStream.getSpeed()).toLong()
        } else if (whipStream != null) {
            return 0
        } else if (mobcamStream != null) {
            return mobcamStream.getSpeed().toLong()
        } else {
            return 0
        }
    }

    fun streamTotal(): Long {
        var total: Long = 0
        for (stream in rtmpStreams) {
            total += stream.info.bitrateStats.value.totalBytes.toLong()
        }
        if (isSrtStreamActive()) {
            return srtTotalByteCount
        }
        val ristStream = this.ristStream
        if (ristStream != null) {
            return ristStream.getTotalByteCount()
        }
        val whipStream = this.whipStream
        if (whipStream != null) {
            return whipStream.getTotalByteCount()
        }
        val mobcamStream = this.mobcamStream
        if (mobcamStream != null) {
            return mobcamStream.getTotalByteCount()
        }
        return total
    }

    private fun queryContains(uri: Uri, name: String): Boolean {
        return uri.getQueryParameter(name) != null
    }

    fun makeLocalhostSrtUrl(
        url: String,
        port: Int,
        latency: Int,
        overheadBandwidth: Int,
        maximumBandwidthFollowInput: Boolean
    ): String? {
        val parsed = Uri.parse(url)
        val builder = Uri.parse("srt://localhost:$port").buildUpon()
        parsed.encodedQuery?.let { builder.encodedQuery(it) }
        if (!queryContains(parsed, "latency")) {
            Log.i("Media", "Setting SRT latency to $latency")
            builder.appendQueryParameter("latency", latency.toString())
        }
        if (!queryContains(parsed, "maxbw")) {
            if (maximumBandwidthFollowInput) {
                Log.i("Media", "Setting SRT maxbw to 0 (follows input)")
                builder.appendQueryParameter("maxbw", "0")
            }
        }
        if (!queryContains(parsed, "oheadbw")) {
            Log.i("Media", "Setting SRT oheadbw to $overheadBandwidth")
            builder.appendQueryParameter("oheadbw", overheadBandwidth.toString())
        }
        return builder.build().toString()
    }

    fun rtmpStartStream(
        url: String,
        targetBitrate: Int,
        adaptiveBitrateEnabled: Boolean
    ) {
        if (adaptiveBitrateEnabled) {
            adaptiveBitrate = AdaptiveBitrateSrtFight(targetBitrate = targetBitrate, delegate = this, rttMax = 500.0, pifMax = 100.0)
            adaptiveBitrate?.setTargetBitrate(bitrate = targetBitrate)
        } else {
            adaptiveBitrate = null
        }
        rtmpStream?.setUrl(url)
        for (rtmpStream in rtmpStreams) {
            rtmpStream.connect()
        }
    }

    fun rtmpStopStream() {
        for (rtmpStream in rtmpStreams) {
            rtmpStream.disconnect()
        }
        adaptiveBitrate = null
    }

    fun ristStartStream(
        url: String,
        bonding: Boolean,
        targetBitrate: Int,
        adaptiveBitrateEnabled: Boolean
    ) {
        if (adaptiveBitrateEnabled) {
            adaptiveBitrate = AdaptiveBitrateRistExperiment(targetBitrate = targetBitrate, delegate = this)
            adaptiveBitrate?.setTargetBitrate(bitrate = targetBitrate)
        } else {
            adaptiveBitrate = null
        }
        ristStream?.start(url = url, bonding = bonding)
    }

    fun ristStopStream() {
        ristStream?.stop()
    }

    fun whipStartStream(
        url: String,
        headers: List<SettingsHttpHeader>,
        videoCodec: SettingsStreamCodec,
        audioCodec: SettingsStreamAudioCodec,
        videoBitrate: Double
    ) {
        adaptiveBitrate = null
        whipStream?.start(url = url,
            headers = headers,
            iceServers = listOf(defaultStunServer),
            videoCodec = videoCodec,
            audioCodec = audioCodec,
            videoBitrate = videoBitrate)
    }

    fun whipStopStream() {
        whipStream?.stop()
    }

    fun mobcamStartStream(port: Int, deviceName: String) {
        adaptiveBitrate = null
        setAllowFrameReordering(value = false)
        mobcamStream?.start(port = port, deviceName = deviceName)
    }

    fun mobcamStopStream() {
        mobcamStream?.stop()
    }

    fun startPreviewStream(url: String, resolution: SettingsStreamResolution, bitrate: Int) {
        previewStreamHandler?.stop()
        previewStreamHandler = PreviewStreamHandler(media = this,
            url = url,
            resolution = resolution,
            bitrate = bitrate)
        previewStreamHandler?.start()
    }

    fun stopPreviewStream() {
        previewStreamHandler?.stop()
        previewStreamHandler = null
    }

    fun setTorch(on: Boolean) {
        processor?.setTorch(value = on)
    }

    fun setTorchLevel(level: Float) {
        processor?.setTorchLevel(value = level)
    }

    fun setMute(on: Boolean) {
        processor?.setHasAudio(value = !on)
    }

    fun setAudioGain(gain: Float) {
        processor?.setAudioGain(gain = gain)
    }

    fun setAudioDelay(delay: Double) {
        processor?.setAudioDelay(delay = delay)
    }

    fun registerEffect(effect: VideoEffect) {
        processor?.registerVideoEffect(effect)
    }

    fun registerEffectBack(effect: VideoEffect) {
        processor?.registerVideoEffectBack(effect)
    }

    fun unregisterEffect(effect: VideoEffect) {
        processor?.unregisterVideoEffect(effect)
    }

    fun unregisterAllEffects() {
        processor?.unregisterAllVideoEffects()
    }

    fun setPendingAfterAttachEffects(effects: List<VideoEffect>, rotation: Double, mirror: Boolean) {
        processor?.setPendingAfterAttachEffects(effects = effects, rotation = rotation, mirror = mirror)
    }

    fun usePendingAfterAttachEffects() {
        processor?.usePendingAfterAttachEffects()
    }

    fun setScreenPreview(enabled: Boolean) {
        processor?.setScreenPreview(enabled = enabled)
    }

    fun setShowCameraPreview(show: Boolean) {
        processor?.setShowCameraPreview(show)
    }

    fun setVideoPreviewEnabled(enabled: Boolean) {
        processor?.setVideoPreviewEnabled(enabled = enabled)
    }

    fun setVideoPreview(cameraId: UUID, drawable: PreviewView) {
        processor?.setVideoPreview(cameraId = cameraId, drawable = drawable)
    }

    fun removeAllVideoPreviews() {
        processor?.removeAllVideoPreviews()
    }

    fun setLowFpsImage(fps: Float) {
        processor?.setLowFpsImage(fps = fps)
    }

    fun setSceneSwitchTransition(sceneSwitchTransition: SceneSwitchTransition) {
        processor?.setSceneSwitchTransition(sceneSwitchTransition = sceneSwitchTransition)
    }

    fun setCameraControls(enabled: Boolean) {
        processor?.setCameraControls(enabled = enabled)
    }

    fun takeSnapshot(age: Float, onComplete: (Bitmap, Bitmap, Bitmap) -> Unit) {
        processor?.takeSnapshot(age = age, onComplete = onComplete)
    }

    fun takePhoto() {
        processor?.takePhoto()
    }

    fun takeVideoSourceSnapshot(videoSourceId: UUID,
                                onComplete: (Bitmap?) -> Unit)
    {
        processor?.takeVideoSourceSnapshot(videoSourceId = videoSourceId, onComplete = onComplete)
    }

    fun setCleanRecordings(enabled: Boolean) {
        processor?.setCleanRecordings(enabled = enabled)
    }

    fun setCleanSnapshots(enabled: Boolean) {
        processor?.setCleanSnapshots(enabled = enabled)
    }

    fun setCleanExternalDisplay(enabled: Boolean) {
        processor?.setCleanExternalDisplay(enabled = enabled)
    }

    fun setVideoSize(capture: Size, canvas: Size, stream: Size) {
        processor?.setVideoSize(capture = capture, canvas = canvas)
        videoEncoderSettings.videoSize = stream
        commitVideoEncoderSettings()
        canvasSize = canvas
    }

    fun getCanvasSize(): Size {
        return canvasSize
    }

    fun setFps(fps: Int, preferAutoFps: Boolean) {
        processor?.setFps(value = fps.toDouble(), preferAutoFps = preferAutoFps)
    }

    fun setColorSpace(colorSpace: Int, onComplete: () -> Unit) {
        processor?.setColorSpace(colorSpace = colorSpace, onComplete = onComplete)
    }

    private fun commitVideoEncoderSettings() {
        processor?.setVideoEncoderSettings(settings = videoEncoderSettings)
    }

    private fun commitAudioEncoderSettings() {
        processor?.setAudioEncoderSettings(settings = audioEncoderSettings)
    }

    fun updateVideoStreamBitrate(bitrate: Int) {
        multiplier = multiplier xor 1
        val bitRate = getVideoStreamBitrate(bitrate = bitrate)
        videoEncoderSettings.bitrate = bitRate + multiplier * (bitRate / 10)
        commitVideoEncoderSettings()
    }

    fun getVideoStreamBitrate(bitrate: Int): Int {
        val adaptiveBitrate = this.adaptiveBitrate
        return if (adaptiveBitrate != null) {
            adaptiveBitrate.getCurrentBitrate()
        } else {
            bitrate
        }
    }

    fun setVideoStreamBitrate(bitrate: Int) {
        val adaptiveBitrate = this.adaptiveBitrate
        if (adaptiveBitrate != null) {
            adaptiveBitrate.setTargetBitrate(bitrate = bitrate)
        } else {
            videoEncoderSettings.bitrate = bitrate
            commitVideoEncoderSettings()
        }
    }

    fun setVideoStreamRateControl(rateControl: SettingsStreamRateControl) {
        when (rateControl) {
            SettingsStreamRateControl.abr -> {
                videoEncoderSettings.rateControl = VideoEncoderSettings.RateControl.abr
            }
            SettingsStreamRateControl.cbr -> {
                videoEncoderSettings.rateControl = VideoEncoderSettings.RateControl.cbr
            }
            SettingsStreamRateControl.vbr -> {
                videoEncoderSettings.rateControl = VideoEncoderSettings.RateControl.vbr
            }
        }
        commitVideoEncoderSettings()
    }

    fun setVideoProfile(profile: String) {
        videoEncoderSettings.profileLevel = profile
        commitVideoEncoderSettings()
    }

    fun setAllowFrameReordering(value: Boolean) {
        videoEncoderSettings.allowFrameReordering = value
        commitVideoEncoderSettings()
    }

    fun setStreamKeyFrameInterval(seconds: Int) {
        videoEncoderSettings.maxKeyFrameIntervalDuration = seconds
        commitVideoEncoderSettings()
    }

    fun setStreamAdaptiveResolution(value: Boolean, thresholdsFactor: Double) {
        videoEncoderSettings.adaptiveResolution = value
        videoEncoderSettings.updateAdtaptiveResolutionThresholds(factor = thresholdsFactor)
        commitVideoEncoderSettings()
    }

    fun setAudioStreamBitrate(bitrate: Int) {
        audioEncoderSettings.bitrate = bitrate
        commitAudioEncoderSettings()
    }

    fun setAudioStreamFormat(format: AudioEncoderSettings.Format) {
        audioEncoderSettings.format = format
        commitAudioEncoderSettings()
    }

    fun setAudioChannelsMap(channelsMap: Map<Int, Int>) {
        audioEncoderSettings.channelsMap = channelsMap.toMutableMap()
        commitAudioEncoderSettings()
        processor?.setAudioChannelsMap(map = channelsMap)
    }

    fun setSpeechToText(enabled: Boolean) {
        processor?.setSpeechToText(enabled = enabled)
    }

    fun setTalkback(cameraId: UUID?) {
        processor?.setTalkback(cameraId = cameraId)
    }

    fun setVideoOrientation(value: Int) {
        processor?.setVideoOrientation(value = value)
    }

    fun setGraphicsImplementation(value: SettingsGraphicsImplementation) {
        processor?.setGraphicsImplementation(value = value)
    }

    fun setCameraZoomLevel(device: CaptureDevice?, level: Float, rate: Float?): Float? {
        if (device == null) {
            Log.i("Media", "Device not ready to zoom")
            return null
        }
        return com.moblin.android.platform.avfoundation.setCameraZoomLevel(device.device as AVCaptureDevice, level, rate)
    }

    fun stopCameraZoomLevel(device: CaptureDevice?): Float? {
        if (device == null) {
            Log.i("Media", "Device not ready to zoom")
            return null
        }
        return com.moblin.android.platform.avfoundation.stopCameraZoomLevel(device.device as AVCaptureDevice)
    }

    fun attachCamera(params: VideoUnitAttachParams, onSuccess: (() -> Unit)? = null) {
        processor?.attachCamera(
            params = params,
            onError = { error ->
                delegate.mediaError(error)
            },
            onSuccess = {
                onSuccess?.invoke()
            }
        )
    }

    fun attachBufferedCamera(
        devices: CaptureDevices,
        builtinDelay: Double,
        cameraPreviewLayers: Map<UUID, Any>,
        attachCameraPreview: Boolean,
        showCameraPreview: Boolean,
        externalDisplayPreview: Boolean,
        cameraId: UUID,
        preferredVideoStabilizationMode: Int,
        ignoreFramesAfterAttachSeconds: Double,
        fillFrame: Boolean,
        isLandscapeStreamAndPortraitUi: Boolean,
        forceSceneTransition: Boolean,
        macScreenCapture: Boolean,
        attachPhotoShoot: Boolean
    ) {
        val params = VideoUnitAttachParams(devices = devices,
            builtinDelay = builtinDelay,
            cameraPreviewLayers = cameraPreviewLayers,
            attachCameraPreview = attachCameraPreview,
            showCameraPreview = showCameraPreview,
            externalDisplayPreview = externalDisplayPreview,
            bufferedVideo = cameraId,
            preferredVideoStabilizationMode = preferredVideoStabilizationMode,
            ignoreFramesAfterAttachSeconds = ignoreFramesAfterAttachSeconds,
            fillFrame = fillFrame,
            isLandscapeStreamAndPortraitUi = isLandscapeStreamAndPortraitUi,
            forceSceneTransition = forceSceneTransition,
            macScreenCapture = macScreenCapture,
            attachPhotoShoot = attachPhotoShoot)
        processor?.attachCamera(params = params)
    }

    fun attachBufferedAudio(cameraId: UUID?) {
        val params = AudioUnitAttachParams(device = null,
            builtinDelay = 0.0,
            bufferedAudio = cameraId)
        processor?.attachAudio(params = params)
    }

    fun addBufferedAudio(cameraId: UUID, name: String, latency: Double) {
        processor?.addBufferedAudio(cameraId = cameraId, name = name, latency = latency)
    }

    fun removeBufferedAudio(cameraId: UUID) {
        processor?.removeBufferedAudio(cameraId = cameraId)
    }

    fun appendBufferedAudioSampleBuffer(cameraId: UUID, sampleBuffer: MediaSample) {
        processor?.appendBufferedAudioSampleBuffer(cameraId = cameraId, sampleBuffer = sampleBuffer)
    }

    fun addBufferedVideo(cameraId: UUID, name: String, latency: Double) {
        processor?.addBufferedVideo(cameraId = cameraId, name = name, latency = latency)
    }

    fun removeBufferedVideo(cameraId: UUID) {
        processor?.removeBufferedVideo(cameraId = cameraId)
    }

    fun appendBufferedVideoSampleBuffer(cameraId: UUID, sampleBuffer: MediaSample) {
        processor?.appendBufferedVideoSampleBuffer(cameraId = cameraId, sampleBuffer = sampleBuffer)
    }

    fun attachDefaultAudioDevice(builtinDelay: Double) {
        val params = AudioUnitAttachParams(
            device = AVCaptureDevice.default(com.moblin.android.platform.avfoundation.AVMediaType.audio),
            builtinDelay = builtinDelay,
            bufferedAudio = null
        )
        processor?.attachAudio(params = params) { error ->
            delegate.mediaError(error)
        }
    }

    fun startRecording(
        url: String?,
        replay: Boolean,
        videoCodec: SettingsStreamCodec,
        videoBitrate: Int?,
        keyFrameInterval: Int?,
        audioBitrate: Int?
    ) {
        processor?.startRecording(url = url?.let { URI.create(it) },
            replay = replay,
            audioSettings = makeAudioCompressionSettings(audioBitrate = audioBitrate),
            videoSettings = makeVideoCompressionSettings(
                videoCodec = videoCodec,
                videoBitrate = videoBitrate,
                keyFrameInterval = keyFrameInterval
            ))
    }

    fun setRecordUrl(url: String?) {
        processor?.setUrl(url = url?.let { URI.create(it) })
    }

    fun setReplayBuffering(enabled: Boolean) {
        processor?.setReplayBuffering(enabled = enabled)
    }

    private fun makeVideoCompressionSettings(videoCodec: SettingsStreamCodec,
                                             videoBitrate: Int?,
                                             keyFrameInterval: Int?): Map<String, Any>
    {
        val codec = when (videoCodec) {
            SettingsStreamCodec.h264avc -> android.media.MediaFormat.MIMETYPE_VIDEO_AVC
            SettingsStreamCodec.h265hevc -> android.media.MediaFormat.MIMETYPE_VIDEO_HEVC
        }
        val settings = mutableMapOf<String, Any>(
            android.media.MediaFormat.KEY_MIME to codec,
            android.media.MediaFormat.KEY_WIDTH to 0,
            android.media.MediaFormat.KEY_HEIGHT to 0,
        )
        val compressionProperties = mutableMapOf<String, Any>()
        if (videoBitrate != null) {
            compressionProperties[android.media.MediaFormat.KEY_BIT_RATE] = videoBitrate
        }
        if (keyFrameInterval != null) {
            compressionProperties[android.media.MediaFormat.KEY_I_FRAME_INTERVAL] = keyFrameInterval
        }
        if (compressionProperties.isNotEmpty()) {
            settings["compressionProperties"] = compressionProperties
        }
        return settings
    }

    private fun makeAudioCompressionSettings(audioBitrate: Int?): Map<String, Any> {
        val settings = mutableMapOf<String, Any>(
            android.media.MediaFormat.KEY_MIME to android.media.MediaFormat.MIMETYPE_AUDIO_AAC,
            android.media.MediaFormat.KEY_SAMPLE_RATE to 48000,
            android.media.MediaFormat.KEY_CHANNEL_COUNT to 0,
        )
        if (audioBitrate != null) {
            settings[android.media.MediaFormat.KEY_BIT_RATE] = audioBitrate
        }
        return settings
    }

    fun stopRecording() {
        processor?.stopRecording()
    }

    override fun streamAudioLevel(audioLevel: Float, numberOfAudioChannels: Int, sampleRate: Double) {
        mainScope.launch {
            this@Media.currentAudioLevel = audioLevel
            this@Media.numberOfAudioChannels = numberOfAudioChannels
            this@Media.audioSampleRate = sampleRate
        }
    }

    override fun streamLowFpsImage(lowFpsImage: ByteArray?, frameNumber: Long) {
        delegate.mediaOnLowFpsImage(lowFpsImage, frameNumber)
    }

    override fun streamVideoAttachCameraError() {
        delegate.mediaOnAttachCameraError()
    }

    override fun streamVideoCaptureSessionError(message: String) {
        delegate.mediaOnCaptureSessionError(message)
    }

    override fun streamVideoBufferedVideoReady(cameraId: UUID) {
        delegate.mediaOnBufferedVideoReady(cameraId = cameraId)
    }

    override fun streamVideoBufferedVideoRemoved(cameraId: UUID) {
        delegate.mediaOnBufferedVideoRemoved(cameraId = cameraId)
    }

    override fun streamVideoEncoderResolution(resolution: Size) {
        delegate.mediaOnEncoderResolutionChanged(resolution = resolution)
    }

    override fun streamAudio(sampleBuffer: MediaSample) {
        delegate.mediaOnAudioBuffer(sampleBuffer)
    }

    override fun streamRecorderInitSegment(data: ByteArray) {
        delegate.mediaOnRecorderInitSegment(data = data)
    }

    override fun streamRecorderDataSegment(segment: RecorderDataSegment) {
        delegate.mediaOnRecorderDataSegment(segment = segment)
    }

    override fun streamRecorderFinished() {
        delegate.mediaOnRecorderFinished()
    }

    override fun streamNoTorch() {
        delegate.mediaOnNoTorch()
    }

    override fun streamVideoFps(fps: Int) {
        delegate.mediaOnFps(fps = fps)
    }

    override fun streamSetZoomX(x: Float) {
        delegate.mediaSetZoomX(x = x)
    }

    override fun streamSetExposureBias(bias: Float) {
        delegate.mediaSetExposureBias(bias = bias)
    }

    override fun streamSelectedFps(auto: Boolean) {
        delegate.mediaSelectedFps(auto = auto)
    }

    override fun srtlaReady(port: Int) {
        processorControlQueue.launch {
            val srtStreamOld = this@Media.srtStreamOld
            if (srtStreamOld != null) {
                try {
                    srtStreamOld.open(
                        uri = makeLocalhostSrtUrl(
                            url = srtUrl,
                            port = port,
                            latency = latency,
                            overheadBandwidth = overheadBandwidth,
                            maximumBandwidthFollowInput = maximumBandwidthFollowInput
                        )?.let { URI.create(it) },
                        sendHook = { data ->
                            val srtla = this@Media.srtlaClient
                            if (srtla != null) {
                                CoroutineScope(srtlaClientQueue).launch {
                                    srtla.handleLocalPacket(packet = data)
                                }
                            }
                            true
                        }
                    )
                    mainScope.launch {
                        this@Media.srtConnected = true
                        this@Media.delegate.mediaOnSrtConnected()
                    }
                } catch (error: Throwable) {
                    val message = localized("SRT connect failed with: $error")
                    mainScope.launch {
                        this@Media.delegate.mediaOnSrtDisconnected(message)
                    }
                }
            } else {
                srtStreamNew?.open(
                    streamId = extractSrtStreamId(url = srtUrl),
                    latency = latency.coerceIn(0, 65535).toUShort(),
                    experimental = experimental
                )
            }
        }
    }

    override fun srtlaError(message: String) {
        Log.i("Media", "stream: SRT error: $message")
        delegate.mediaOnSrtDisconnected(localized("SRT error: $message"))
    }

    override fun srtlaReceivedPacket(packet: ByteArray) {
        srtStreamNew?.inputPacket(packet = packet)
    }

    override fun moblinkStreamerDestinationAddress(address: String, port: Int) {
        delegate.mediaMoblinkStreamerDestinationAddress(address = address, port = port)
    }

    override fun moblinkStreamerRestartTunnel(relayId: UUID) {
        delegate.mediaMoblinkStreamerRestartTunnel(relayId = relayId)
    }

    override fun adaptiveBitrateSetVideoStreamBitrate(bitrate: Int) {
        videoEncoderSettings.bitrate = bitrate
        commitVideoEncoderSettings()
    }

    override fun ristStreamOnConnected() {
        delegate.mediaOnRistConnected()
    }

    override fun ristStreamOnDisconnected() {
        delegate.mediaOnRistDisconnected()
    }

    override fun ristStreamRelayDestinationAddress(address: String, port: Int) {
        delegate.mediaMoblinkStreamerDestinationAddress(address = address, port = port)
    }

    override fun srtStreamMoblinConnected() {
        mainScope.launch {
            this@Media.srtConnected = true
            this@Media.delegate.mediaOnSrtConnected()
        }
    }

    override fun srtStreamMoblinDisconnected() {
        mainScope.launch {
            this@Media.srtConnected = false
        }
        srtlaError(localized("SRT disconnected"))
    }

    override fun srtStreamMoblinOutput(packet: ByteArray) {
        srtlaClient?.handleLocalPacket(packet = packet)
    }

    override fun srtStreamOfficialError() {
        mainScope.launch {
            this@Media.srtConnected = false
        }
        srtlaError(localized("SRT disconnected"))
    }

    override fun rtmpStreamStatus(rtmpStream: RtmpStream, code: String) {
        mainScope.launch {
            when (RtmpConnectionCode.fromRawValue(code)) {
                RtmpConnectionCode.connectFailed, RtmpConnectionCode.connectClosed -> {
                    if (rtmpStream === this@Media.rtmpStream) {
                        this@Media.delegate.mediaOnRtmpDisconnected("$code")
                    } else {
                        this@Media.delegate.mediaOnRtmpDestinationDisconnected(rtmpStream.name)
                        rtmpStream.reconnectSoon()
                    }
                }
                else -> {
                }
            }
        }
    }

    override fun rtmpStreamConnected(rtmpStream: RtmpStream) {
        mainScope.launch {
            if (rtmpStream === this@Media.rtmpStream) {
                this@Media.delegate.mediaOnRtmpConnected()
            } else {
                this@Media.delegate.mediaOnRtmpDestinationConnected(rtmpStream.name)
            }
        }
    }

    override fun whipStreamOnConnected() {
        delegate.mediaOnWhipConnected()
    }

    override fun whipStreamOnDisconnected(reason: String) {
        delegate.mediaOnWhipDisconnected(reason)
    }

    override fun whipStreamPerform(
        request: Request,
        queue: CoroutineDispatcher,
        completion: ((ByteArray?, Response?, Throwable?) -> Unit)?
    ) {
        delegate.mediaOnWhipPerform(request = request, queue = queue, completion = completion)
    }

    override fun whipStreamStartEncoding(
        audioDelegate: AudioEncoderDelegate,
        videoDelegate: VideoEncoderDelegate
    ) {
        val delegate = (audioDelegate as? AudioVideoEncoderDelegate)
            ?: (videoDelegate as AudioVideoEncoderDelegate)
        processorPipelineQueue.launch {
            this@Media.processor?.startEncoding(delegate)
        }
    }

    override fun whipStreamStopEncoding(
        audioDelegate: AudioEncoderDelegate,
        videoDelegate: VideoEncoderDelegate
    ) {
        val delegate = (audioDelegate as? AudioVideoEncoderDelegate)
            ?: (videoDelegate as AudioVideoEncoderDelegate)
        processorPipelineQueue.launch {
            this@Media.processor?.stopEncoding(delegate)
        }
    }

    override fun mobcamStreamOnConnected() {
        delegate.mediaOnMobcamConnected()
    }

    override fun mobcamStreamOnDisconnected(reason: String) {
        delegate.mediaOnMobcamDisconnected(reason)
    }

    override fun <T> mobcamStreamStartEncoding(delegate: T)
        where T : AudioEncoderDelegate, T : VideoEncoderDelegate
    {
        processorPipelineQueue.launch {
            this@Media.processor?.startEncoding(delegate as AudioVideoEncoderDelegate)
        }
    }

    override fun <T> mobcamStreamStopEncoding(delegate: T)
        where T : AudioEncoderDelegate, T : VideoEncoderDelegate
    {
        processorPipelineQueue.launch {
            this@Media.processor?.stopEncoding(delegate as AudioVideoEncoderDelegate)
        }
    }
}

private class PreviewStreamHandler(
    private val media: Media,
    private val url: String,
    private val resolution: SettingsStreamResolution,
    private val bitrate: Int
) : WhipStreamDelegate {
    private var previewStream: WhipStream? = null
    private val reconnectTimer = SimpleTimer(Dispatchers.Main)

    fun start() {
        stop()
        previewStream = WhipStream(delegate = this)
        previewStream?.start(
            url = url,
            headers = listOf(),
            iceServers = listOf(defaultStunServer),
            videoCodec = SettingsStreamCodec.h264avc,
            audioCodec = SettingsStreamAudioCodec.opus,
            videoBitrate = bitrate.toDouble()
        )
    }

    fun stop() {
        reconnectTimer.stop()
        previewStream?.stop()
        previewStream = null
    }

    private fun reconnectSoon(reason: String) {
        reconnectTimer.startSingleShot(5.0) {
            Log.i("Media", "preview-stream: Reconnecting due to: $reason")
            start()
        }
    }

    override fun whipStreamOnConnected() {
    }

    override fun whipStreamOnDisconnected(reason: String) {
        CoroutineScope(Dispatchers.Main).launch {
            reconnectSoon(reason)
        }
    }

    override fun whipStreamPerform(
        request: Request,
        queue: CoroutineDispatcher,
        completion: ((ByteArray?, Response?, Throwable?) -> Unit)?
    ) {
        media.delegate.mediaOnWhipPerform(request = request, queue = queue, completion = completion)
    }

    override fun whipStreamStartEncoding(
        audioDelegate: AudioEncoderDelegate,
        videoDelegate: VideoEncoderDelegate
    ) {
        val videoSettings = VideoEncoderSettings()
        videoSettings.videoSize = resolution.dimensions(portrait = false)
        videoSettings.bitrate = bitrate
        videoSettings.profileLevel = "H264_Baseline_AutoLevel"
        val audioSettings = AudioEncoderSettings()
        audioSettings.bitrate = 64000
        audioSettings.format = AudioEncoderSettings.Format.opus
        val delegate = (audioDelegate as? AudioVideoEncoderDelegate)
            ?: (videoDelegate as AudioVideoEncoderDelegate)
        media.processor?.startPreviewEncoding(delegate, videoSettings, audioSettings)
    }

    override fun whipStreamStopEncoding(
        audioDelegate: AudioEncoderDelegate,
        videoDelegate: VideoEncoderDelegate
    ) {
        media.processor?.stopPreviewEncoding()
    }
}

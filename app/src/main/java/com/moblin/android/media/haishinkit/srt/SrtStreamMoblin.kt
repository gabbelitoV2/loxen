package com.moblin.android.media.haishinkit.srt

import com.moblin.android.media.haishinkit.media.AudioVideoEncoderDelegate
import com.moblin.android.media.haishinkit.media.Processor
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.mpeg.MpegTsWriter
import com.moblin.android.media.haishinkit.mpeg.MpegTsWriterDelegate
import com.moblin.android.media.haishinkit.mpeg.payloadSize
import com.moblin.android.media.srtla.client.srtlaClientQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

interface SrtStreamMoblinDelegate {
    fun srtStreamMoblinConnected()
    fun srtStreamMoblinDisconnected()
    fun srtStreamMoblinOutput(packet: ByteArray)
}

class SrtStreamMoblin : MpegTsWriterDelegate, SrtSenderDelegate {
    private val writer: MpegTsWriter
    private val delegate: SrtStreamMoblinDelegate
    private val processor: Processor
    private var srtSender: SrtSender? = null

    constructor(processor: Processor, timecodesEnabled: Boolean, delegate: SrtStreamMoblinDelegate) {
        this.processor = processor
        this.writer = MpegTsWriter(timecodesEnabled = timecodesEnabled, newSrt = true)
        this.delegate = delegate
        this.writer.delegate = this
    }

    fun open(streamId: String?, latency: UShort, experimental: Boolean) {
        srtSender = SrtSender(streamId = streamId, latency = latency, experimental = experimental)
        srtSender?.delegate = this
        srtSender?.start()
    }

    fun close() {
        srtSender?.stop()
        srtSender = null
    }

    fun inputPacket(packet: ByteArray) {
        srtSender?.input(packet = packet)
    }

    fun getPerformanceData(): SrtPerformanceData? {
        return srtSender?.getPerformanceData()
    }

    private fun write(data: ByteArray, containsAudio: Boolean) {
        val srtSender = srtSender ?: return
        val now = System.nanoTime()
        var offset = 0
        while (offset < data.size) {
            val length = minOf(payloadSize, data.size - offset)
            val payload = data.copyOfRange(offset, offset + length)
            val packet = srtSender.newDataPacket(payload = payload)
            packet.containsAudio = containsAudio
            srtSender.enqueue(packet = packet, now = now)
            offset += payloadSize
        }
        srtSender.send(now = now)
    }

    override fun writer(writer: MpegTsWriter, doOutput: ByteArray, containsAudio: Boolean) {
        CoroutineScope(srtlaClientQueue).launch {
            this@SrtStreamMoblin.write(data = doOutput, containsAudio = containsAudio)
        }
    }

    override fun writer(writer: MpegTsWriter, doOutputPointer: ByteArray, count: Int) {
    }

    override fun srtSenderConnected() {
        processorPipelineQueue.launch {
            processor.startEncoding(writer as AudioVideoEncoderDelegate)
            writer.startRunning()
            delegate.srtStreamMoblinConnected()
        }
    }

    override fun srtSenderDisconnected() {
        processorPipelineQueue.launch {
            writer.stopRunning()
            processor.stopEncoding(writer as AudioVideoEncoderDelegate)
            delegate.srtStreamMoblinDisconnected()
        }
    }

    override fun srtSenderOutput(packet: ByteArray) {
        delegate.srtStreamMoblinOutput(packet = packet)
    }
}

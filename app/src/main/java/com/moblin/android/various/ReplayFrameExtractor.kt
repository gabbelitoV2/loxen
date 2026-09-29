package com.moblin.android.various

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import com.moblin.android.media.haishinkit.media.RecorderDataSegment
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch

private val replayQueue: CoroutineScope = CoroutineScope(
    Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "com.eerimoq.replay").apply { isDaemon = true }
    }.asCoroutineDispatcher() + SupervisorJob()
)

internal class ReplayBufferFile(val url: String, val duration: Double, private val remove: Boolean) {
    private var destroyed = false

    @Synchronized
    fun destroy() {
        if (destroyed) {
            return
        }
        destroyed = true
        if (remove) {
            runCatching { File(url).delete() }
        }
    }
}

internal interface ReplayDelegate {
    fun replayOutputFrame(
        image: Bitmap,
        offset: Double,
        video: ReplayBufferFile,
        completion: (suspend () -> Unit)?
    )
}

internal interface JobDelegate {
    fun jobCompleted(image: Bitmap?, video: ReplayBufferFile, offset: Double)
}

internal class FrameExtractorJob(
    private val video: ReplayBufferFile,
    private val offset: Double,
    private val pixelFormatType: Int,
    delegate: JobDelegate
) {
    var delegate: JobDelegate? = delegate
    private var reader: MediaMetadataRetriever? = null

    init {
        createReader(offset)
    }

    private fun createReader(offset: Double) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(video.url)
        } catch (error: Exception) {
            runCatching { retriever.release() }
            throw error
        }
        reader = retriever
        replayQueue.launch {
            loadVideoTrackCompletion()
        }
    }

    private fun loadVideoTrackCompletion() {
        val retriever = reader
        if (retriever == null) {
            Log.i("FrameExtractorJob", "replay: Failed to get video track with error: no reader")
            delegate?.jobCompleted(null, video, offset)
            return
        }
        val image = runCatching {
            retriever.getFrameAtTime(
                (offset * 1_000_000).toLong(),
                MediaMetadataRetriever.OPTION_CLOSEST
            )
        }.getOrElse { error ->
            Log.i("FrameExtractorJob", "replay: Failed to get video track with error: $error")
            null
        }
        reader = null
        runCatching { retriever.release() }
        if (image == null) {
            Log.i("FrameExtractorJob", "replay: No video track in file")
            delegate?.jobCompleted(null, video, offset)
            return
        }
        delegate?.jobCompleted(image, video, offset)
    }
}

internal class ReplayFrameExtractor(
    private val video: ReplayBufferFile,
    offset: Double,
    private val pixelFormatType: Int,
    delegate: ReplayDelegate,
    completion: (suspend () -> Unit)?
) : JobDelegate {
    private var delegate: ReplayDelegate? = delegate
    private var completion: (suspend () -> Unit)? = completion
    private var job: FrameExtractorJob? = null
    private var pendingOffset: Double? = null

    init {
        seek(offset)
    }

    fun seek(offset: Double) {
        replayQueue.launch {
            pendingOffset = offset
            tryNextJob()
        }
    }

    private fun tryNextJob() {
        if (job != null) {
            return
        }
        val offset = pendingOffset ?: return
        job = runCatching { FrameExtractorJob(video, offset, pixelFormatType, this) }.getOrNull()
        pendingOffset = null
    }

    override fun jobCompleted(image: Bitmap?, video: ReplayBufferFile, offset: Double) {
        if (image != null) {
            delegate?.replayOutputFrame(image, offset, video, completion)
            completion = null
        }
        job = null
        tryNextJob()
    }
}

internal class ReplayBuffer {
    private var initSegment: ByteArray? = null
    private val dataSegments: ArrayDeque<RecorderDataSegment> = ArrayDeque()

    fun setInitSegment(data: ByteArray) {
        replayQueue.launch {
            setInitSegmentInternal(data)
        }
    }

    fun appendDataSegment(segment: RecorderDataSegment) {
        replayQueue.launch {
            appendDataSegmentInternal(segment)
        }
    }

    fun createFile(completion: (ReplayBufferFile?) -> Unit) {
        replayQueue.launch {
            createFileInternal(completion)
        }
    }

    private fun setInitSegmentInternal(data: ByteArray) {
        initSegment = data
        dataSegments.clear()
    }

    private fun appendDataSegmentInternal(segment: RecorderDataSegment) {
        dataSegments.addLast(segment)
        while (stopTime() - startTime() > 30) {
            dataSegments.removeFirst()
        }
    }

    private fun createFileInternal(completion: (ReplayBufferFile?) -> Unit) {
        val initSegment = this.initSegment
        if (initSegment == null || dataSegments.isEmpty()) {
            completion(null)
            return
        }
        val url = File.createTempFile(UUID.randomUUID().toString(), ".mp4")
        var duration = 0.0
        try {
            FileOutputStream(url).use { handle ->
                handle.write(initSegment)
                for (segment in dataSegments) {
                    handle.write(segment.data)
                    duration += segment.duration
                }
            }
        } catch (error: Exception) {
            Log.i("ReplayBuffer", "replay: Error: $error")
            completion(null)
            return
        }
        completion(ReplayBufferFile(url.absolutePath, duration, true))
    }

    private fun startTime(): Double {
        val segment = dataSegments.firstOrNull() ?: return 0.0
        return segment.startTime
    }

    private fun stopTime(): Double {
        val segment = dataSegments.lastOrNull() ?: return 0.0
        return segment.startTime + segment.duration
    }
}

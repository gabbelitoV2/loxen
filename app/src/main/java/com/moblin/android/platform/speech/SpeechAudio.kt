package com.moblin.android.platform.speech

import android.media.AudioFormat
import com.moblin.android.media.MediaSample
import com.moblin.android.platform.audio.audioChannelCount
import com.moblin.android.platform.audio.audioSampleRate
import com.moblin.android.platform.audio.isRawPcmAudio
import com.moblin.android.platform.audio.pcmEncoding
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min
import kotlin.math.roundToInt

const val speechAudioSampleRate = 16000

internal class SpeechAudioConverter(private val outputSampleRate: Int = speechAudioSampleRate) {
    private var inputSampleRate = 0
    private var inputChannels = 0
    private var inputEncoding = 0
    private var step = 1.0
    private var remaining = 1.0
    private var accumulator = 0.0
    private var phase = 0.0
    private var previous = 0.0
    private var hasPrevious = false
    private var output = ShortArray(0)
    private var outputCount = 0

    fun convert(sampleBuffer: MediaSample): ByteArray? {
        val format = sampleBuffer.format ?: return null
        if (!format.isRawPcmAudio()) {
            return null
        }
        val sampleRate = format.audioSampleRate()
        val channels = format.audioChannelCount()
        val encoding = format.pcmEncoding()
        val bytesPerSample = bytesPerSample(encoding) ?: return null
        if (sampleRate <= 0 || channels <= 0) {
            return null
        }
        if (sampleRate != inputSampleRate || channels != inputChannels || encoding != inputEncoding) {
            reset(sampleRate, channels, encoding)
        }
        val data = sampleBuffer.data
        val frames = data.size / (channels * bytesPerSample)
        if (frames == 0) {
            return ByteArray(0)
        }
        ensureCapacity((frames / step).toInt() + 4)
        outputCount = 0
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val frameBytes = channels * bytesPerSample
        for (frame in 0 until frames) {
            var sum = 0.0
            var offset = frame * frameBytes
            for (channel in 0 until channels) {
                sum += readSample(buffer, offset, encoding)
                offset += bytesPerSample
            }
            push(sum / channels)
        }
        val bytes = ByteArray(outputCount * 2)
        for (index in 0 until outputCount) {
            val value = output[index].toInt()
            bytes[2 * index] = (value and 0xFF).toByte()
            bytes[2 * index + 1] = ((value shr 8) and 0xFF).toByte()
        }
        return bytes
    }

    private fun reset(sampleRate: Int, channels: Int, encoding: Int) {
        inputSampleRate = sampleRate
        inputChannels = channels
        inputEncoding = encoding
        step = sampleRate.toDouble() / outputSampleRate
        remaining = step
        accumulator = 0.0
        phase = 0.0
        previous = 0.0
        hasPrevious = false
    }

    private fun push(sample: Double) {
        if (step >= 1.0) {
            var weight = 1.0
            while (weight > 0.0) {
                val take = min(weight, remaining)
                accumulator += sample * take
                remaining -= take
                weight -= take
                if (remaining <= 1e-9) {
                    emit(accumulator / step)
                    accumulator = 0.0
                    remaining += step
                }
            }
        } else {
            if (!hasPrevious) {
                previous = sample
                hasPrevious = true
                return
            }
            while (phase < 1.0) {
                emit(previous + (sample - previous) * phase)
                phase += step
            }
            phase -= 1.0
            previous = sample
        }
    }

    private fun emit(value: Double) {
        ensureCapacity(outputCount + 1)
        output[outputCount] = (value * 32768.0).roundToInt().coerceIn(-32768, 32767).toShort()
        outputCount += 1
    }

    private fun ensureCapacity(capacity: Int) {
        if (output.size < capacity) {
            output = output.copyOf(maxOf(capacity, output.size * 2))
        }
    }

    private fun bytesPerSample(encoding: Int): Int? {
        return when (encoding) {
            AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_DEFAULT -> 2
            AudioFormat.ENCODING_PCM_8BIT -> 1
            AudioFormat.ENCODING_PCM_FLOAT -> 4
            AudioFormat.ENCODING_PCM_24BIT_PACKED -> 3
            AudioFormat.ENCODING_PCM_32BIT -> 4
            else -> null
        }
    }

    private fun readSample(buffer: ByteBuffer, offset: Int, encoding: Int): Double {
        return when (encoding) {
            AudioFormat.ENCODING_PCM_8BIT -> ((buffer.get(offset).toInt() and 0xFF) - 128) / 128.0
            AudioFormat.ENCODING_PCM_FLOAT -> buffer.getFloat(offset).toDouble().coerceIn(-1.0, 1.0)
            AudioFormat.ENCODING_PCM_24BIT_PACKED -> {
                val b0 = buffer.get(offset).toInt() and 0xFF
                val b1 = buffer.get(offset + 1).toInt() and 0xFF
                val b2 = buffer.get(offset + 2).toInt()
                ((b2 shl 16) or (b1 shl 8) or b0) / 8388608.0
            }
            AudioFormat.ENCODING_PCM_32BIT -> buffer.getInt(offset) / 2147483648.0
            else -> buffer.getShort(offset) / 32768.0
        }
    }
}

internal class SpeechAudioPipe(private val maximumBufferedBytes: Int) {
    private val lock = Object()
    private val chunks = ArrayDeque<ByteArray>()
    private var bufferedBytes = 0
    private var output: OutputStream? = null
    private var ended = false
    private var closed = false
    private var finished = false
    var droppedBytes = 0L
        private set
    var writtenBytes = 0L
        private set

    val isFinished: Boolean
        get() = synchronized(lock) { finished }

    val isEnded: Boolean
        get() = synchronized(lock) { ended || closed }

    fun write(data: ByteArray) {
        if (data.isEmpty()) {
            return
        }
        synchronized(lock) {
            if (ended || closed) {
                return
            }
            chunks.addLast(data)
            bufferedBytes += data.size
            while (bufferedBytes > maximumBufferedBytes && chunks.size > 1) {
                val dropped = chunks.removeFirst()
                bufferedBytes -= dropped.size
                droppedBytes += dropped.size
            }
            lock.notifyAll()
        }
    }

    fun attach(output: OutputStream, onFinished: () -> Unit) {
        val attached = synchronized(lock) {
            if (this.output != null) {
                false
            } else {
                this.output = output
                true
            }
        }
        if (!attached) {
            closeQuietly(output)
            return
        }
        val thread = Thread({ run(output, onFinished) }, "Moblin.SpeechAudio")
        thread.isDaemon = true
        thread.start()
    }

    fun end() {
        synchronized(lock) {
            ended = true
            lock.notifyAll()
        }
    }

    fun close() {
        val output: OutputStream?
        synchronized(lock) {
            if (closed) {
                return
            }
            closed = true
            chunks.clear()
            bufferedBytes = 0
            output = this.output
            lock.notifyAll()
        }
        closeQuietly(output)
    }

    private fun run(output: OutputStream, onFinished: () -> Unit) {
        try {
            while (true) {
                val chunk = synchronized(lock) {
                    while (!closed && !ended && chunks.isEmpty()) {
                        lock.wait()
                    }
                    if (closed || chunks.isEmpty()) {
                        null
                    } else {
                        val chunk = chunks.removeFirst()
                        bufferedBytes -= chunk.size
                        chunk
                    }
                } ?: break
                output.write(chunk)
                synchronized(lock) {
                    writtenBytes += chunk.size
                }
            }
        } catch (error: IOException) {
        } catch (error: InterruptedException) {
        } finally {
            synchronized(lock) {
                closed = true
                chunks.clear()
                bufferedBytes = 0
            }
            closeQuietly(output)
            synchronized(lock) {
                finished = true
            }
            onFinished()
        }
    }

    private fun closeQuietly(output: OutputStream?) {
        try {
            output?.close()
        } catch (error: IOException) {
        }
    }
}

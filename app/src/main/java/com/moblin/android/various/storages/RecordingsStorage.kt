package com.moblin.android.various.storages

import com.moblin.android.various.settings.SettingsStreamRecording
import com.moblin.android.various.utils.createAndGetDirectory
import com.moblin.android.various.utils.formatFilenameDateAndTime
import java.io.File
import java.time.Instant

private fun getRecordingsDirectory(): File {
    return createAndGetDirectory("Recordings")
}

private fun loadRecordingPath(settings: SettingsStreamRecording?): File? {
    if (settings?.recordingPath == null) {
        return null
    }
    return null
}

class Recording private constructor(
    private var filename: String,
    private var recording: SettingsStreamRecording?,
    private var recordingPath: File?
) {
    var startTime: Instant = Instant.now()

    private fun name(): String {
        return filename
    }

    fun url(): File? {
        return if (isDefaultRecordingPath()) {
            File(getRecordingsDirectory(), name())
        } else {
            if (recordingPath == null) {
                recordingPath = loadRecordingPath(recording)
            }
            recordingPath?.let { File(it, name()) }
        }
    }

    private fun isDefaultRecordingPath(): Boolean {
        return recording?.isDefaultRecordingPath() ?: true
    }

    companion object {
        fun create(recording: SettingsStreamRecording): Recording? {
            val result = Recording("", recording, null)
            var date = Instant.now()
            while (true) {
                result.filename = "Recording_${formatFilenameDateAndTime(date)}.mp4"
                if (result.url()?.exists() == true) {
                    date = date.plusSeconds(1)
                    continue
                }
                break
            }
            if (!result.isDefaultRecordingPath()) {
                result.recordingPath = loadRecordingPath(recording)
                if (result.recordingPath == null) {
                    return null
                }
            }
            return result
        }
    }
}

class RecordingsStorage {
    fun createRecording(recording: SettingsStreamRecording): Recording? {
        return Recording.create(recording)
    }

    fun defaultStorageDirectory(): File {
        return getRecordingsDirectory()
    }
}

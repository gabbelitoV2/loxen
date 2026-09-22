package com.moblin.android.common.various

import android.media.MediaFormat
import android.os.Build

fun MediaFormat.atoms(): Map<String, ByteArray>? {
    val buffer = getByteBuffer("csd-0") ?: return null
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val mime = runCatching { getString(MediaFormat.KEY_MIME) }.getOrNull()
    val key = if (mime == MediaFormat.MIMETYPE_VIDEO_HEVC) "hvcC" else "avcC"
    return mapOf(key to bytes)
}

fun MediaFormat.extensions(): Map<String, Any>? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        return TODO("MediaFormat.getKeys() and getValue() require API 29; key enumeration is unavailable on API 26-28")
    }
    return TODO("MediaFormat.getValue(String) is unresolved in this compile SDK; extension values cannot be enumerated")
}

fun MediaFormat.numberOfAudioChannels(): Int? {
    if (!containsKey(MediaFormat.KEY_CHANNEL_COUNT)) return null
    return runCatching { getInteger(MediaFormat.KEY_CHANNEL_COUNT) }.getOrNull()
}

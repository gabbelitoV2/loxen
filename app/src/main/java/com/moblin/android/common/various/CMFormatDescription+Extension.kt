package com.moblin.android.common.various

import android.media.MediaFormat
import com.moblin.android.platform.videotoolbox.CMFormatDescriptionGetExtension
import com.moblin.android.platform.videotoolbox.CMFormatDescriptionGetExtensions
import com.moblin.android.platform.videotoolbox.kCMFormatDescriptionExtension_SampleDescriptionExtensionAtoms

@Suppress("UNCHECKED_CAST")
fun MediaFormat.atoms(): Map<String, ByteArray>? {
    return CMFormatDescriptionGetExtension(
        this,
        extensionKey = kCMFormatDescriptionExtension_SampleDescriptionExtensionAtoms,
    ) as? Map<String, ByteArray>
}

fun MediaFormat.extensions(): Map<String, Any>? {
    return CMFormatDescriptionGetExtensions(this)
}

fun MediaFormat.numberOfAudioChannels(): Int? {
    if (!containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
        return null
    }
    return runCatching { getInteger(MediaFormat.KEY_CHANNEL_COUNT) }.getOrNull()
}

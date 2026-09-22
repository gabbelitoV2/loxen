package com.moblin.android.media

import android.media.MediaFormat

class MediaSample(
    val data: ByteArray,
    val presentationTimeUs: Long,
    val isKeyFrame: Boolean,
    val format: MediaFormat?,
)

package com.moblin.android.platform.coreimage

import com.moblin.android.platform.coreimage.internal.pixelBufferSources
import com.moblin.android.platform.video.PixelBufferLeases

fun retainImageLeases(image: CIImage?): CIImage? {
    if (image != null) {
        for (buffer in pixelBufferSources(image.node)) {
            PixelBufferLeases.retain(buffer, "retainImageLeases")
        }
    }
    return image
}

fun releaseImageLeases(image: CIImage?) {
    if (image == null) {
        return
    }
    for (buffer in pixelBufferSources(image.node)) {
        PixelBufferLeases.release(buffer, "releaseImageLeases")
    }
}

fun <T : Any> swapImageLease(old: T?, new: T?, image: (T) -> CIImage?): T? {
    if (old === new) {
        return new
    }
    if (new != null) {
        retainImageLeases(image(new))
    }
    if (old != null) {
        releaseImageLeases(image(old))
    }
    return new
}

fun <T> drainImageLeases(items: MutableCollection<T>, image: (T) -> CIImage?) {
    val drained = items.toList()
    items.clear()
    for (item in drained) {
        releaseImageLeases(image(item))
    }
}

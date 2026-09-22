package com.moblin.android.view.stream

import androidx.compose.ui.geometry.Size
import com.moblin.android.various.model.Model

data class StreamViewInsets(
    val leading: Float,
    val top: Float,
    val trailing: Float,
    val bottom: Float,
)

data class StreamViewMetrics(
    val size: Size,
    val safeAreaInsets: StreamViewInsets,
)

data class StreamViewLayout(val size: Size, val offset: Size) {
    companion object {
        operator fun invoke(
            metrics: StreamViewMetrics,
            aspectRatio: Float,
            portraitOrientation: Boolean,
            portraitStream: Boolean,
            portraitVideoOffset: Double,
        ): StreamViewLayout {
            val insets = metrics.safeAreaInsets
            val fullSize = Size(
                width = metrics.size.width + insets.leading + insets.trailing,
                height = metrics.size.height + insets.top + insets.bottom,
            )
            var size = Size(width = fullSize.width, height = fullSize.width / aspectRatio)
            if (size.height > fullSize.height) {
                size = Size(width = fullSize.height * aspectRatio, height = fullSize.height)
            }
            val x: Float = if (size.width <= metrics.size.width) {
                (metrics.size.width - size.width) / 2
            } else if (portraitOrientation) {
                (fullSize.width - size.width) / 2 - insets.leading
            } else {
                fullSize.width - size.width - insets.leading
            }
            val y: Float = if (portraitOrientation && !portraitStream) {
                (portraitVideoOffset * size.height * 2).toFloat() - insets.top
            } else if (size.height <= metrics.size.height) {
                (metrics.size.height - size.height) / 2
            } else if (portraitOrientation) {
                fullSize.height - size.height - insets.top
            } else {
                (fullSize.height - size.height) / 2 - insets.top
            }
            return StreamViewLayout(size = size, offset = Size(width = x, height = y))
        }
    }
}

fun Model.streamViewLayout(metrics: StreamViewMetrics): StreamViewLayout {
    val dimensions = stream.value.dimensions()
    return StreamViewLayout(
        metrics = metrics,
        aspectRatio = dimensions.width.toFloat() / dimensions.height.toFloat(),
        portraitOrientation = orientation.isPortrait.value,
        portraitStream = stream.value.portrait,
        portraitVideoOffset = if (stream.value.portrait) 0.0 else portraitVideoOffsetFromTop.value,
    )
}

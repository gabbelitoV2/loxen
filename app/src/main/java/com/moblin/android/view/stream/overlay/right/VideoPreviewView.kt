package com.moblin.android.view.stream.overlay.right

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.moblin.android.LocalModel
import com.moblin.android.common.various.backgroundColor
import com.moblin.android.localized
import com.moblin.android.media.haishinkit.media.video.PreviewView
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.Orientation
import com.moblin.android.various.model.VideoPreviewProvider
import com.moblin.android.various.model.setCurrentSceneVideoSource
import com.moblin.android.view.stream.SharedUiViewContainerView

@Composable
fun VideoPreviewItemView(previewView: PreviewView, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context -> SharedUiViewContainerView(context, previewView) },
        modifier = modifier,
        update = { uiView -> uiView.attachSharedView() },
    )
}

@Composable
private fun VideoPreviewItem(
    orientation: Orientation,
    name: String,
    previewView: PreviewView,
    onTap: () -> Unit,
) {
    val isPortrait by orientation.isPortrait.collectAsState()
    val currentOnTap by rememberUpdatedState(onTap)

    fun height(): Dp = if (isPortrait) 118.dp else 68.dp

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { currentOnTap() })
        },
    ) {
        VideoPreviewItemView(
            previewView = previewView,
            modifier = Modifier
                .height(height())
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(5.dp))
                .border(
                    width = 1.dp,
                    color = Color.Gray.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(5.dp),
                ),
        )
        Text(
            text = name,
            fontSize = 11.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun StreamOverlayRightVideoPreviewView(
    model: Model = LocalModel.current,
    orientation: Orientation,
    videoPreview: VideoPreviewProvider,
) {
    val isPortrait by orientation.isPortrait.collectAsState()
    val feeds by videoPreview.feeds.collectAsState()

    fun height(): Dp = if (isPortrait) 140.dp else 90.dp

    LazyRow(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(backgroundColor)
            .padding(4.dp)
            .height(height()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (feeds.isEmpty()) {
            item {
                Text(
                    text = localized("No built-in cameras or ingests connected"),
                    fontSize = 17.sp,
                    color = Color.White,
                    modifier = Modifier.padding(start = 30.dp),
                )
            }
        }
        items(feeds) { feed ->
            VideoPreviewItem(
                orientation = orientation,
                name = feed.name,
                previewView = feed.previewView,
                onTap = { model.setCurrentSceneVideoSource(cameraId = feed.cameraId) },
            )
        }
    }
}

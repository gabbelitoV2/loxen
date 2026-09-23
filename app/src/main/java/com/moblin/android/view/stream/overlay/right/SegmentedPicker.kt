package com.moblin.android.view.stream.overlay.right

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.moblin.android.common.various.color
import com.moblin.android.various.settings.defaultSegmentedPickerSelectedColor

val zoomSegmentWidth = 50.0
val zoomSegmentWidthBig = 60.0
val segmentHeight = 40.0
val segmentHeightBig = 60.0
val sceneSegmentWidth = 70.0
val sliderWidth = 250.0
val sliderHeight = 40.0
val cameraButtonWidth = 70.0
val pickerBorderColor = Color.Gray
val pickerBackgroundColor = Color.Black.copy(alpha = 0.4f)
val pickerLabelMinimumScaleFactor = 0.7

@Composable
fun PickerLabelText(text: String, width: Dp, height: Dp) {
    val minimumScale = pickerLabelMinimumScaleFactor.toFloat()
    var scale by remember(text, width, height) { mutableFloatStateOf(1f) }
    var ready by remember(text, width, height) { mutableStateOf(false) }
    Box(
        modifier = Modifier.size(width = width, height = height),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = (17 * scale).sp,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.drawWithContent {
                if (ready) {
                    drawContent()
                }
            },
            onTextLayout = { result ->
                if (result.hasVisualOverflow && scale > minimumScale) {
                    scale = maxOf(scale - 0.05f, minimumScale)
                } else {
                    ready = true
                }
            },
        )
    }
}

@Composable
private fun <T : Any, Content> Segment(
    item: T,
    index: Int,
    selectedItem: T?,
    selectedColor: Color,
    onSelectedItemChange: (T?) -> Unit,
    onLongPress: ((Int) -> Unit)?,
    content: @Composable (T) -> Content,
    modifier: Modifier,
) {
    val currentOnSelectedItemChange by rememberUpdatedState(onSelectedItemChange)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.1f))
            .then(if (item == selectedItem) Modifier.background(selectedColor) else Modifier)
            .pointerInput(item, index) {
                detectTapGestures(
                    onTap = { currentOnSelectedItemChange(item) },
                    onLongPress = { currentOnLongPress?.invoke(index) },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        content(item)
    }
}

@Composable
fun <T : Any, Content> SegmentedPicker(
    items: List<T>,
    selectedItem: T?,
    onSelectedItemChange: (T?) -> Unit,
    selectedColor: Color,
    content: @Composable (T) -> Content,
    onLongPress: ((Int) -> Unit)? = null,
) {
    SegmentedVPicker(
        items = items,
        selectedItem = selectedItem,
        onSelectedItemChange = onSelectedItemChange,
        selectedColor = selectedColor,
        onLongPress = onLongPress,
        content = content,
    )
}

@Composable
fun <T : Any, Content> SegmentedHPicker(
    items: List<T>,
    selectedItem: T?,
    onSelectedItemChange: (T?) -> Unit,
    selectedColor: Color = defaultSegmentedPickerSelectedColor.color(),
    onLongPress: ((Int) -> Unit)? = null,
    content: @Composable (T) -> Content,
) {
    Row(
        modifier = Modifier
            .wrapContentWidth(unbounded = true)
            .height(IntrinsicSize.Min),
    ) {
        items.forEachIndexed { index, item ->
            Segment(
                item = item,
                index = index,
                selectedItem = selectedItem,
                selectedColor = selectedColor,
                onSelectedItemChange = onSelectedItemChange,
                onLongPress = onLongPress,
                content = content,
                modifier = Modifier.fillMaxHeight(),
            )
            VerticalDivider(color = pickerBorderColor)
        }
    }
}

@Composable
fun <T : Any, Content> SegmentedVPicker(
    items: List<T>,
    selectedItem: T?,
    onSelectedItemChange: (T?) -> Unit,
    selectedColor: Color = defaultSegmentedPickerSelectedColor.color(),
    onLongPress: ((Int) -> Unit)? = null,
    content: @Composable (T) -> Content,
) {
    Column {
        items.forEachIndexed { index, item ->
            Segment(
                item = item,
                index = index,
                selectedItem = selectedItem,
                selectedColor = selectedColor,
                onSelectedItemChange = onSelectedItemChange,
                onLongPress = onLongPress,
                content = content,
                modifier = Modifier.fillMaxWidth(),
            )
            HorizontalDivider(color = pickerBorderColor)
        }
    }
}

package com.moblin.android.view.stream.overlay.right

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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

private fun defaultSegmentedPickerSelectedColorColor(): Color {
    val color = defaultSegmentedPickerSelectedColor
    return Color(red = color.red, green = color.green, blue = color.blue)
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
) {
    Box(
        modifier = Modifier
            .background(if (item == selectedItem) selectedColor else Color.Black.copy(alpha = 0.1f))
            .pointerInput(item) {
                detectTapGestures(
                    onTap = { onSelectedItemChange(item) },
                    onLongPress = { onLongPress?.invoke(index) },
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
    selectedColor: Color = defaultSegmentedPickerSelectedColorColor(),
    onLongPress: ((Int) -> Unit)? = null,
    content: @Composable (T) -> Content,
) {
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        items.forEachIndexed { index, item ->
            Segment(item, index, selectedItem, selectedColor, onSelectedItemChange, onLongPress, content)
            VerticalDivider(color = pickerBorderColor)
        }
    }
}

@Composable
fun <T : Any, Content> SegmentedVPicker(
    items: List<T>,
    selectedItem: T?,
    onSelectedItemChange: (T?) -> Unit,
    selectedColor: Color = defaultSegmentedPickerSelectedColorColor(),
    onLongPress: ((Int) -> Unit)? = null,
    content: @Composable (T) -> Content,
) {
    Column(modifier = Modifier.width(IntrinsicSize.Min)) {
        items.forEachIndexed { index, item ->
            Segment(item, index, selectedItem, selectedColor, onSelectedItemChange, onLongPress, content)
            HorizontalDivider(color = pickerBorderColor)
        }
    }
}

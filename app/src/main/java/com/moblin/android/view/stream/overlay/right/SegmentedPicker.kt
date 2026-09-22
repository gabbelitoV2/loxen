package com.moblin.android.view.stream.overlay.right

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

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
fun <T : Any, Content> SegmentedPicker(
    items: List<T>,
    selectedItem: T?,
    onSelectedItemChange: (T?) -> Unit,
    selectedColor: Color,
    content: @Composable (T) -> Content,
    onLongPress: ((Int) -> Void)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        for (index in items.indices) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(segmentHeight.dp)
                    .background(Color.Black.copy(alpha = 0.1f))
                    .pointerInput(items[index]) {
                        detectTapGestures(
                            onTap = { onSelectedItemChange(items[index]) },
                            onLongPress = { onLongPress?.invoke(index) },
                        )
                    },
            ) {
                if (items[index] == selectedItem) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(segmentHeight.dp)
                            .background(selectedColor),
                    )
                }
                content(items[index])
            }
            HorizontalDivider(color = pickerBorderColor)
        }
    }
}

@Composable
fun <T : Any, Content> SegmentedHPicker(
    items: List<T>,
    selectedItem: T?,
    onSelectedItemChange: (T?) -> Unit,
    selectedColor: Color = TODO("defaultSegmentedPickerSelectedColor.color()"),
    onLongPress: ((Int) -> Void)? = null,
    content: @Composable (T) -> Content,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        SegmentedPicker(
            items = items,
            selectedItem = selectedItem,
            onSelectedItemChange = onSelectedItemChange,
            selectedColor = selectedColor,
            content = content,
            onLongPress = onLongPress,
        )
    }
}

@Composable
fun <T : Any, Content> SegmentedVPicker(
    items: List<T>,
    selectedItem: T?,
    onSelectedItemChange: (T?) -> Unit,
    selectedColor: Color = TODO("defaultSegmentedPickerSelectedColor.color()"),
    onLongPress: ((Int) -> Void)? = null,
    content: @Composable (T) -> Content,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SegmentedPicker(
            items = items,
            selectedItem = selectedItem,
            onSelectedItemChange = onSelectedItemChange,
            selectedColor = selectedColor,
            content = content,
            onLongPress = onLongPress,
        )
    }
}

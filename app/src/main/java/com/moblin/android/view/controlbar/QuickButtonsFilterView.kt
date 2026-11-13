package com.moblin.android.view.controlbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation as AxisOrientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.common.various.controlBarWidthDefault
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.Orientation as DeviceOrientation
import com.moblin.android.various.model.QuickButtonsFilter
import com.moblin.android.various.settings.SettingsQuickButtons
import com.moblin.android.various.model.quickButtonsFilterFocusLost
import com.moblin.android.various.model.startFilteringQuickButtons

@Composable
private fun FilterTextFieldView(model: Model = LocalModel.current, quickButtonsFilter: QuickButtonsFilter) {
    val filter by quickButtonsFilter.filter.collectAsState()
    val focusRequester = remember { FocusRequester() }
    var hadFocus by remember { mutableStateOf(false) }
    val palette = formPalette()
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    BasicTextField(
        value = filter,
        onValueChange = { value ->
            quickButtonsFilter.filter.value = value
            model.updateFilteredQuickButtonPairs()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(palette.cell)
            .border(0.5.dp, palette.separator, RoundedCornerShape(7.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                if (state.isFocused) {
                    hadFocus = true
                } else if (hadFocus) {
                    hadFocus = false
                    model.quickButtonsFilterFocusLost()
                }
            },
        singleLine = true,
        textStyle = TextStyle(fontSize = 17.sp, color = palette.label),
        cursorBrush = SolidColor(palette.accent),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (filter.isEmpty()) {
                    Text(
                        text = localized("Filter"),
                        style = TextStyle(fontSize = 17.sp, color = palette.secondaryLabel),
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun QuickButtonsFilterView(
    model: Model = LocalModel.current,
    quickButtonsFilter: QuickButtonsFilter,
    quickButtonsSettings: SettingsQuickButtons,
    orientation: DeviceOrientation,
) {
    val pairs by quickButtonsFilter.pairs.collectAsState()
    val bigButtons by quickButtonsSettings.bigButtons.collectAsState()
    val twoColumns by quickButtonsSettings.twoColumns.collectAsState()
    val isPortrait by orientation.isPortrait.collectAsState()
    val width = remember(bigButtons, twoColumns) {
        controlBarWidth(quickButtons = quickButtonsSettings)
    }
    if (isPortrait) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier.padding(start = 5.dp, top = 5.dp),
            ) {
                ControlBarPortraitPageView(
                    model = model,
                    quickButtons = model.quickButtons,
                    quickButtonsSettings = quickButtonsSettings,
                    pairs = pairs,
                    height = width,
                )
            }
            Box(
                modifier = Modifier
                    .width(controlBarWidthDefault.dp)
                    .padding(horizontal = 5.dp)
                    .padding(top = 3.dp),
            ) {
                FilterTextFieldView(model = model, quickButtonsFilter = quickButtonsFilter)
            }
        }
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Box(
                modifier = Modifier.padding(start = 5.dp),
            ) {
                ControlBarLandscapePageView(
                    model = model,
                    quickButtons = model.quickButtons,
                    quickButtonsSettings = quickButtonsSettings,
                    pairs = pairs,
                    width = width,
                )
            }
            Box(
                modifier = Modifier.padding(5.dp),
            ) {
                FilterTextFieldView(model = model, quickButtonsFilter = quickButtonsFilter)
            }
        }
    }
}

@Composable
private fun QuickButtonsFilterOverlayView(model: Model = LocalModel.current, quickButtonsFilter: QuickButtonsFilter) {
    val filtering by quickButtonsFilter.filtering.collectAsState()
    if (filtering) {
        Box {
            Box(modifier = Modifier.matchParentSize()) {
                ControlBarBackgroundView(controlBar = model.controlBar)
            }
            QuickButtonsFilterView(
                model = model,
                quickButtonsFilter = quickButtonsFilter,
                quickButtonsSettings = model.database.quickButtonsGeneral,
                orientation = model.orientation,
            )
        }
    }
}

@Composable
fun controlBarDoubleTapArea(model: Model = LocalModel.current, content: @Composable () -> Unit) {
    Box {
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(model) {
                    detectTapGestures(onDoubleTap = {
                        model.startFilteringQuickButtons()
                    })
                },
        )
        content()
    }
}

@Composable
fun controlBarFilter(model: Model = LocalModel.current, content: @Composable () -> Unit) {
    Box {
        Box(modifier = Modifier.matchParentSize()) {
            ControlBarBackgroundView(controlBar = model.controlBar)
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(model) {
                    detectTapGestures(onDoubleTap = {
                        model.startFilteringQuickButtons()
                    })
                },
        )
        content()
        QuickButtonsFilterOverlayView(model = model, quickButtonsFilter = model.quickButtonsFilter)
    }
}

@Composable
private fun FilterOnOverscrollModifier(
    model: Model = LocalModel.current,
    axis: AxisOrientation,
    content: @Composable () -> Unit,
) {
    Unit
}

@Composable
fun filterQuickButtonsOnOverscroll(
    model: Model = LocalModel.current,
    axis: AxisOrientation,
    content: @Composable () -> Unit,
) {
    content()
}

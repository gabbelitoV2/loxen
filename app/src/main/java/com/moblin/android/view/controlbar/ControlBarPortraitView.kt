package com.moblin.android.view.controlbar

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.controlBarButtonSize
import com.moblin.android.common.various.controlBarQuickButtonSingleQuickButtonSize
import com.moblin.android.common.various.controlBarWidthDefault
import com.moblin.android.common.view.ThermalStateView
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.QuickButtons
import com.moblin.android.various.model.ShowingPanel
import com.moblin.android.various.model.StatusOther
import com.moblin.android.various.model.Store
import com.moblin.android.various.settings.SettingsQuickButtons
import com.moblin.android.LocalModel

private fun buttonSize(bigButtons: Boolean) =
    if (bigButtons) controlBarQuickButtonSingleQuickButtonSize else controlBarButtonSize

@Composable
private fun QuickButtonsView(
    model: Model = LocalModel.current,
    quickButtons: QuickButtons,
    quickButtonsSettings: SettingsQuickButtons,
    page: Int,
    height: Double,
) {
    val bigButtons by quickButtonsSettings.bigButtons.collectAsState()
    val twoColumns by quickButtonsSettings.twoColumns.collectAsState()
    val size = buttonSize(bigButtons)
    Row {
        model.getQuickButtonPairs(page + 1).forEach { pair ->
            if (twoColumns) {
                Column(horizontalAlignment = Alignment.Start) {
                    val second = pair.second
                    if (second != null) {
                        QuickButtonsInnerView(
                            quickButtons = quickButtons,
                            quickButtonsSettings = quickButtonsSettings,
                            orientation = model.orientation,
                            button = second,
                            size = size,
                            nameSize = size,
                            nameWidth = size,
                        )
                    } else {
                        QuickButtonPlaceholderImage(size = size)
                    }
                    QuickButtonsInnerView(
                        quickButtons = quickButtons,
                        quickButtonsSettings = quickButtonsSettings,
                        orientation = model.orientation,
                        button = pair.first,
                        size = size,
                        nameSize = size,
                        nameWidth = size,
                    )
                }
            } else {
                val second = pair.second
                if (second != null) {
                    Box(modifier = Modifier.height((height - 10).dp)) {
                        QuickButtonsInnerView(
                            quickButtons = quickButtons,
                            quickButtonsSettings = quickButtonsSettings,
                            orientation = model.orientation,
                            button = second,
                            size = size,
                            nameSize = size,
                            nameWidth = size,
                        )
                    }
                }
                Box(modifier = Modifier.height((height - 10).dp)) {
                    QuickButtonsInnerView(
                        quickButtons = quickButtons,
                        quickButtonsSettings = quickButtonsSettings,
                        orientation = model.orientation,
                        button = pair.first,
                        size = size,
                        nameSize = size,
                        nameWidth = size,
                    )
                }
            }
        }
    }
}

@Composable
private fun PageView(
    model: Model = LocalModel.current,
    quickButtons: QuickButtons,
    quickButtonsSettings: SettingsQuickButtons,
    page: Int,
    height: Double,
) {
    val enableScroll by quickButtonsSettings.enableScroll.collectAsState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(state = rememberScrollState(), enabled = enableScroll)
            .rotate(180f),
        contentAlignment = Alignment.BottomStart,
    ) {
        QuickButtonsView(
            model = model,
            quickButtons = quickButtons,
            quickButtonsSettings = quickButtonsSettings,
            page = page,
            height = height,
        )
    }
}

@Composable
private fun IconAndSettingsView(model: Model = LocalModel.current, store: Store) {
    val iconImage by store.iconImage.collectAsState()
    val context = LocalContext.current
    val storeIconResId = remember(iconImage) {
        context.resources.getIdentifier("${iconImage}NoBackground", "drawable", context.packageName)
    }
    Row(
        modifier = Modifier.padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        TextButton(
            onClick = { model.toggleShowingPanel(null, ShowingPanel.store) },
            contentPadding = PaddingValues(0.dp),
        ) {
            Image(
                painter = painterResource(id = storeIconResId),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(controlBarButtonSize.dp)
                    .padding(bottom = 4.dp)
                    .offset(x = 2.dp),
            )
        }
        TextButton(
            onClick = { model.toggleShowingPanel(null, ShowingPanel.settings) },
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.padding(start = 10.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(controlBarButtonSize.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = CircleShape,
                    ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainPageView(
    model: Model = LocalModel.current,
    quickButtons: QuickButtons,
    quickButtonsSettings: SettingsQuickButtons,
    status: StatusOther,
    height: Double,
) {
    val thermalState by status.thermalState.collectAsState()
    val presentingThermalState = remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 5.dp, top = 5.dp),
            contentAlignment = Alignment.BottomStart,
        ) {
            PageView(
                model = model,
                quickButtons = quickButtons,
                quickButtonsSettings = quickButtonsSettings,
                page = 0,
                height = height,
            )
        }
        Column(modifier = Modifier.width(controlBarWidthDefault.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 3.dp, end = 5.dp),
            ) {
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { presentingThermalState.value = !presentingThermalState.value },
                    contentPadding = PaddingValues(0.dp),
                ) {
                    ThermalStateView(thermalState = TODO("MoblinkThermalState to ThermalState conversion"))
                }
                Spacer(modifier = Modifier.weight(1f))
            }
            IconAndSettingsView(model = model, store = model.store)
            Box(modifier = Modifier.padding(top = 10.dp, start = 5.dp, end = 5.dp)) {
                StreamButton(show = model.show)
            }
        }
    }
    if (presentingThermalState.value) {
        ModalBottomSheet(onDismissRequest = { presentingThermalState.value = false }) {
            ThermalStateSheetView(
                presenting = presentingThermalState.value,
                onPresentingChange = { presentingThermalState.value = it },
            )
        }
    }
}

@Composable
private fun PagesView(
    model: Model = LocalModel.current,
    quickButtons: QuickButtons,
    quickButtonsSettings: SettingsQuickButtons,
    height: Double,
) {
    val pairs by quickButtons.pairs.collectAsState()
    val listState = rememberLazyListState()
    val pages = (1 until controlBarPages).filter { pairs.getOrNull(it)?.isNotEmpty() == true }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = 1) {
            Box(modifier = Modifier.fillParentMaxHeight()) {
                MainPageView(
                    model = model,
                    quickButtons = quickButtons,
                    quickButtonsSettings = quickButtonsSettings,
                    status = model.statusOther,
                    height = height,
                )
            }
        }
        items(items = pages, key = { it }) { page ->
            Box(
                modifier = Modifier
                    .fillParentMaxHeight()
                    .padding(top = 5.dp, start = 5.dp, end = 5.dp),
            ) {
                PageView(
                    model = model,
                    quickButtons = quickButtons,
                    quickButtonsSettings = quickButtonsSettings,
                    page = page,
                    height = height,
                )
            }
        }
    }
}

@Composable
fun ControlBarPortraitView(model: Model = LocalModel.current, quickButtons: SettingsQuickButtons) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(controlBarWidth(quickButtons = quickButtons).dp),
    ) {
        ControlBarBackgroundView(controlBar = model.controlBar)
        PagesView(
            model = model,
            quickButtons = model.quickButtons,
            quickButtonsSettings = model.database.quickButtonsGeneral,
            height = controlBarWidth(quickButtons = quickButtons),
        )
    }
}

package com.moblin.android.view.controlbar

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.controlBarButtonSize
import com.moblin.android.common.various.controlBarQuickButtonNameSingleColumnSize
import com.moblin.android.common.various.controlBarQuickButtonNameSize
import com.moblin.android.common.various.controlBarQuickButtonSingleQuickButtonSize
import com.moblin.android.common.various.controlBarWidthBigQuickButtons
import com.moblin.android.common.various.controlBarWidthDefault
import com.moblin.android.common.various.smallFont
import com.moblin.android.common.view.ThermalStateView
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.QuickButtons
import com.moblin.android.various.model.ShowingPanel
import com.moblin.android.various.model.StatusOther
import com.moblin.android.various.model.Store
import com.moblin.android.various.settings.SettingsQuickButtons
import com.moblin.android.various.utils.isMac
import com.moblin.android.various.utils.isPhone
import com.moblin.android.view.utils.HCenter
import com.moblin.android.LocalModel

private fun edgesToIgnore(): List<String> {
    return if (isPhone()) {
        listOf("trailing")
    } else {
        emptyList()
    }
}

fun controlBarWidth(quickButtons: SettingsQuickButtons): Double {
    return if (quickButtons.bigButtons.value && quickButtons.twoColumns.value) {
        controlBarWidthBigQuickButtons
    } else {
        controlBarWidthDefault
    }
}

@Composable
private fun QuickButtonsView(
    model: Model = LocalModel.current,
    quickButtons: QuickButtons,
    quickButtonsSettings: SettingsQuickButtons,
    page: Int,
    width: Double,
) {
    val bigButtons = quickButtonsSettings.bigButtons.collectAsState().value
    val twoColumns = quickButtonsSettings.twoColumns.collectAsState().value
    val orientation = model.orientation.collectAsState().value

    fun buttonSize(): Double {
        return if (bigButtons) {
            controlBarQuickButtonSingleQuickButtonSize
        } else {
            controlBarButtonSize
        }
    }

    fun nameSize(): Double {
        return if (bigButtons) {
            controlBarQuickButtonNameSingleColumnSize
        } else {
            controlBarQuickButtonNameSize
        }
    }

    Column {
        model.getQuickButtonPairs(page = page + 1).forEach { pair ->
            if (twoColumns) {
                Row(verticalAlignment = Alignment.Bottom) {
                    val second = pair.second
                    if (second != null) {
                        QuickButtonsInnerView(
                            quickButtons = quickButtons,
                            quickButtonsSettings = quickButtonsSettings,
                            orientation = orientation,
                            button = second,
                            size = buttonSize(),
                            nameSize = nameSize(),
                            nameWidth = buttonSize(),
                        )
                    } else {
                        QuickButtonPlaceholderImage(size = buttonSize())
                    }
                    QuickButtonsInnerView(
                        quickButtons = quickButtons,
                        quickButtonsSettings = quickButtonsSettings,
                        orientation = orientation,
                        button = pair.first,
                        size = buttonSize(),
                        nameSize = nameSize(),
                        nameWidth = buttonSize(),
                    )
                }
            } else {
                val second = pair.second
                if (second != null) {
                    Box(modifier = Modifier.width((width - 10).dp)) {
                        QuickButtonsInnerView(
                            quickButtons = quickButtons,
                            quickButtonsSettings = quickButtonsSettings,
                            orientation = orientation,
                            button = second,
                            size = buttonSize(),
                            nameSize = nameSize(),
                            nameWidth = width - 10,
                        )
                    }
                }
                Box(modifier = Modifier.width((width - 10).dp)) {
                    QuickButtonsInnerView(
                        quickButtons = quickButtons,
                        quickButtonsSettings = quickButtonsSettings,
                        orientation = orientation,
                        button = pair.first,
                        size = buttonSize(),
                        nameSize = nameSize(),
                        nameWidth = width - 10,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusView(model: Model = LocalModel.current, status: StatusOther) {
    var presentingThermalState by remember { mutableStateOf(false) }
    val battery = model.battery.collectAsState().value
    val thermalState = status.thermalState.collectAsState().value
    val digitalClock = status.digitalClock.collectAsState().value

    Row(
        modifier = Modifier
            .padding(start = 0.dp, bottom = 0.dp)
            .padding(end = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isPhone()) {
            BatteryView(model = model, battery = battery)
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                presentingThermalState = !presentingThermalState
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        ) {
            ThermalStateView(thermalState = thermalState)
        }
        Spacer(Modifier.weight(1f))
        if (isPhone()) {
            Text(
                text = digitalClock,
                color = Color.White,
                style = smallFont,
            )
        }
    }
    if (presentingThermalState) {
        ModalBottomSheet(onDismissRequest = { presentingThermalState = false }) {
            ThermalStateSheetView(
                presenting = presentingThermalState,
                onPresentingChange = { presentingThermalState = it },
            )
        }
    }
}

@Composable
private fun IconAndSettingsView(model: Model = LocalModel.current, store: Store) {
    val iconImage = store.iconImage.collectAsState().value

    HCenter {
        val context = LocalContext.current
        val iconResId = remember(iconImage) {
            context.resources.getIdentifier("${iconImage}NoBackground", "drawable", context.packageName)
        }
        Button(
            onClick = {
                model.toggleShowingPanel(type = null, panel = ShowingPanel.store)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp),
        ) {
            if (iconResId != 0) {
                Image(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    modifier = Modifier.size(controlBarButtonSize.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        }
        Button(
            onClick = {
                model.toggleShowingPanel(type = null, panel = ShowingPanel.settings)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(controlBarButtonSize.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            )
        }
    }
}

@Composable
private fun PageView(
    model: Model = LocalModel.current,
    quickButtons: QuickButtons,
    quickButtonsSettings: SettingsQuickButtons,
    page: Int,
    width: Double,
) {
    val enableScroll = quickButtonsSettings.enableScroll.collectAsState().value

    Column(
        modifier = Modifier
            .verticalScroll(state = rememberScrollState(), enabled = enableScroll)
            .rotate(180f),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            QuickButtonsView(
                model = model,
                quickButtons = quickButtons,
                quickButtonsSettings = quickButtonsSettings,
                page = page,
                width = width,
            )
        }
    }
}

@Composable
private fun MainPageView(
    model: Model = LocalModel.current,
    quickButtons: QuickButtons,
    quickButtonsSettings: SettingsQuickButtons,
    store: Store,
    width: Double,
) {
    val bigButtons = quickButtonsSettings.bigButtons.collectAsState().value
    val twoColumns = quickButtonsSettings.twoColumns.collectAsState().value
    val show = model.show.collectAsState().value

    fun buttonsWidth(): Double {
        return if (bigButtons && twoColumns) {
            width - 20
        } else {
            width - 10
        }
    }

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Box(modifier = Modifier.padding(vertical = 2.dp).width(buttonsWidth().dp)) {
            IconAndSettingsView(model = model, store = store)
        }
        PageView(
            model = model,
            quickButtons = quickButtons,
            quickButtonsSettings = quickButtonsSettings,
            page = 0,
            width = width,
        )
        Row(
            modifier = Modifier.width(buttonsWidth().dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            Box(modifier = Modifier.padding(top = 5.dp)) {
                StreamButton(show = show)
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

private class ControlBarPageScrollTargetBehavior(private val model: Model) {
    fun updateTarget(containerWidth: Double, targetPosition: Double): Double {
        return controlBarScrollTargetBehavior(
            model = model,
            containerWidth = containerWidth,
            targetPosition = targetPosition,
        )
    }
}

@Composable
private fun PageIndicatorView(model: Model = LocalModel.current, size: Float, quickButtons: QuickButtons) {
    val pairs = quickButtons.pairs.collectAsState().value
    val activePage = quickButtons.activePage.collectAsState().value

    fun visiblePages(): List<Int> {
        val pages = mutableListOf(1)
        for (page in 1 until controlBarPages) {
            if (pairs[page].isNotEmpty()) {
                pages.add(page + 1)
            }
        }
        return pages
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        visiblePages().forEach { page ->
            Icon(
                imageVector = if (activePage == page) Icons.Filled.Circle else Icons.Outlined.Circle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(size.dp)
                    .clickable {
                        quickButtons.page.value = page
                        quickButtons.activePage.value = page
                        model.updateQuickButtonPairs()
                    },
            )
        }
    }
}

@Composable
private fun PagesView(
    model: Model = LocalModel.current,
    quickButtons: QuickButtons,
    quickButtonsSettings: SettingsQuickButtons,
    width: Double,
) {
    val bigButtons = quickButtonsSettings.bigButtons.collectAsState().value
    val twoColumns = quickButtonsSettings.twoColumns.collectAsState().value
    val pairs = quickButtons.pairs.collectAsState().value
    val activePage = quickButtons.activePage.collectAsState().value
    val store = model.store.collectAsState().value

    fun offsetX(): Double {
        return if (bigButtons && twoColumns) {
            -6.0
        } else {
            -1.0
        }
    }

    val pages = remember(pairs) {
        buildList {
            add(1)
            for (page in 1 until controlBarPages) {
                if (pairs[page].isNotEmpty()) {
                    add(page + 1)
                }
            }
        }
    }
    val pagerState = rememberPagerState(
        initialPage = (activePage - 1).coerceIn(0, pages.size - 1),
        pageCount = { pages.size },
    )
    LaunchedEffect(pagerState.currentPage) {
        val page = pages.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        quickButtons.page.value = page
        quickButtons.activePage.value = page
        model.updateQuickButtonPairs()
    }
    Column {
        Box(modifier = Modifier.fillMaxWidth()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
            ) { index ->
                val page = pages[index]
                Box(modifier = Modifier.padding(start = 5.dp)) {
                    if (page == 1) {
                        MainPageView(
                            model = model,
                            quickButtons = quickButtons,
                            quickButtonsSettings = quickButtonsSettings,
                            store = store,
                            width = width,
                        )
                    } else {
                        PageView(
                            model = model,
                            quickButtons = quickButtons,
                            quickButtonsSettings = quickButtonsSettings,
                            page = page - 1,
                            width = width,
                        )
                    }
                }
            }
            if (!isMac()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(x = offsetX().dp, y = 13.dp),
                ) {
                    PageIndicatorView(model = model, size = 5f, quickButtons = quickButtons)
                }
            }
        }
        if (isMac()) {
            Box(modifier = Modifier.padding(top = 1.dp)) {
                PageIndicatorView(model = model, size = 9f, quickButtons = quickButtons)
            }
        }
    }
}

@Composable
fun ControlBarLandscapeView(model: Model = LocalModel.current, quickButtons: SettingsQuickButtons) {
    val statusOther = model.statusOther.collectAsState().value
    val quickButtonsState = model.quickButtons.collectAsState().value
    val controlBar = model.controlBar.collectAsState().value
    val width = controlBarWidth(quickButtons = quickButtons)

    Box(
        modifier = Modifier
            .padding(vertical = 0.dp)
            .width(width.dp),
    ) {
        ControlBarBackgroundView(controlBar = controlBar)
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!isPhone()) {
                    Spacer(Modifier.weight(1f))
                }
                Box(modifier = Modifier.width(controlBarWidthDefault.dp)) {
                    StatusView(model = model, status = statusOther)
                }
                Spacer(Modifier.weight(1f))
            }
            PagesView(
                model = model,
                quickButtons = quickButtonsState,
                quickButtonsSettings = quickButtons,
                width = width,
            )
        }
    }
}

package com.moblin.android.view.controlbar

import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.common.various.controlBarButtonSize
import com.moblin.android.common.various.controlBarQuickButtonNameSingleColumnSize
import com.moblin.android.common.various.controlBarQuickButtonNameSize
import com.moblin.android.common.various.controlBarQuickButtonSingleQuickButtonSize
import com.moblin.android.common.various.controlBarWidthBigQuickButtons
import com.moblin.android.common.various.controlBarWidthDefault
import com.moblin.android.common.various.smallFont
import com.moblin.android.common.view.ThermalStateView
import com.moblin.android.platform.Bundle
import com.moblin.android.platform.SystemImage
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.QuickButtons
import com.moblin.android.various.model.ShowingPanel
import com.moblin.android.various.model.StatusOther
import com.moblin.android.various.model.Store
import com.moblin.android.various.settings.SettingsQuickButtons
import com.moblin.android.various.storages.toThermalState
import com.moblin.android.various.utils.isMac
import com.moblin.android.various.utils.isPhone
import com.moblin.android.view.utils.HCenter

private fun edgesToIgnore(): List<String> {
    return if (isPhone()) {
        listOf("trailing")
    } else {
        emptyList()
    }
}

fun controlBarWidth(quickButtons: SettingsQuickButtons): Double {
    return if (quickButtons.bigButtons.value && quickButtons.twoColumns.value) {
        controlBarWidthBigQuickButtons.toDouble()
    } else {
        controlBarWidthDefault.toDouble()
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
    val pairs by quickButtons.pairs.collectAsState()
    val bigButtons by quickButtonsSettings.bigButtons.collectAsState()
    val twoColumns by quickButtonsSettings.twoColumns.collectAsState()
    val orientation = model.orientation

    fun buttonSize(): Float {
        return if (bigButtons) {
            controlBarQuickButtonSingleQuickButtonSize
        } else {
            controlBarButtonSize
        }
    }

    fun nameSize(): Float {
        return if (bigButtons) {
            controlBarQuickButtonNameSingleColumnSize
        } else {
            controlBarQuickButtonNameSize
        }
    }

    val pagePairs = remember(pairs, page) { model.getQuickButtonPairs(page = page + 1) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        pagePairs.forEach { pair ->
            key(pair.id) {
                if (twoColumns) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        val second = pair.second
                        if (second != null) {
                            QuickButtonsInnerView(
                                model = model,
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
                            model = model,
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
                        Box(
                            modifier = Modifier.width((width - 10).dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            QuickButtonsInnerView(
                                model = model,
                                quickButtons = quickButtons,
                                quickButtonsSettings = quickButtonsSettings,
                                orientation = orientation,
                                button = second,
                                size = buttonSize(),
                                nameSize = nameSize(),
                                nameWidth = (width - 10).toFloat(),
                            )
                        }
                    }
                    Box(
                        modifier = Modifier.width((width - 10).dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        QuickButtonsInnerView(
                            model = model,
                            quickButtons = quickButtons,
                            quickButtonsSettings = quickButtonsSettings,
                            orientation = orientation,
                            button = pair.first,
                            size = buttonSize(),
                            nameSize = nameSize(),
                            nameWidth = (width - 10).toFloat(),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusView(model: Model = LocalModel.current, status: StatusOther, modifier: Modifier = Modifier) {
    var presentingThermalState by remember { mutableStateOf(false) }
    val thermalState by status.thermalState.collectAsState()
    val digitalClock by status.digitalClock.collectAsState()

    Row(
        modifier = modifier
            .padding(start = 0.dp, bottom = 0.dp)
            .padding(end = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isPhone()) {
            BatteryView(model = model, battery = model.battery)
        }
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                presentingThermalState = !presentingThermalState
            },
        ) {
            ThermalStateView(thermalState = thermalState.toThermalState())
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
    val iconImage by store.iconImage.collectAsState()
    val image = remember(iconImage) { Bundle.image("${iconImage}NoBackground")?.asImageBitmap() }

    HCenter {
        Box(
            modifier = Modifier
                .size(controlBarButtonSize.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    model.toggleShowingPanel(type = null, panel = ShowingPanel.store)
                },
            contentAlignment = Alignment.Center,
        ) {
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.High,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(controlBarButtonSize.dp)
                .border(1.dp, Color(0x99EBEBF5), CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    model.toggleShowingPanel(type = null, panel = ShowingPanel.settings)
                },
            contentAlignment = Alignment.Center,
        ) {
            SystemImage(name = "gearshape", fontSize = 17.sp, tint = Color.White)
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
    modifier: Modifier = Modifier,
) {
    val enableScroll by quickButtonsSettings.enableScroll.collectAsState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .rotate(180f)
            .verticalScroll(state = rememberScrollState(), enabled = enableScroll),
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
    modifier: Modifier = Modifier,
) {
    val bigButtons by quickButtonsSettings.bigButtons.collectAsState()
    val twoColumns by quickButtonsSettings.twoColumns.collectAsState()

    fun buttonsWidth(): Double {
        return if (bigButtons && twoColumns) {
            width - 20
        } else {
            width - 10
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Box(
            modifier = Modifier
                .width(buttonsWidth().dp)
                .padding(vertical = 2.dp),
        ) {
            IconAndSettingsView(model = model, store = store)
        }
        PageView(
            model = model,
            quickButtons = quickButtons,
            quickButtonsSettings = quickButtonsSettings,
            page = 0,
            width = width,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier.width(buttonsWidth().dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            Box(modifier = Modifier.padding(top = 5.dp)) {
                StreamButton(model = model, show = model.show)
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

private class ControlBarLandscapePageScrollTargetBehavior(
    private val model: Model,
    private val scrollState: ScrollState,
    private val density: Density,
    private val decay: DecayAnimationSpec<Float>,
    private val containerWidth: Double,
    private val onTargetUpdated: () -> Unit,
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        val position = scrollState.value.toFloat()
        val targetPosition = decay.calculateTargetValue(position, initialVelocity)
        val target = with(density) {
            controlBarScrollTargetBehavior(
                model = model,
                containerWidth = containerWidth,
                targetPosition = targetPosition.toDp().value.toDouble(),
            ).toFloat().dp.toPx()
        }
        var previous = position
        animate(initialValue = position, targetValue = target, initialVelocity = initialVelocity) { value, _ ->
            previous += scrollBy(value - previous)
        }
        onTargetUpdated()
        return 0f
    }
}

@Composable
private fun PageIndicatorView(model: Model = LocalModel.current, size: Float, quickButtons: QuickButtons) {
    val pairs by quickButtons.pairs.collectAsState()
    val activePage by quickButtons.activePage.collectAsState()

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
            key(page) {
                SystemImage(
                    name = if (activePage == page) "circle.fill" else "circle",
                    fontSize = size.sp,
                    modifier = Modifier
                        .padding(bottom = 0.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            quickButtons.page = page
                            quickButtons.activePage.value = page
                            model.updateQuickButtonPairs()
                        },
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun PagesView(
    model: Model = LocalModel.current,
    quickButtons: QuickButtons,
    quickButtonsSettings: SettingsQuickButtons,
    width: Double,
    modifier: Modifier = Modifier,
) {
    val bigButtons by quickButtonsSettings.bigButtons.collectAsState()
    val twoColumns by quickButtonsSettings.twoColumns.collectAsState()
    val pairs by quickButtons.pairs.collectAsState()
    val activePage by quickButtons.activePage.collectAsState()

    fun offsetX(): Double {
        return if (bigButtons && twoColumns) {
            -6.0
        } else {
            -1.0
        }
    }

    val pages = buildList {
        add(1)
        for (page in 1 until controlBarPages) {
            if (pairs[page].isNotEmpty()) {
                add(page + 1)
            }
        }
    }

    Column(modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val containerWidth = maxWidth.value.toDouble()
            val scrollState = rememberScrollState()
            val density = LocalDensity.current
            val decay = rememberSplineBasedDecay<Float>()
            val scrollTargetBehavior = remember(model, scrollState, density, decay, containerWidth, pages) {
                ControlBarLandscapePageScrollTargetBehavior(
                    model = model,
                    scrollState = scrollState,
                    density = density,
                    decay = decay,
                    containerWidth = containerWidth,
                ) {
                    quickButtons.activePage.value = pages.getOrNull(quickButtons.page - 1)
                }
            }
            LaunchedEffect(activePage, pages, containerWidth) {
                val index = pages.indexOf(activePage)
                if (index >= 0) {
                    scrollState.animateScrollTo(
                        with(density) { (index * (containerWidth + 8)).dp.roundToPx() },
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .horizontalScroll(state = scrollState, flingBehavior = scrollTargetBehavior),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                pages.forEach { page ->
                    key(page) {
                        Box(
                            modifier = Modifier
                                .width(containerWidth.dp)
                                .fillMaxHeight()
                                .padding(start = 5.dp),
                            contentAlignment = Alignment.TopStart,
                        ) {
                            if (page == 1) {
                                MainPageView(
                                    model = model,
                                    quickButtons = quickButtons,
                                    quickButtonsSettings = quickButtonsSettings,
                                    store = model.store,
                                    width = width,
                                    modifier = Modifier.fillMaxHeight(),
                                )
                            } else {
                                PageView(
                                    model = model,
                                    quickButtons = quickButtons,
                                    quickButtonsSettings = quickButtonsSettings,
                                    page = page - 1,
                                    width = width,
                                    modifier = Modifier.fillMaxHeight(),
                                )
                            }
                        }
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
    val bigButtons by quickButtons.bigButtons.collectAsState()
    val twoColumns by quickButtons.twoColumns.collectAsState()
    val width = remember(bigButtons, twoColumns) { controlBarWidth(quickButtons = quickButtons) }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(width.dp),
    ) {
        ControlBarBackgroundView(controlBar = model.controlBar)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 0.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!isPhone()) {
                    Spacer(Modifier.weight(1f))
                }
                StatusView(
                    model = model,
                    status = model.statusOther,
                    modifier = Modifier.width(controlBarWidthDefault.dp),
                )
                Spacer(Modifier.weight(1f))
            }
            PagesView(
                model = model,
                quickButtons = model.quickButtons,
                quickButtonsSettings = quickButtons,
                width = width,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

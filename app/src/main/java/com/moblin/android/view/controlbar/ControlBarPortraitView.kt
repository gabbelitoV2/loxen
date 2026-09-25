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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import com.moblin.android.common.various.controlBarQuickButtonSingleQuickButtonSize
import com.moblin.android.common.various.controlBarWidthDefault
import com.moblin.android.common.view.ThermalStateView
import com.moblin.android.platform.Bundle
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Sheet
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.QuickButtons
import com.moblin.android.various.model.ShowingPanel
import com.moblin.android.various.model.StatusOther
import com.moblin.android.various.model.Store
import com.moblin.android.various.settings.SettingsQuickButtons
import com.moblin.android.various.storages.toThermalState

private class ControlBarPortraitPageScrollTargetBehavior(
    private val model: Model,
    private val scrollState: ScrollState,
    private val density: Density,
    private val decay: DecayAnimationSpec<Float>,
    private val containerHeight: Double,
    private val onTargetUpdated: () -> Unit,
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        val position = scrollState.value.toFloat()
        val targetPosition = decay.calculateTargetValue(position, initialVelocity)
        val target = with(density) {
            controlBarScrollTargetBehavior(
                model = model,
                containerWidth = containerHeight,
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
private fun QuickButtonsView(
    model: Model = LocalModel.current,
    quickButtons: QuickButtons,
    quickButtonsSettings: SettingsQuickButtons,
    page: Int,
    height: Double,
) {
    val pairs by quickButtons.pairs.collectAsState()
    val bigButtons by quickButtonsSettings.bigButtons.collectAsState()
    val twoColumns by quickButtonsSettings.twoColumns.collectAsState()

    fun buttonSize(): Float {
        return if (bigButtons) {
            controlBarQuickButtonSingleQuickButtonSize
        } else {
            controlBarButtonSize
        }
    }

    val pagePairs = remember(pairs, page) { model.getQuickButtonPairs(page = page + 1) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        pagePairs.forEach { pair ->
            key(pair.id) {
                if (twoColumns) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        val second = pair.second
                        if (second != null) {
                            QuickButtonsInnerView(
                                model = model,
                                quickButtons = quickButtons,
                                quickButtonsSettings = quickButtonsSettings,
                                orientation = model.orientation,
                                button = second,
                                size = buttonSize(),
                                nameSize = buttonSize(),
                                nameWidth = buttonSize(),
                            )
                        } else {
                            QuickButtonPlaceholderImage(size = buttonSize())
                        }
                        QuickButtonsInnerView(
                            model = model,
                            quickButtons = quickButtons,
                            quickButtonsSettings = quickButtonsSettings,
                            orientation = model.orientation,
                            button = pair.first,
                            size = buttonSize(),
                            nameSize = buttonSize(),
                            nameWidth = buttonSize(),
                        )
                    }
                } else {
                    val second = pair.second
                    if (second != null) {
                        Box(
                            modifier = Modifier.height((height - 10).dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            QuickButtonsInnerView(
                                model = model,
                                quickButtons = quickButtons,
                                quickButtonsSettings = quickButtonsSettings,
                                orientation = model.orientation,
                                button = second,
                                size = buttonSize(),
                                nameSize = buttonSize(),
                                nameWidth = buttonSize(),
                            )
                        }
                    }
                    Box(
                        modifier = Modifier.height((height - 10).dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        QuickButtonsInnerView(
                            model = model,
                            quickButtons = quickButtons,
                            quickButtonsSettings = quickButtonsSettings,
                            orientation = model.orientation,
                            button = pair.first,
                            size = buttonSize(),
                            nameSize = buttonSize(),
                            nameWidth = buttonSize(),
                        )
                    }
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
    modifier: Modifier = Modifier,
) {
    val enableScroll by quickButtonsSettings.enableScroll.collectAsState()

    Box(
        modifier = modifier
            .fillMaxHeight()
            .rotate(180f)
            .horizontalScroll(state = rememberScrollState(), enabled = enableScroll),
    ) {
        Box(
            modifier = Modifier.fillMaxHeight(),
            contentAlignment = Alignment.BottomCenter,
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
}

@Composable
private fun IconAndSettingsView(model: Model = LocalModel.current, store: Store) {
    val iconImage by store.iconImage.collectAsState()
    val image = remember(iconImage) { Bundle.image("${iconImage}NoBackground")?.asImageBitmap() }

    Row(
        modifier = Modifier
            .wrapContentWidth(unbounded = true)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = 2.dp)
                        .padding(bottom = 4.dp),
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.High,
                )
            }
        }
        Box(
            modifier = Modifier
                .padding(start = 10.dp)
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
private fun MainPageView(
    model: Model = LocalModel.current,
    quickButtons: QuickButtons,
    quickButtonsSettings: SettingsQuickButtons,
    status: StatusOther,
    height: Double,
    modifier: Modifier = Modifier,
) {
    val thermalState by status.thermalState.collectAsState()
    var presentingThermalState by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalAlignment = Alignment.Top,
    ) {
        PageView(
            model = model,
            quickButtons = quickButtons,
            quickButtonsSettings = quickButtonsSettings,
            page = 0,
            height = height,
            modifier = Modifier
                .weight(1f)
                .padding(top = 5.dp, start = 5.dp)
                .padding(end = 0.dp),
        )
        Column(
            modifier = Modifier
                .width(controlBarWidthDefault.dp)
                .padding(start = 0.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 3.dp)
                    .padding(end = 5.dp)
                    .padding(start = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        presentingThermalState = !presentingThermalState
                    },
                ) {
                    ThermalStateView(thermalState = thermalState)
                }
                Spacer(modifier = Modifier.weight(1f))
            }
            IconAndSettingsView(model = model, store = model.store)
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .padding(horizontal = 5.dp),
            ) {
                StreamButton(model = model, show = model.show)
            }
        }
    }
    Sheet(isPresented = presentingThermalState, onDismissRequest = { presentingThermalState = false }) {
        ThermalStateSheetView(
            presenting = presentingThermalState,
            onPresentingChange = { presentingThermalState = it },
        )
    }
}

@Composable
private fun PagesView(
    model: Model = LocalModel.current,
    quickButtons: QuickButtons,
    quickButtonsSettings: SettingsQuickButtons,
    height: Double,
    modifier: Modifier = Modifier,
) {
    val pairs by quickButtons.pairs.collectAsState()
    val activePage by quickButtons.activePage.collectAsState()

    val pages = buildList {
        add(1)
        for (page in 1 until controlBarPages) {
            if (pairs[page].isNotEmpty()) {
                add(page + 1)
            }
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val containerHeight = maxHeight.value.toDouble()
        val scrollState = rememberScrollState()
        val density = LocalDensity.current
        val decay = rememberSplineBasedDecay<Float>()
        val scrollTargetBehavior = remember(model, scrollState, density, decay, containerHeight, pages) {
            ControlBarPortraitPageScrollTargetBehavior(
                model = model,
                scrollState = scrollState,
                density = density,
                decay = decay,
                containerHeight = containerHeight,
            ) {
                quickButtons.activePage.value = pages.getOrNull(quickButtons.page - 1)
            }
        }
        LaunchedEffect(activePage, pages, containerHeight) {
            val index = pages.indexOf(activePage)
            if (index >= 0) {
                scrollState.animateScrollTo(
                    with(density) { (index * (containerHeight + 8)).dp.roundToPx() },
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = scrollState, flingBehavior = scrollTargetBehavior),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            pages.forEach { page ->
                key(page) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(containerHeight.dp),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        if (page == 1) {
                            MainPageView(
                                model = model,
                                quickButtons = quickButtons,
                                quickButtonsSettings = quickButtonsSettings,
                                status = model.statusOther,
                                height = height,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            PageView(
                                model = model,
                                quickButtons = quickButtons,
                                quickButtonsSettings = quickButtonsSettings,
                                page = page - 1,
                                height = height,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 5.dp, start = 5.dp, end = 5.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ControlBarPortraitView(model: Model = LocalModel.current, quickButtons: SettingsQuickButtons) {
    val bigButtons by quickButtons.bigButtons.collectAsState()
    val twoColumns by quickButtons.twoColumns.collectAsState()
    val height = remember(bigButtons, twoColumns) { controlBarWidth(quickButtons = quickButtons) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height.dp),
    ) {
        ControlBarBackgroundView(controlBar = model.controlBar)
        PagesView(
            model = model,
            quickButtons = model.quickButtons,
            quickButtonsSettings = model.database.quickButtonsGeneral,
            height = height,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

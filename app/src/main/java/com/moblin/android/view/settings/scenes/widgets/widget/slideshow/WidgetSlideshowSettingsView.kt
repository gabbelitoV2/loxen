package com.moblin.android.view.settings.scenes.widgets.widget.slideshow

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetSlideshow
import com.moblin.android.various.settings.SettingsWidgetSlideshowSlide
import com.moblin.android.various.settings.SettingsWidgetType
import com.moblin.android.various.utils.makeOffsets
import com.moblin.android.view.settings.scenes.autoswitchers.SwitcherTimePickerView
import com.moblin.android.view.settings.scenes.widgets.widget.WidgetNameView
import com.moblin.android.view.utils.AddButtonView
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.WidgetShortcutView
import com.moblin.android.various.model.resetSelectedScene

private fun widgets(database: Database): List<SettingsWidget> =
    database.widgets.filter {
        it.type == SettingsWidgetType.text || it.type == SettingsWidgetType.image
    }

@Composable
fun WidgetSlideshowSlidePickerView(
    database: Database,
    slide: SettingsWidgetSlideshowSlide,
) {
    val widgetList = widgets(database)
    Picker(
        title = "Widget",
        selection = slide.widgetId,
        options = listOf(null) + widgetList.map { it.id },
        text = { widgetId ->
            widgetList.firstOrNull { it.id == widgetId }?.name ?: "-- None --"
        },
        onChange = { widgetId -> slide.widgetId = widgetId },
    )
}

@Composable
fun WidgetSlideshowSlideSummaryView(
    model: Model = LocalModel.current,
    slide: SettingsWidgetSlideshowSlide,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DraggableItemPrefixView()
        val widget = slide.widgetId?.let { widgetId ->
            model.database.widgets.firstOrNull { it.id == widgetId }
        }
        if (widget != null) {
            WidgetNameView(widget = widget)
        } else {
            Text(localized("-- None --"))
        }
        Spacer(modifier = Modifier.weight(1f))
        Text("${slide.time}s")
    }
}

@Composable
private fun SlideView(
    model: Model = LocalModel.current,
    database: Database,
    slide: SettingsWidgetSlideshowSlide,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var previousWidgetId by remember { mutableStateOf(slide.widgetId) }
    if (previousWidgetId != slide.widgetId) {
        previousWidgetId = slide.widgetId
        model.resetSelectedScene(false, false)
    }
    NavigationLink(
        destination = {
            Form {
                Section {
                    WidgetSlideshowSlidePickerView(database = database, slide = slide)
                    SwitcherTimePickerView(
                        time = slide.time,
                        onTimeChange = { time ->
                            slide.time = time
                            model.resetSelectedScene(false, false)
                        },
                    )
                }
                val widget = slide.widgetId?.let { widgetId ->
                    model.database.widgets.firstOrNull { it.id == widgetId }
                }
                if (widget != null) {
                    ShortcutSectionView {
                        WidgetShortcutView(
                            model = model,
                            database = database,
                            widget = widget,
                        )
                    }
                }
            }
        },
    ) {
        WidgetSlideshowSlideSummaryView(model = model, slide = slide)
    }
}

private fun deleteSlide(
    model: Model,
    slideshow: SettingsWidgetSlideshow,
    at: Int,
) {
    slideshow.slides = slideshow.slides.filterIndexed { index, _ -> index != at }
    model.resetSelectedScene(false, false)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SlidesView(
    model: Model = LocalModel.current,
    slideshow: SettingsWidgetSlideshow,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Section(
        header = "Slides",
        footerContent = { SwipeLeftToDeleteHelpView(kind = localized("a slide")) },
    ) {
        slideshow.slides.forEach { slide ->
            key(slide.id) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                            onLongClick = {
                                val index = slideshow.slides.indexOfFirst { it.id == slide.id }
                                if (index >= 0) {
                                    deleteSlide(
                                        model = model,
                                        slideshow = slideshow,
                                        at = index,
                                    )
                                }
                            },
                        ),
                ) {
                    SlideView(
                        model = model,
                        database = model.database,
                        slide = slide,
                        onNavigate = onNavigate,
                    )
                }
            }
        }
        AddButtonView {
            slideshow.slides = slideshow.slides + SettingsWidgetSlideshowSlide()
            model.resetSelectedScene(false, false)
        }
    }
}

@Composable
fun WidgetSlideshowSettingsView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    onNavigate: (String) -> Unit = {},
) {
    SlidesView(model = model, slideshow = widget.slideshow, onNavigate = onNavigate)
}

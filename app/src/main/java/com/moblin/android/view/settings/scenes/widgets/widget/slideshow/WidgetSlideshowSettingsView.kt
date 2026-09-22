package com.moblin.android.view.settings.scenes.widgets.widget.slideshow

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetSlideshow
import com.moblin.android.various.settings.SettingsWidgetSlideshowSlide
import com.moblin.android.various.settings.SettingsWidgetType
import com.moblin.android.various.utils.makeOffsets
import com.moblin.android.view.settings.scenes.widgets.widget.WidgetNameView
import com.moblin.android.view.utils.AddButtonView
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView

private fun widgets(database: Database): List<SettingsWidget> =
    database.widgets.filter {
        it.type == SettingsWidgetType.TEXT || it.type == SettingsWidgetType.IMAGE
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSlideshowSlidePickerView(
    database: Database,
    slide: SettingsWidgetSlideshowSlide,
) {
    val widgetList = widgets(database)
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Widget", modifier = Modifier.weight(1f))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.weight(1f),
        ) {
            OutlinedTextField(
                value = widgetList.firstOrNull { it.id == slide.widgetId }?.name ?: "-- None --",
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("-- None --") },
                    onClick = {
                        slide.widgetId = null
                        expanded = false
                    },
                )
                widgetList.forEach { widget ->
                    DropdownMenuItem(
                        text = { WidgetNameView(widget = widget) },
                        onClick = {
                            slide.widgetId = widget.id
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun WidgetSlideshowSlideSummaryView(
    model: Model,
    slide: SettingsWidgetSlideshowSlide,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DraggableItemPrefixView()
        val widget = slide.widgetId?.let { model.findWidget(id = it) }
        if (widget != null) {
            WidgetNameView(widget = widget)
        } else {
            Text("-- None --")
        }
        Spacer(modifier = Modifier.weight(1f))
        Text("${slide.time}s")
    }
}

@Composable
private fun SlideView(
    model: Model,
    database: Database,
    slide: SettingsWidgetSlideshowSlide,
    onNavigate: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("slideSettings") },
    ) {
        WidgetSlideshowSlideSummaryView(model = model, slide = slide)
    }
}

private fun deleteSlide(
    model: Model,
    slideshow: SettingsWidgetSlideshow,
    at: List<Int>,
) {
    at.sortedDescending().forEach { index -> slideshow.slides.removeAt(index) }
    model.resetSelectedScene(changeScene = false, attachCamera = false)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SlidesView(
    model: Model,
    slideshow: SettingsWidgetSlideshow,
    onNavigate: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Slides", style = MaterialTheme.typography.titleMedium)
        slideshow.slides.forEach { slide ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            makeOffsets(slideshow.slides, slide.id)?.let { offsets ->
                                deleteSlide(model = model, slideshow = slideshow, at = offsets)
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
        AddButtonView {
            slideshow.slides.add(SettingsWidgetSlideshowSlide())
            model.resetSelectedScene(changeScene = false, attachCamera = false)
        }
        SwipeLeftToDeleteHelpView(kind = localized("a slide"))
    }
}

@Composable
fun WidgetSlideshowSettingsView(
    model: Model,
    widget: SettingsWidget,
    onNavigate: (String) -> Unit = {},
) {
    SlidesView(model = model, slideshow = widget.slideshow, onNavigate = onNavigate)
}

package com.moblin.android.view.settings.scenes.widgets.widget.wizard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.moblin.android.localized
import com.moblin.android.various.model.CreateWidgetWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsWidgetSlideshow
import com.moblin.android.various.settings.SettingsWidgetSlideshowSlide
import com.moblin.android.view.settings.scenes.widgets.widget.WidgetWizardSelectScenesNavigationView
import com.moblin.android.view.settings.scenes.widgets.widget.basicWidgetSettingsTitle
import com.moblin.android.view.settings.scenes.widgets.widget.slideshow.WidgetSlideshowSlideSummaryView
import com.moblin.android.view.utils.AddButtonView
import com.moblin.android.view.utils.CloseToolbar
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@Composable
private fun SlideView(
    model: Model = LocalModel.current,
    database: Database,
    slide: SettingsWidgetSlideshowSlide,
    presentingCreateWizard: Boolean,
    onChangePresentingCreateWizard: (Boolean) -> Unit,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("widgetWizardSlideshowSlideSettings") },
    ) {
        WidgetSlideshowSlideSummaryView(model = model, slide = slide)
    }
}

@Composable
private fun SlidesView(
    model: Model = LocalModel.current,
    slideshow: SettingsWidgetSlideshow,
    presentingCreateWizard: Boolean,
    onChangePresentingCreateWizard: (Boolean) -> Unit,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Column {
        Text("Slides")
        slideshow.slides.forEachIndexed { index, slide ->
            ContextMenuActions(
                actions = buildList<Pair<String, () -> Unit>> {
                    add(
                        localized("Delete") to {
                            slideshow.slides.removeAll { it.id == slide.id }
                        },
                    )
                    if (index > 0) {
                        add(
                            localized("Move Up") to {
                                slideshow.slides.moveElement(index, index - 1)
                            },
                        )
                    }
                    if (index < slideshow.slides.size - 1) {
                        add(
                            localized("Move Down") to {
                                slideshow.slides.moveElement(index, index + 1)
                            },
                        )
                    }
                },
            ) {
                SlideView(
                    model = model,
                    database = model.database,
                    slide = slide,
                    presentingCreateWizard = presentingCreateWizard,
                    onChangePresentingCreateWizard = onChangePresentingCreateWizard,
                    onNavigate = onNavigate,
                )
            }
        }
        AddButtonView {
            slideshow.slides.add(SettingsWidgetSlideshowSlide())
        }
        SwipeLeftToDeleteHelpView(kind = localized("a slide"))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetWizardSlideshowSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    createWidgetWizard: CreateWidgetWizard,
    slideshow: SettingsWidgetSlideshow,
    presentingCreateWizard: Boolean,
    onChangePresentingCreateWizard: (Boolean) -> Unit,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(basicWidgetSettingsTitle(createWidgetWizard)) },
                actions = {
                    CloseToolbar(
                        presenting = presentingCreateWizard,
                        onChangePresenting = onChangePresentingCreateWizard,
                    )
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            item {
                SlidesView(
                    model = model,
                    slideshow = slideshow,
                    presentingCreateWizard = presentingCreateWizard,
                    onChangePresentingCreateWizard = onChangePresentingCreateWizard,
                    onNavigate = onNavigate,
                )
            }
            item {
                WidgetWizardSelectScenesNavigationView(
                    model = model,
                    database = database,
                    createWidgetWizard = createWidgetWizard,
                    presentingCreateWizard = presentingCreateWizard,
                    onChangePresentingCreateWizard = onChangePresentingCreateWizard,
                    enabled = slideshow.slides.isNotEmpty(),
                )
            }
        }
    }
}

@Composable
private fun ContextMenuActions(
    actions: List<Pair<String, () -> Unit>>,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { expanded = true })
            },
    ) {
        content()
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.first) },
                    onClick = {
                        expanded = false
                        action.second()
                    },
                )
            }
        }
    }
}

private fun <T> MutableList<T>.moveElement(fromIndex: Int, toIndex: Int) {
    if (fromIndex == toIndex) {
        return
    }
    val element = removeAt(fromIndex)
    add(toIndex, element)
}

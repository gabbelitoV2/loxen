package com.moblin.android.view.settings.scenes.widgets.widget.wizard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.CreateWidgetWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsWidgetSlideshow
import com.moblin.android.various.settings.SettingsWidgetSlideshowSlide
import com.moblin.android.view.settings.scenes.autoswitchers.SwitcherTimePickerView
import com.moblin.android.view.settings.scenes.widgets.widget.WidgetWizardSelectScenesNavigationView
import com.moblin.android.view.settings.scenes.widgets.widget.basicWidgetSettingsTitle
import com.moblin.android.view.settings.scenes.widgets.widget.slideshow.WidgetSlideshowSlidePickerView
import com.moblin.android.view.settings.scenes.widgets.widget.slideshow.WidgetSlideshowSlideSummaryView
import com.moblin.android.view.utils.AddButtonView
import com.moblin.android.view.utils.CloseToolbar
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView

@Composable
private fun SlideView(
    model: Model = LocalModel.current,
    database: Database,
    slide: SettingsWidgetSlideshowSlide,
    presentingCreateWizard: Boolean,
    onChangePresentingCreateWizard: (Boolean) -> Unit,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            Form(
                toolbar = {
                    CloseToolbar(
                        presenting = presentingCreateWizard,
                        onPresentingChange = onChangePresentingCreateWizard,
                    )
                },
            ) {
                WidgetSlideshowSlidePickerView(database = database, slide = slide)
                SwitcherTimePickerView(
                    time = slide.time,
                    onTimeChange = { slide.time = it },
                )
            }
        },
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
    Section(
        header = localized("Slides"),
        footerContent = {
            SwipeLeftToDeleteHelpView(kind = localized("a slide"))
        },
    ) {
        slideshow.slides.forEachIndexed { index, slide ->
            key(slide.id) {
                ContextMenuActions(
                    actions = buildList<Pair<String, () -> Unit>> {
                        add(
                            localized("Delete") to {
                                slideshow.slides = slideshow.slides.filterNot { it.id == slide.id }
                            },
                        )
                        if (index > 0) {
                            add(
                                localized("Move Up") to {
                                    slideshow.slides = slideshow.slides.moveElement(index, index - 1)
                                },
                            )
                        }
                        if (index < slideshow.slides.size - 1) {
                            add(
                                localized("Move Down") to {
                                    slideshow.slides = slideshow.slides.moveElement(index, index + 1)
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
        }
        AddButtonView {
            slideshow.slides = slideshow.slides + SettingsWidgetSlideshowSlide()
        }
    }
}

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
    Form(
        title = basicWidgetSettingsTitle(createWidgetWizard),
        toolbar = {
            CloseToolbar(
                presenting = presentingCreateWizard,
                onPresentingChange = onChangePresentingCreateWizard,
            )
        },
    ) {
        SlidesView(
            model = model,
            slideshow = slideshow,
            presentingCreateWizard = presentingCreateWizard,
            onChangePresentingCreateWizard = onChangePresentingCreateWizard,
            onNavigate = onNavigate,
        )
        WidgetWizardSelectScenesNavigationView(
            model = model,
            database = database,
            createWidgetWizard = createWidgetWizard,
            presentingCreateWizard = presentingCreateWizard,
            onPresentingCreateWizardChange = onChangePresentingCreateWizard,
        )
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
    }
    if (expanded) {
        AlertDialog(
            onDismissRequest = { expanded = false },
            confirmButton = {
                Text(
                    text = localized("Cancel"),
                    style = formBodyStyle,
                    color = formPalette().accent,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        expanded = false
                    },
                )
            },
            text = {
                Column {
                    actions.forEach { action ->
                        Text(
                            text = action.first,
                            style = formBodyStyle,
                            color = formPalette().label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    expanded = false
                                    action.second()
                                }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            },
        )
    }
}

private fun <T> List<T>.moveElement(fromIndex: Int, toIndex: Int): List<T> {
    if (fromIndex == toIndex) {
        return this
    }
    val list = toMutableList()
    val element = list.removeAt(fromIndex)
    list.add(toIndex, element)
    return list
}

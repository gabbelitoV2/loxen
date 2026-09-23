package com.moblin.android.view.settings.scenes.widgets.widget.wizard

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.ButtonRole
import com.moblin.android.platform.swiftui.ConfirmationDialog
import com.moblin.android.platform.swiftui.DialogActions
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
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
                    actions = {
                        Button("Delete", role = ButtonRole.destructive) {
                            slideshow.slides = slideshow.slides.filterNot { it.id == slide.id }
                        }
                        if (index > 0) {
                            Button("Move Up") {
                                slideshow.slides = slideshow.slides.moveElement(index, index - 1)
                            }
                        }
                        if (index < slideshow.slides.size - 1) {
                            Button("Move Down") {
                                slideshow.slides = slideshow.slides.moveElement(index, index + 1)
                            }
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
    actions: DialogActions.() -> Unit,
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
    ConfirmationDialog(title = "", isPresented = expanded, onDismissRequest = { expanded = false }, actions = actions)
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

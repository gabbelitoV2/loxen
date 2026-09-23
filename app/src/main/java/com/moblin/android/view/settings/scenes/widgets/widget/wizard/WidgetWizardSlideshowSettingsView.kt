package com.moblin.android.view.settings.scenes.widgets.widget.wizard

import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.moving
import com.moblin.android.platform.swiftui.removing
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
import com.moblin.android.view.utils.ContextMenuDeleteButton
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
        ForEach(
            slideshow.slides,
            id = { it.id },
            onDelete = { offsets ->
                slideshow.slides = slideshow.slides.removing(atOffsets = offsets)
            },
            onMove = { froms, to ->
                slideshow.slides = slideshow.slides.moving(fromOffsets = froms, toOffset = to)
            },
        ) { slide ->
            ContextMenuDeleteButton(
                action = {
                    slideshow.slides = slideshow.slides.filterNot { it.id == slide.id }
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

package com.moblin.android.view.settings.scenes.widgets.widget.wizard

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.CreateWidgetWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsWidgetBingoCard
import com.moblin.android.view.settings.scenes.widgets.widget.WidgetWizardSelectScenesNavigationView
import com.moblin.android.view.settings.scenes.widgets.widget.basicWidgetSettingsTitle
import com.moblin.android.view.settings.scenes.widgets.widget.bingocard.BingCardWidgetSquaresView
import com.moblin.android.view.utils.CloseToolbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetWizardBingoCardSettingsView(
    model: Model,
    database: Database,
    bingoCard: SettingsWidgetBingoCard,
    createWidgetWizard: CreateWidgetWizard,
    presentingCreateWizard: Boolean,
    onPresentingCreateWizardChange: (Boolean) -> Unit,
) {
    var squaresText by remember { mutableStateOf(bingoCard.squaresText.value) }
    var lastSquaresText by remember { mutableStateOf(bingoCard.squaresText.value) }

    LaunchedEffect(bingoCard.squaresText.value) {
        squaresText = bingoCard.squaresText.value
    }

    LaunchedEffect(squaresText) {
        if (squaresText != lastSquaresText) {
            lastSquaresText = squaresText
            bingoCard.squaresTextChanged()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(basicWidgetSettingsTitle(createWidgetWizard))
                },
                actions = {
                    CloseToolbar(presentingCreateWizard, onPresentingCreateWizardChange)
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
        ) {
            item {
                BingCardWidgetSquaresView(squaresText) { newValue ->
                    squaresText = newValue
                }
            }
            item {
                WidgetWizardSelectScenesNavigationView(
                    model = model,
                    database = database,
                    createWidgetWizard = createWidgetWizard,
                    presentingCreateWizard = presentingCreateWizard,
                    onPresentingCreateWizardChange = onPresentingCreateWizardChange,
                )
            }
        }
    }
}

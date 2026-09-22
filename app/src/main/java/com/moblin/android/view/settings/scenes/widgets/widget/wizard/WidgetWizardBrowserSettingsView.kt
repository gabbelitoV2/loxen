package com.moblin.android.view.settings.scenes.widgets.widget.wizard

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.moblin.android.common.various.isValidHttpUrl
import com.moblin.android.various.model.CreateWidgetWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsWidgetBrowser
import com.moblin.android.view.settings.scenes.widgets.widget.WidgetWizardSelectScenesNavigationView
import com.moblin.android.view.settings.scenes.widgets.widget.basicWidgetSettingsTitle
import com.moblin.android.view.utils.CloseToolbar
import com.moblin.android.LocalModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetWizardBrowserSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    createWidgetWizard: CreateWidgetWizard,
    browser: SettingsWidgetBrowser,
    presentingCreateWizard: Boolean,
    onPresentingCreateWizardChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val url by browser.url.collectAsState()
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(basicWidgetSettingsTitle(createWidgetWizard))
                },
                actions = {
                    CloseToolbar(
                        presenting = presentingCreateWizard,
                        onPresentingChange = onPresentingCreateWizardChange
                    )
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            item {
                Text(
                    text = "URL",
                    style = MaterialTheme.typography.titleSmall
                )
            }
            item {
                OutlinedTextField(
                    value = url,
                    onValueChange = { browser.url.value = it },
                    placeholder = { Text("https://example.com") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                val message = isValidHttpUrl(url)
                if (message != null) {
                    Text(
                        text = message,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            item {
                WidgetWizardSelectScenesNavigationView(
                    model = model,
                    database = database,
                    createWidgetWizard = createWidgetWizard,
                    presentingCreateWizard = presentingCreateWizard,
                    onPresentingCreateWizardChange = onPresentingCreateWizardChange,
                    enabled = isValidHttpUrl(url) == null && url.isNotEmpty()
                )
            }
        }
    }
}

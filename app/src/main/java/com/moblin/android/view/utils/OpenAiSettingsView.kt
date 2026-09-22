package com.moblin.android.view.utils

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.moblin.android.common.various.isValidHttpUrl
import com.moblin.android.localized
import com.moblin.android.various.settings.SettingsOpenAi

@Composable
fun OpenAiSettingsView(ai: SettingsOpenAi) {
    val baseUrl = ai.baseUrl.collectAsState().value
    val apiKey = ai.apiKey.collectAsState().value
    val model = ai.model.collectAsState().value
    val personality = ai.personality.collectAsState().value
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "OpenAI compatible service",
            style = MaterialTheme.typography.titleSmall,
        )
        TextEditNavigationView(
            title = localized("Base URL"),
            value = baseUrl,
            onChange = { isValidHttpUrl() },
            onSubmit = { ai.baseUrl.value = it },
        )
        TextEditNavigationView(
            title = localized("API key"),
            value = apiKey,
            onSubmit = { ai.apiKey.value = it },
            sensitive = true,
        )
        TextEditNavigationView(
            title = localized("Model"),
            value = model,
            onSubmit = { ai.model.value = it },
        )
        MultiLineTextFieldNavigationView(
            title = localized("Personality"),
            placeholder = "You give fast and short answers.",
            value = personality,
            onSubmit = { ai.personality.value = it },
        )
    }
}

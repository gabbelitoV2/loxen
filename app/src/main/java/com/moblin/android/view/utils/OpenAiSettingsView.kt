package com.moblin.android.view.utils

import androidx.compose.runtime.Composable
import com.moblin.android.common.various.isValidHttpUrl
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.settings.SettingsOpenAi

@Composable
fun OpenAiSettingsView(ai: SettingsOpenAi) {
    val baseUrl = ai.baseUrl
    val apiKey = ai.apiKey
    val model = ai.model
    val personality = ai.personality
    Section(header = localized("OpenAI compatible service")) {
        TextEditNavigationView(
            title = localized("Base URL"),
            value = baseUrl,
            onChange = { isValidHttpUrl(it) },
            onSubmit = { ai.baseUrl = it },
        )
        TextEditNavigationView(
            title = localized("API key"),
            value = apiKey,
            onSubmit = { ai.apiKey = it },
            sensitive = true,
        )
        TextEditNavigationView(
            title = localized("Model"),
            value = model,
            onSubmit = { ai.model = it },
        )
        MultiLineTextFieldNavigationView(
            title = localized("Personality"),
            placeholder = "You give fast and short answers.",
            value = personality,
            onSubmit = { ai.personality = it },
            onValueChange = { ai.personality = it },
        )
    }
}

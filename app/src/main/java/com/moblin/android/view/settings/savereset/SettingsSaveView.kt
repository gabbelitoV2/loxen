package com.moblin.android.view.settings.savereset

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.moblin.android.LocalModel
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.view.settings.FormButton
import com.moblin.android.view.utils.HCenter
import kotlinx.coroutines.delay

@Composable
fun SettingsSaveView(model: Model = LocalModel.current) {
    var showSaved by remember { mutableStateOf(false) }
    if (showSaved) {
        HCenter {
            Text(text = localized("Saved 😅"))
        }
    } else {
        FormButton(title = "Save settings", centered = true) {
            model.storeSettings()
            showSaved = true
        }
    }
    LaunchedEffect(showSaved) {
        if (showSaved) {
            delay(3000)
            showSaved = false
        }
    }
}

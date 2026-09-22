package com.moblin.android.view.settings.savereset

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.moblin.android.various.model.Model
import com.moblin.android.view.utils.HCenter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsSaveView(model: Model) {
    var showSaved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    HCenter {
        if (showSaved) {
            Text("Saved 😅")
        } else {
            Button(onClick = {
                scope.launch {
                    model.storeSettings()
                    showSaved = true
                }
            }) {
                Text("Save settings")
            }
        }
    }

    LaunchedEffect(showSaved) {
        if (showSaved) {
            delay(3000)
            showSaved = false
        }
    }
}

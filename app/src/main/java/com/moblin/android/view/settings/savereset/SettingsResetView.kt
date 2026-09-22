package com.moblin.android.view.settings.savereset

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.view.utils.HCenter
import kotlinx.coroutines.launch
import com.moblin.android.LocalModel

@Composable
fun SettingsResetView(model: Model = LocalModel.current) {
    var presentingResetConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    HCenter {
        Button(
            onClick = { presentingResetConfirm = true },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text(localized("Reset settings"))
        }
    }
    if (presentingResetConfirm) {
        AlertDialog(
            onDismissRequest = { presentingResetConfirm = false },
            title = { Text(localized("Are you sure?")) },
            confirmButton = {
                TextButton(onClick = {
                    presentingResetConfirm = false
                    scope.launch {
                        model.settings.reset()
                        model.updateQuickButtonPairs()
                        TODO("setCurrentStream, reloadStream, resetSelectedScene, loadStealthModeImage, loadControlBarBackgroundImage, loadFaceBackgroundImage")
                    }
                }) {
                    Text(
                        localized("Reset settings"),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        )
    }
}

package com.moblin.android.platform

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState

@Composable
fun PhotosPicker(
    isPresented: MutableState<Boolean>,
    selection: MutableState<Uri?>,
    matching: ActivityResultContracts.PickVisualMedia.VisualMediaType,
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selection.value = uri
        }
    }
    LaunchedEffect(isPresented.value) {
        if (isPresented.value) {
            launcher.launch(PickVisualMediaRequest(matching))
            isPresented.value = false
        }
    }
}

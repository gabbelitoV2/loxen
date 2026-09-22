package com.moblin.android.view.utils

import androidx.compose.runtime.Composable

@Composable
fun CreateButtonView(action: () -> Unit) {
    TextButtonView("Create") {
        action()
    }
}

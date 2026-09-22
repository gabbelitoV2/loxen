package com.moblin.android.view.utils

import androidx.compose.runtime.Composable

@Composable
fun AddButtonView(action: () -> Unit) {
    TextButtonView(title = "Add", action = action)
}

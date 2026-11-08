package com.moblin.android.view.utils

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier

enum class Axis {
    vertical,
    horizontal,
}

@Composable
fun EndAnchoredScrollView(axis: Axis, content: @Composable () -> Unit) {
    if (isVertical(axis)) {
        val scrollState = rememberScrollState()
        LaunchedEffect(scrollState.maxValue) {
            if (scrollState.maxValue > 0) {
                scrollState.scrollTo(scrollState.maxValue)
            }
        }
        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
            content()
        }
    } else {
        val scrollState = rememberScrollState()
        LaunchedEffect(scrollState.maxValue) {
            if (scrollState.maxValue > 0) {
                scrollState.scrollTo(scrollState.maxValue)
            }
        }
        Row(modifier = Modifier.fillMaxSize().horizontalScroll(scrollState)) {
            content()
        }
    }
}

private fun isVertical(axis: Axis): Boolean {
    return axis == Axis.vertical
}

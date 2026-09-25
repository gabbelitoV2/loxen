package com.moblin.android.platform.uikit

import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
import com.moblin.android.AppDelegate

object UIColor {
    val secondarySystemBackground: Color
        get() = if (isDark()) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)

    private fun isDark(): Boolean {
        return try {
            val uiMode = AppDelegate.context.resources.configuration.uiMode
            (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        } catch (error: Throwable) {
            false
        }
    }
}

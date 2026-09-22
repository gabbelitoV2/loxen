package com.moblin.android

import androidx.compose.runtime.staticCompositionLocalOf
import com.moblin.android.various.model.Model

val LocalModel = staticCompositionLocalOf<Model> { error("LocalModel is not provided") }

val LocalOnNavigate = staticCompositionLocalOf<(String) -> Unit> { {} }

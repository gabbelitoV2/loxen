package com.moblin.android.various.model

import com.moblin.android.platform.appintents.AppDependencyManager

fun Model.setupAppIntents() {
    AppDependencyManager.shared.add(dependency = this)
}

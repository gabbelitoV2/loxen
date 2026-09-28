package com.moblin.android

import com.moblin.android.platform.loxen.Loxen

fun localized(text: String): String = Loxen.rename(text)

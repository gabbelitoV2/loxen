package com.moblin.android.platform

import androidx.core.content.FileProvider
import java.lang.reflect.Modifier

object FileProviderRoots {
    fun forget() {
        for (field in FileProvider::class.java.declaredFields) {
            if (!Modifier.isStatic(field.modifiers) || !MutableMap::class.java.isAssignableFrom(field.type)) {
                continue
            }
            field.isAccessible = true
            val cache = field.get(null) as? MutableMap<*, *> ?: continue
            synchronized(cache) {
                cache.clear()
            }
        }
    }
}

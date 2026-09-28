package com.moblin.android.platform.appintents

import android.content.Intent
import android.util.Log
import com.moblin.android.platform.loxen.Loxen
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "AppIntents"

typealias LocalizedStringResource = String

class IntentDescription(val descriptionText: LocalizedStringResource)

class IntentResult private constructor() {
    companion object {
        private val done = IntentResult()

        fun result(): IntentResult = done
    }
}

interface AppIntent {
    suspend fun perform(): IntentResult
}

class Dependency<T : Any>(private val type: KClass<T>) : ReadOnlyProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = AppDependencyManager.shared.dependency(type)
}

inline fun <reified T : Any> Dependency(): Dependency<T> = Dependency(T::class)

class AppDependencyManager internal constructor() {
    private val dependencies = mutableListOf<Any>()
    private val waiting = mutableListOf<() -> Unit>()

    fun <T : Any> add(dependency: T) {
        val ready = synchronized(this) {
            dependencies.removeAll { it::class == dependency::class }
            dependencies.add(dependency)
            waiting.toList().also { waiting.clear() }
        }
        for (block in ready) {
            block()
        }
    }

    internal fun <T : Any> dependency(type: KClass<T>): T {
        val found = synchronized(this) { dependencies.lastOrNull { type.isInstance(it) } }
            ?: throw IllegalStateException("AppDependencyManager has no dependency of type ${type.simpleName}")
        @Suppress("UNCHECKED_CAST")
        return found as T
    }

    internal fun whenAdded(block: () -> Unit) {
        val now = synchronized(this) {
            if (dependencies.isEmpty()) {
                waiting.add(block)
                false
            } else {
                true
            }
        }
        if (now) {
            block()
        }
    }

    internal fun reset() {
        synchronized(this) {
            dependencies.clear()
            waiting.clear()
        }
    }

    companion object {
        val shared: AppDependencyManager by lazy { AppDependencyManager() }
    }
}

enum class ShortcutTileColor {
    red,
    tangerine,
    orange,
    yellow,
    lime,
    grayGreen,
    lightBlue,
    navy,
    blue,
    teal,
    purple,
    pink,
    grape,
    grayBlue,
    grayBrown,
}

object AppShortcutPhraseToken {
    const val applicationName = Loxen.appName
}

class AppShortcut(
    val intent: AppIntent,
    val phrases: List<String>,
    val shortTitle: LocalizedStringResource,
    val systemImageName: String,
)

interface AppShortcutsProvider {
    val shortcutTileColor: ShortcutTileColor
        get() = ShortcutTileColor.navy

    val appShortcuts: List<AppShortcut>

    fun updateAppShortcutParameters() {}
}

object AppShortcuts {
    const val actionPrefix = "com.moblin.android.shortcut."
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun action(shortcut: AppShortcut): String = actionPrefix + shortcut.intent.javaClass.simpleName

    fun perform(provider: AppShortcutsProvider, intent: Intent?): Boolean {
        val action = intent?.action ?: return false
        if (!action.startsWith(actionPrefix)) {
            return false
        }
        val shortcut = provider.appShortcuts.firstOrNull { action(it) == action }
        if (shortcut == null) {
            Log.i(TAG, "No app shortcut for $action")
            return true
        }
        Log.i(TAG, "Performing app shortcut ${shortcut.shortTitle}")
        AppDependencyManager.shared.whenAdded {
            scope.launch {
                try {
                    shortcut.intent.perform()
                } catch (error: Exception) {
                    Log.i(TAG, "App shortcut ${shortcut.shortTitle} failed: $error")
                }
            }
        }
        return true
    }
}

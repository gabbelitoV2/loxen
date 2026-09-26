package com.moblin.android.platform.uikit

import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.moblin.android.AppDelegate

val UIApplication.supportsAlternateIcons: Boolean
    get() = AppIcons.aliases(AppDelegate.context).size > 1

val UIApplication.alternateIconName: String?
    get() = AppIcons.selected(AppDelegate.context)?.takeIf { it != AppIcons.primaryIconName }

fun UIApplication.setAlternateIconName(alternateIconName: String?, completionHandler: ((Throwable?) -> Unit)? = null) {
    val error = AppIcons.select(AppDelegate.context, alternateIconName)
    completionHandler?.invoke(error)
}

internal object AppIcons {
    private const val TAG = "AppIcons"
    const val primaryIconName = "AppIcon"

    private fun iconName(info: ActivityInfo): String = info.name.substringAfterLast('.')

    private fun activities(context: Context): List<ActivityInfo> {
        val flags = PackageManager.GET_ACTIVITIES or PackageManager.MATCH_DISABLED_COMPONENTS
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, flags)
        }
        return info.activities.orEmpty().toList()
    }

    fun aliases(context: Context): List<ActivityInfo> {
        return activities(context).filter { it.targetActivity != null && iconName(it).startsWith(primaryIconName) }
    }

    private fun isEnabled(context: Context, info: ActivityInfo): Boolean {
        return when (context.packageManager.getComponentEnabledSetting(ComponentName(context.packageName, info.name))) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> info.enabled
            else -> false
        }
    }

    fun selected(context: Context): String? {
        return aliases(context).firstOrNull { isEnabled(context, it) }?.let { iconName(it) }
    }

    fun select(context: Context, name: String?): Throwable? {
        val wanted = name ?: primaryIconName
        return try {
            val aliases = aliases(context)
            val target = aliases.firstOrNull { iconName(it) == wanted }
                ?: return IllegalArgumentException("No app icon named $wanted")
            setEnabled(context, target, true)
            for (alias in aliases) {
                if (alias !== target) {
                    setEnabled(context, alias, false)
                }
            }
            Log.i(TAG, "App icon is $wanted")
            null
        } catch (error: Exception) {
            error
        }
    }

    private fun setEnabled(context: Context, info: ActivityInfo, enabled: Boolean) {
        if (isEnabled(context, info) == enabled) {
            return
        }
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context.packageName, info.name),
            state,
            PackageManager.DONT_KILL_APP,
        )
    }
}

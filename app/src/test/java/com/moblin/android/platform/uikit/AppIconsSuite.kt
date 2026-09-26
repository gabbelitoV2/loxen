package com.moblin.android.platform.uikit

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import com.moblin.android.R
import com.moblin.android.various.model.plainIcon
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AppIconsSuite {
    private lateinit var application: Application
    private val icons = listOf(
        "AppIcon",
        "AppIconSanDiego",
        "AppIconKing",
        "AppIconQueen",
        "AppIconLooking",
        "AppIconPixels",
        "AppIconHeart",
        "AppIconPink",
        "AppIconHappy",
        "AppIconMillionaire",
        "AppIconBillionaire",
        "AppIconTrillionaire",
        "AppIconTetris",
        "AppIconTub",
        "AppIconGoblin",
        "AppIconGoblina",
        "AppIconIreland",
        "AppIconPeru",
    )

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication()
    }

    private fun component(icon: String) = ComponentName(application.packageName, "com.moblin.android.$icon")

    private fun enabledIcons(): List<String> {
        return AppIcons.aliases(application)
            .filter {
                when (application.packageManager.getComponentEnabledSetting(component(it.name.substringAfterLast('.')))) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> it.enabled
                    else -> false
                }
            }
            .map { it.name.substringAfterLast('.') }
    }

    @Test
    fun everyStoreIconHasALauncherAliasAndOnlyThePlainOneIsEnabled() {
        val aliases = AppIcons.aliases(application)
        assertEquals(icons.toSet(), aliases.map { it.name.substringAfterLast('.') }.toSet())
        for (alias in aliases) {
            assertEquals("com.moblin.android.MainActivity", alias.targetActivity)
            assertTrue(alias.icon != 0)
        }
        assertEquals(listOf(plainIcon.id), enabledIcons())
        assertTrue(UIApplication.shared.supportsAlternateIcons)
        assertNull(UIApplication.shared.alternateIconName)
        assertEquals(R.mipmap.app_icon, application.applicationInfo.icon)
    }

    @Test
    fun theLauncherShowsTheEnabledAliasWithTheShortcuts() {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(application.packageName)
        val activities = application.packageManager.queryIntentActivities(launcher, 0)
        assertEquals(listOf("com.moblin.android.AppIcon"), activities.map { it.activityInfo.name })
        val info = application.packageManager.getActivityInfo(component("AppIcon"), PackageManager.GET_META_DATA)
        assertEquals(R.xml.shortcuts, info.metaData.getInt("android.app.shortcuts"))
    }

    @Test
    fun choosingAnIconEnablesOnlyItsAlias() {
        var error: Throwable? = IllegalStateException("Not called")
        UIApplication.shared.setAlternateIconName("AppIconSanDiego") { error = it }
        assertNull(error)
        assertEquals(listOf("AppIconSanDiego"), enabledIcons())
        assertEquals("AppIconSanDiego", UIApplication.shared.alternateIconName)
        assertEquals(
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            application.packageManager.getComponentEnabledSetting(component("AppIcon")),
        )
        UIApplication.shared.setAlternateIconName(null) { error = it }
        assertNull(error)
        assertEquals(listOf("AppIcon"), enabledIcons())
        assertNull(UIApplication.shared.alternateIconName)
    }

    @Test
    fun anUnknownIconFailsAndKeepsTheCurrentOne() {
        var error: Throwable? = null
        UIApplication.shared.setAlternateIconName("AppIconMissing") { error = it }
        assertNotNull(error)
        assertEquals(listOf("AppIcon"), enabledIcons())
    }
}

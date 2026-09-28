package com.moblin.android.platform.uikit

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import com.moblin.android.R
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication()
    }

    @Test
    fun theAppIsLoxenWithItsOwnIconAndNoMoblinIconAliases() {
        assertEquals("com.loxen.app", application.packageName)
        assertEquals("Loxen", application.packageManager.getApplicationLabel(application.applicationInfo).toString())
        assertEquals(R.mipmap.ic_launcher, application.applicationInfo.icon)
        assertTrue(AppIcons.aliases(application).isEmpty())
        assertFalse(UIApplication.shared.supportsAlternateIcons)
        assertNull(UIApplication.shared.alternateIconName)
    }

    @Test
    fun theLauncherStartsMainActivityWithTheShortcuts() {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(application.packageName)
        val activities = application.packageManager.queryIntentActivities(launcher, 0)
        assertEquals(listOf("com.moblin.android.MainActivity"), activities.map { it.activityInfo.name })
        val info = application.packageManager.getActivityInfo(
            ComponentName(application.packageName, "com.moblin.android.MainActivity"),
            PackageManager.GET_META_DATA,
        )
        assertEquals(R.xml.shortcuts, info.metaData.getInt("android.app.shortcuts"))
    }

    @Test
    fun choosingAMoblinIconFailsAndKeepsTheLauncherIcon() {
        var error: Throwable? = null
        UIApplication.shared.setAlternateIconName("AppIconSanDiego") { error = it }
        assertNotNull(error)
        assertNull(UIApplication.shared.alternateIconName)
    }
}

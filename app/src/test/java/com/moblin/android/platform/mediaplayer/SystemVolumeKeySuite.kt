package com.moblin.android.platform.mediaplayer

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.core.Notification
import com.moblin.android.platform.core.NotificationCenter
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class SystemVolumeKeySuite {
    private lateinit var audioManager: AudioManager
    private val observer = Any()
    private val received = mutableListOf<Notification>()

    @Before
    fun setUp() {
        audioManager = context().getSystemService(AudioManager::class.java)
        MPVolumeView(frame = CGRect.zero)
        NotificationCenter.default.addObserver(observer, SystemVolume.didChangeNotification, null) {
            received.add(it)
        }
    }

    @After
    fun tearDown() {
        NotificationCenter.default.removeObserver(observer)
    }

    private fun context(): Context {
        return ApplicationProvider.getApplicationContext()
    }

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun maxIndex(): Int {
        return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    }

    private fun setMediaIndex(index: Int) {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0)
    }

    private fun press(keyCode: Int, action: Int = KeyEvent.ACTION_DOWN, repeatCount: Int = 0) {
        SystemVolume.volumeKeyPressed(KeyEvent(0, 0, action, keyCode, repeatCount))
        runMain()
    }

    private fun broadcast(index: Int) {
        context().sendBroadcast(
            Intent(SystemVolume.volumeChangedAction)
                .putExtra(SystemVolume.extraStreamType, AudioManager.STREAM_MUSIC)
                .putExtra(SystemVolume.extraStreamValue, index),
        )
        runMain()
    }

    private fun volume(notification: Notification): Float {
        return notification.userInfo["Volume"] as Float
    }

    private fun sequenceNumber(notification: Notification): Int {
        return notification.userInfo["SequenceNumber"] as Int
    }

    @Test
    fun volumeKeysOfTheActivityControlTheMediaVolume() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        SystemVolume.install(activity)
        assertEquals(AudioManager.STREAM_MUSIC, activity.volumeControlStream)
        setMediaIndex(maxIndex())
        deliverThroughTheWindow(activity, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP))
        deliverThroughTheWindow(activity, KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_UP))
        assertEquals(1f, volume(received.single()))
    }

    private fun deliverThroughTheWindow(activity: ComponentActivity, event: KeyEvent) {
        val decorView = activity.window.decorView
        val viewRootImpl = View::class.java.getMethod("getViewRootImpl").invoke(decorView)!!
        val field = viewRootImpl.javaClass.getDeclaredField("mUnhandledKeyManager")
        field.isAccessible = true
        val unhandledKeyManager = field.get(viewRootImpl)!!
        val preViewDispatch = unhandledKeyManager.javaClass.getDeclaredMethod("preViewDispatch", KeyEvent::class.java)
        preViewDispatch.isAccessible = true
        preViewDispatch.invoke(unhandledKeyManager, event)
        decorView.dispatchKeyEvent(event)
        runMain()
    }

    @Test
    fun volumeUpAtMaximumIsPostedLikeIosWithANewSequenceNumberEachTime() {
        setMediaIndex(maxIndex())
        press(KeyEvent.KEYCODE_VOLUME_UP)
        press(KeyEvent.KEYCODE_VOLUME_UP)
        assertEquals(2, received.size)
        assertTrue(received.all { volume(it) == 1f && it.userInfo["Reason"] == "ExplicitVolumeChange" })
        assertEquals(sequenceNumber(received[0]) + 1, sequenceNumber(received[1]))
    }

    @Test
    fun volumeDownAtMinimumIsPostedLikeIos() {
        setMediaIndex(0)
        press(KeyEvent.KEYCODE_VOLUME_DOWN)
        assertEquals(0f, volume(received.single()))
    }

    @Test
    fun keysThatChangeTheVolumeAreLeftToTheSystemBroadcast() {
        setMediaIndex(maxIndex())
        press(KeyEvent.KEYCODE_VOLUME_DOWN)
        setMediaIndex(0)
        press(KeyEvent.KEYCODE_VOLUME_UP)
        setMediaIndex(4)
        press(KeyEvent.KEYCODE_VOLUME_UP)
        press(KeyEvent.KEYCODE_VOLUME_DOWN)
        assertTrue(received.isEmpty())
    }

    @Test
    fun repeatsReleasesAndOtherKeysAreNotPresses() {
        setMediaIndex(maxIndex())
        press(KeyEvent.KEYCODE_VOLUME_UP, repeatCount = 1)
        press(KeyEvent.KEYCODE_VOLUME_UP, action = KeyEvent.ACTION_UP)
        press(KeyEvent.KEYCODE_VOLUME_MUTE)
        press(KeyEvent.KEYCODE_CAMERA)
        assertTrue(received.isEmpty())
    }

    @Test
    fun keysDuringACallAdjustTheCallAndAreNotPosted() {
        setMediaIndex(maxIndex())
        audioManager.mode = AudioManager.MODE_IN_CALL
        press(KeyEvent.KEYCODE_VOLUME_UP)
        audioManager.mode = AudioManager.MODE_NORMAL
        assertTrue(received.isEmpty())
    }

    @Test
    fun broadcastsAfterAPressAtTheLimitStillGetNewSequenceNumbers() {
        val max = maxIndex()
        setMediaIndex(max)
        broadcast(index = max)
        press(KeyEvent.KEYCODE_VOLUME_UP)
        broadcast(index = max - 1)
        broadcast(index = max)
        broadcast(index = max)
        val numbers = received.map { sequenceNumber(it) }
        assertEquals(5, numbers.size)
        assertNotEquals(numbers[0], numbers[1])
        assertEquals(numbers[1] + 1, numbers[2])
        assertEquals(numbers[2] + 1, numbers[3])
        assertEquals(numbers[3], numbers[4])
    }
}

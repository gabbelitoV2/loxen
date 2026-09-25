package com.moblin.android.platform.mediaplayer

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.core.Notification
import com.moblin.android.platform.core.NotificationCenter
import com.moblin.android.platform.uikit.UISlider
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class MPVolumeViewSuite {
    private lateinit var audioManager: AudioManager
    private val observer = Any()
    private val received = mutableListOf<Notification>()

    @Before
    fun setUp() {
        audioManager = context().getSystemService(AudioManager::class.java)
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

    private fun slider(volumeView: MPVolumeView): UISlider {
        return assertNotNull(volumeView.subviews.firstOrNull { it is UISlider } as? UISlider)
    }

    private fun maxIndex(): Int {
        return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    }

    private fun mediaIndex(): Int {
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }

    private fun broadcast(index: Int, streamType: Int = AudioManager.STREAM_MUSIC) {
        val intent = Intent(SystemVolume.volumeChangedAction)
            .putExtra(SystemVolume.extraStreamType, streamType)
            .putExtra(SystemVolume.extraStreamValue, index)
        context().sendBroadcast(intent)
        runMain()
    }

    private fun volume(notification: Notification): Float {
        return notification.userInfo["Volume"] as Float
    }

    private fun sequenceNumber(notification: Notification): Int {
        return notification.userInfo["SequenceNumber"] as Int
    }

    @Test
    fun volumeViewHasOneSliderLikeTheUIKitView() {
        val volumeView = MPVolumeView(frame = CGRect.zero)
        assertSame(CGRect.zero, volumeView.frame)
        assertEquals(1, volumeView.subviews.count { it is UISlider })
        assertEquals(0f, slider(volumeView).minimumValue)
        assertEquals(1f, slider(volumeView).maximumValue)
    }

    @Test
    fun settingTheSliderSetsTheMediaVolumeOverTheStreamRange() {
        val slider = slider(MPVolumeView(frame = CGRect.zero))
        val max = maxIndex()
        assertTrue(max > 1)
        slider.value = 1f
        assertEquals(max, mediaIndex())
        slider.value = 0f
        assertEquals(0, mediaIndex())
        slider.value = 0.5f
        assertEquals(Math.round(max * 0.5f), mediaIndex())
        for (index in 0..max) {
            slider.value = index.toFloat() / max
            assertEquals(index, mediaIndex())
        }
    }

    @Test
    fun sliderValuesOutsideZeroToOneAreClamped() {
        val slider = slider(MPVolumeView(frame = CGRect.zero))
        slider.value = 1.7f
        assertEquals(maxIndex(), mediaIndex())
        slider.value = -0.3f
        assertEquals(0, mediaIndex())
        slider.value = 0.5f
        slider.value = Float.NaN
        assertEquals(0, mediaIndex())
    }

    @Test
    fun readingTheSliderReturnsTheCurrentMediaVolume() {
        val slider = slider(MPVolumeView(frame = CGRect.zero))
        val max = maxIndex()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 3, 0)
        assertEquals(3f / max, slider.value)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, max, 0)
        assertEquals(1f, slider.value)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        assertEquals(0f, slider.value)
    }

    @Test
    fun onlyTheMediaVolumeIsChanged() {
        val slider = slider(MPVolumeView(frame = CGRect.zero))
        audioManager.setStreamVolume(AudioManager.STREAM_RING, 2, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 4, 0)
        slider.value = 1f
        assertEquals(2, audioManager.getStreamVolume(AudioManager.STREAM_RING))
        assertEquals(4, audioManager.getStreamVolume(AudioManager.STREAM_ALARM))
    }

    @Test
    fun streamWithAHigherMinimumMapsZeroToOneOntoItsRange() {
        assertEquals(1, SystemVolume.volumeToIndex(volume = 0f, minIndex = 1, maxIndex = 25))
        assertEquals(25, SystemVolume.volumeToIndex(volume = 1f, minIndex = 1, maxIndex = 25))
        assertEquals(13, SystemVolume.volumeToIndex(volume = 0.5f, minIndex = 1, maxIndex = 25))
        assertEquals(0f, SystemVolume.indexToVolume(index = 1, minIndex = 1, maxIndex = 25))
        assertEquals(1f, SystemVolume.indexToVolume(index = 25, minIndex = 1, maxIndex = 25))
        assertEquals(0.5f, SystemVolume.indexToVolume(index = 13, minIndex = 1, maxIndex = 25))
        assertEquals(0f, SystemVolume.indexToVolume(index = 0, minIndex = 1, maxIndex = 25))
        assertEquals(1f, SystemVolume.indexToVolume(index = 30, minIndex = 1, maxIndex = 25))
        assertEquals(3, SystemVolume.volumeToIndex(volume = 0.5f, minIndex = 3, maxIndex = 3))
        assertEquals(0f, SystemVolume.indexToVolume(index = 3, minIndex = 3, maxIndex = 3))
    }

    @Test
    fun everyIndexSurvivesTheRoundTripThroughAFloatVolume() {
        for ((min, max) in listOf(0 to 15, 1 to 25, 0 to 30, 0 to 150, 2 to 7)) {
            for (index in min..max) {
                val volume = SystemVolume.indexToVolume(index = index, minIndex = min, maxIndex = max)
                assertEquals(index, SystemVolume.volumeToIndex(volume = volume, minIndex = min, maxIndex = max))
            }
        }
    }

    @Test
    fun mediaVolumeChangesArePostedAsSystemVolumeDidChange() {
        MPVolumeView(frame = CGRect.zero)
        val max = maxIndex()
        broadcast(index = 4)
        val notification = received.single()
        assertEquals("SystemVolumeDidChange", notification.name)
        assertEquals(4f / max, volume(notification))
        assertEquals("ExplicitVolumeChange", notification.userInfo["Reason"])
        assertTrue(notification.userInfo["SequenceNumber"] is Int)
    }

    @Test
    fun eachChangeGetsANewSequenceNumberAndDuplicatesShareIt() {
        MPVolumeView(frame = CGRect.zero)
        broadcast(index = 5)
        broadcast(index = 6)
        broadcast(index = 6)
        broadcast(index = 5)
        broadcast(index = 6)
        assertEquals(5, received.size)
        val numbers = received.map { sequenceNumber(it) }
        assertEquals(numbers[0] + 1, numbers[1])
        assertEquals(numbers[1], numbers[2])
        assertEquals(numbers[1] + 1, numbers[3])
        assertEquals(numbers[3] + 1, numbers[4])
    }

    @Test
    fun otherStreamsAreNotPosted() {
        MPVolumeView(frame = CGRect.zero)
        broadcast(index = 3, streamType = AudioManager.STREAM_RING)
        broadcast(index = 3, streamType = AudioManager.STREAM_VOICE_CALL)
        broadcast(index = 3, streamType = AudioManager.STREAM_NOTIFICATION)
        assertTrue(received.isEmpty())
        context().sendBroadcast(
            Intent(SystemVolume.volumeChangedAction).putExtra(SystemVolume.extraStreamType, AudioManager.STREAM_MUSIC),
        )
        runMain()
        assertTrue(received.isEmpty())
    }

    @Test
    fun severalVolumeViewsPostEachChangeOnce() {
        MPVolumeView(frame = CGRect.zero)
        MPVolumeView(frame = CGRect.zero)
        MPVolumeView(frame = CGRect.zero)
        broadcast(index = 2)
        assertEquals(1, received.size)
    }

    @Test
    fun plainSliderClampsToItsRange() {
        val slider = UISlider()
        slider.value = 0.25f
        assertEquals(0.25f, slider.value)
        slider.value = 3f
        assertEquals(1f, slider.value)
        slider.value = -3f
        assertEquals(0f, slider.value)
        slider.maximumValue = 10f
        slider.value = 3f
        assertEquals(3f, slider.value)
    }
}

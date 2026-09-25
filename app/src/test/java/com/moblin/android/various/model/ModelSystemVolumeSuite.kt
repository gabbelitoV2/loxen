package com.moblin.android.various.model

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Looper
import android.view.KeyEvent
import androidx.test.core.app.ApplicationProvider
import com.moblin.android.platform.core.Notification
import com.moblin.android.platform.core.NotificationCenter
import com.moblin.android.platform.mediaplayer.MPVolumeView
import com.moblin.android.platform.mediaplayer.SystemVolume
import com.moblin.android.platform.uikit.UISlider
import com.moblin.android.various.MainTimer
import com.moblin.android.various.settings.SettingsControllerFunction
import java.time.Duration
import kotlin.test.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ModelSystemVolumeSuite {
    private lateinit var model: Model
    private lateinit var audioManager: AudioManager
    private var selfieStickEnabled = false
    private var selfieStickFunction = SettingsControllerFunction.UNUSED
    private var sequenceNumber = 1_000_000
    private var actions = 0

    @Before
    fun setUp() {
        model = Model()
        runMain()
        audioManager = context().getSystemService(AudioManager::class.java)
        selfieStickEnabled = model.database.selfieStick.enabled.value
        selfieStickFunction = model.database.selfieStick.function.value
        model.database.selfieStick.enabled.value = true
        model.database.selfieStick.function.value = SettingsControllerFunction.GIMBAL_PRESET
        observeLikeSetup()
        setMediaIndex(5)
        armActionCounter()
    }

    @After
    fun tearDown() {
        NotificationCenter.default.removeObserver(model)
        model.isAppActive = true
        model.database.selfieStick.enabled.value = selfieStickEnabled
        model.database.selfieStick.function.value = selfieStickFunction
    }

    private fun context(): Context {
        return ApplicationProvider.getApplicationContext()
    }

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun advance(milliseconds: Long) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(milliseconds))
    }

    private fun observeLikeSetup() {
        NotificationCenter.default.addObserver(model, SystemVolume.didChangeNotification, null) {
            model.handleSystemVolumeDidChange(it)
        }
    }

    private fun maxIndex(): Int {
        return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    }

    private fun mediaIndex(): Int {
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }

    private fun setMediaIndex(index: Int) {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0)
    }

    private fun armActionCounter() {
        model.gimbalPresetLongPressTimers["s:button"] = MainTimer()
    }

    private fun countAction() {
        if (!model.gimbalPresetLongPressTimers.containsKey("s:button")) {
            actions += 1
            armActionCounter()
        }
    }

    private fun systemChangesMediaVolume(to: Int) {
        setMediaIndex(to)
        context().sendBroadcast(
            Intent(SystemVolume.volumeChangedAction)
                .putExtra(SystemVolume.extraStreamType, AudioManager.STREAM_MUSIC)
                .putExtra(SystemVolume.extraStreamValue, to),
        )
        runMain()
        countAction()
    }

    private fun resetFinishes() {
        val before = mediaIndex()
        advance(100)
        val after = mediaIndex()
        if (after != before) {
            systemChangesMediaVolume(to = after)
        }
    }

    private fun postVolume(volume: Float, sequenceNumber: Int? = null) {
        this.sequenceNumber += 1
        val number = sequenceNumber ?: this.sequenceNumber
        NotificationCenter.default.post(
            Notification(
                "SystemVolumeDidChange",
                null,
                mapOf("Volume" to volume, "Reason" to "ExplicitVolumeChange", "SequenceNumber" to number),
            ),
        )
        runMain()
        countAction()
    }

    @Test
    fun modelOwnsAVolumeViewWithASlider() {
        val volumeView: MPVolumeView = model.volumeView
        assertEquals(1, volumeView.subviews.count { it is UISlider })
    }

    @Test
    fun volumeButtonPressRunsTheSelfieStickActionAndRestoresTheVolume() {
        systemChangesMediaVolume(to = 6)
        assertEquals(0, actions)
        assertEquals(6, mediaIndex())
        systemChangesMediaVolume(to = 7)
        assertEquals(1, actions)
        assertEquals(7, mediaIndex())
        advance(50)
        assertEquals(7, mediaIndex())
        resetFinishes()
        assertEquals(6, mediaIndex())
        assertEquals(1, actions)
        systemChangesMediaVolume(to = 5)
        assertEquals(2, actions)
        resetFinishes()
        assertEquals(6, mediaIndex())
        assertEquals(2, actions)
        systemChangesMediaVolume(to = 7)
        resetFinishes()
        assertEquals(3, actions)
        assertEquals(6, mediaIndex())
    }

    @Test
    fun duplicateBroadcastsOfOneChangeRunTheActionOnce() {
        systemChangesMediaVolume(to = 6)
        systemChangesMediaVolume(to = 7)
        systemChangesMediaVolume(to = 7)
        assertEquals(1, actions)
        resetFinishes()
        assertEquals(6, mediaIndex())
        assertEquals(1, actions)
    }

    @Test
    fun disabledSelfieStickOnlyFollowsTheVolume() {
        model.database.selfieStick.enabled.value = false
        systemChangesMediaVolume(to = 6)
        systemChangesMediaVolume(to = 7)
        systemChangesMediaVolume(to = 8)
        advance(200)
        assertEquals(8, mediaIndex())
        assertEquals(0, actions)
        model.database.selfieStick.enabled.value = true
        systemChangesMediaVolume(to = 9)
        assertEquals(1, actions)
        resetFinishes()
        assertEquals(8, mediaIndex())
    }

    @Test
    fun inactiveAppOnlyFollowsTheVolume() {
        model.isAppActive = false
        systemChangesMediaVolume(to = 6)
        systemChangesMediaVolume(to = 7)
        advance(200)
        assertEquals(7, mediaIndex())
        assertEquals(0, actions)
        model.isAppActive = true
        systemChangesMediaVolume(to = 8)
        assertEquals(1, actions)
        resetFinishes()
        assertEquals(7, mediaIndex())
    }

    @Test
    fun restoringTheMaximumVolumeIsNotAnotherPress() {
        val max = maxIndex()
        setMediaIndex(max - 1)
        systemChangesMediaVolume(to = max)
        assertEquals(0, actions)
        systemChangesMediaVolume(to = max - 1)
        assertEquals(1, actions)
        resetFinishes()
        assertEquals(max, mediaIndex())
        assertEquals(1, actions)
    }

    @Test
    fun pressAtMinimumOrMaximumCountsOnlyOneSecondAfterTheVolumeWasSet() {
        postVolume(0.5f)
        postVolume(0.25f)
        assertEquals(1, actions)
        advance(100)
        assertEquals(Math.round(0.5f * maxIndex()), mediaIndex())
        postVolume(0.5f)
        assertEquals(1, actions)
        model.initialVolume = 1f
        postVolume(1f)
        assertEquals(1, actions)
        advance(850)
        postVolume(1f)
        assertEquals(1, actions)
        advance(200)
        postVolume(1f)
        assertEquals(2, actions)
        model.initialVolume = 0f
        postVolume(0f)
        assertEquals(3, actions)
    }

    private fun pressVolumeKey(keyCode: Int) {
        SystemVolume.volumeKeyPressed(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        runMain()
        countAction()
    }

    @Test
    fun volumeUpAtMaximumRunsTheSelfieStickActionLikeIos() {
        val max = maxIndex()
        setMediaIndex(max - 1)
        systemChangesMediaVolume(to = max)
        assertEquals(0, actions)
        advance(1100)
        pressVolumeKey(KeyEvent.KEYCODE_VOLUME_UP)
        assertEquals(1, actions)
        pressVolumeKey(KeyEvent.KEYCODE_VOLUME_UP)
        assertEquals(2, actions)
        assertEquals(max, mediaIndex())
        systemChangesMediaVolume(to = max - 1)
        assertEquals(3, actions)
        resetFinishes()
        assertEquals(max, mediaIndex())
        pressVolumeKey(KeyEvent.KEYCODE_VOLUME_UP)
        assertEquals(3, actions)
        advance(1100)
        pressVolumeKey(KeyEvent.KEYCODE_VOLUME_UP)
        assertEquals(4, actions)
    }

    @Test
    fun volumeDownAtMinimumRunsTheSelfieStickActionLikeIos() {
        setMediaIndex(1)
        systemChangesMediaVolume(to = 0)
        assertEquals(0, actions)
        advance(1100)
        pressVolumeKey(KeyEvent.KEYCODE_VOLUME_DOWN)
        assertEquals(1, actions)
        assertEquals(0, mediaIndex())
    }

    @Test
    fun repeatedSequenceNumberIsIgnored() {
        postVolume(0.5f, sequenceNumber = 1)
        postVolume(0.25f, sequenceNumber = 1)
        assertEquals(0, actions)
        postVolume(0.25f, sequenceNumber = 2)
        assertEquals(1, actions)
    }

    @Test
    fun volumeWithoutWholeUserInfoIsIgnored() {
        postVolume(0.5f)
        NotificationCenter.default.post(
            Notification("SystemVolumeDidChange", null, mapOf("Volume" to 0.25f, "SequenceNumber" to 5)),
        )
        runMain()
        countAction()
        assertEquals(0, actions)
        assertEquals(0.5f, model.initialVolume)
    }
}

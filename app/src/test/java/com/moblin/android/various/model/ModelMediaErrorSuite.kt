package com.moblin.android.various.model

import android.os.Looper
import com.moblin.android.platform.avfoundation.AVError
import com.moblin.android.platform.core.NotificationCenter
import com.moblin.android.various.utils.tryGetToastSubTitle
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ModelMediaErrorSuite {
    private val models = mutableListOf<Model>()

    @After
    fun tearDown() {
        for (model in models) {
            NotificationCenter.default.removeObserver(model)
        }
    }

    private fun makeModel(): Model {
        val model = Model()
        models.add(model)
        return model
    }

    private fun toast(model: Model, error: Throwable): Pair<String, String?> {
        model.mediaError(error)
        shadowOf(Looper.getMainLooper()).idle()
        val toast = model.toast.toast.value
        return toast.title to toast.subTitle
    }

    @Test
    fun theSubTitleIsTheAVErrorFailureReason() {
        assertEquals("Camera closed", tryGetToastSubTitle(AVError(AVError.unknown, "Camera closed")))
        assertNull(tryGetToastSubTitle(AVError(AVError.deviceNotConnected, null)))
        assertNull(tryGetToastSubTitle(IllegalStateException("Camera closed")))
    }

    @Test
    fun anAVErrorToastsItsDescriptionOverItsFailureReason() {
        val model = makeModel()
        assertEquals(
            "The operation could not be completed" to "Camera configuration failed",
            toast(model, AVError(AVError.unknown, "Camera configuration failed")),
        )
        assertEquals("Cannot Record" to null, toast(model, AVError(AVError.deviceNotConnected, null)))
    }

    @Test
    fun otherErrorsToastTheirMessageOnly() {
        val model = makeModel()
        assertEquals("Camera is busy" to null, toast(model, IllegalStateException("Camera is busy")))
    }
}

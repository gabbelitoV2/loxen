package com.moblin.android.platform.avfoundation

import android.content.Context
import android.hardware.camera2.CameraManager
import com.moblin.android.AppDelegate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AVErrorSuite {
    @Test
    fun theMessageIsAppleLocalizedDescriptionForTheCode() {
        val expected = mapOf(
            AVError.unknown to "The operation could not be completed",
            AVError.sessionNotRunning to "Cannot Record",
            AVError.deviceNotConnected to "Cannot Record",
            AVError.mediaServicesWereReset to "Cannot Complete Action",
            AVError.decodeFailed to "Cannot Decode",
            AVError.fileFormatNotRecognized to "Cannot Open",
            AVError.fileFailedToParse to "Cannot Open",
            AVError.operationNotSupportedForAsset to "Operation Stopped",
            AVError.invalidVideoComposition to "Operation Stopped",
            AVError.serverIncorrectlyConfigured to "Operation Stopped",
            AVError.operationInterrupted to "Operation Interrupted",
        )
        for ((code, description) in expected) {
            val error = AVError(code, "reason")
            assertEquals(description, error.localizedDescription, "$code")
            assertEquals(description, error.message, "$code")
            assertEquals(description, error.localizedMessage, "$code")
        }
    }

    @Test
    fun otherCodesGetFoundationsGenericDescription() {
        assertEquals(
            "The operation couldn’t be completed. (AVFoundationErrorDomain error 4.)",
            AVError(4, "ERROR_CAMERA_DEVICE").localizedMessage,
        )
    }

    @Test
    fun theFailureReasonIsKeptApartFromTheDescription() {
        val error = AVError(AVError.unknown, "Camera configuration failed")
        assertEquals("Camera configuration failed", error.localizedFailureReason)
        assertEquals("The operation could not be completed", error.localizedMessage)
        assertNull(AVError(AVError.deviceNotConnected, null).localizedFailureReason)
    }

    @Test
    fun anExplicitDescriptionWins() {
        val error = AVError(
            AVError.applicationIsNotAuthorizedToUseDevice,
            "This app is not authorized to use Back Camera.",
            "Cannot use Back Camera",
        )
        assertEquals("Cannot use Back Camera", error.localizedMessage)
        assertEquals("This app is not authorized to use Back Camera.", error.localizedFailureReason)
    }

    @Test
    fun theDescriptionPrintsLikeNSError() {
        assertEquals(
            "Error Domain=AVFoundationErrorDomain Code=-11800 \"The operation could not be completed\" " +
                "UserInfo={NSLocalizedFailureReason=No inputs, NSLocalizedDescription=The operation could not be completed}",
            AVError(AVError.unknown, "No inputs").toString(),
        )
        assertEquals(
            "Error Domain=AVFoundationErrorDomain Code=-11814 \"Cannot Record\" " +
                "UserInfo={NSLocalizedDescription=Cannot Record}",
            AVError(AVError.deviceNotConnected, null).toString(),
        )
    }

    @Test
    fun aDisconnectedDeviceCannotRecordWithoutAFailureReason() {
        val device = FakeCaptureDevices.make()
        val manager = AppDelegate.context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        shadowOf(manager).removeCamera(device.uniqueID)
        val error = assertFailsWith<AVError> { AVCaptureDeviceInput(device = device) }
        assertEquals(AVError.deviceNotConnected, error.code)
        assertEquals("Cannot Record", error.localizedMessage)
        assertNull(error.localizedFailureReason)
    }
}

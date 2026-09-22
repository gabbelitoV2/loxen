package com.moblin.android.videoeffects.alerts

private const val alertsEffectBackgroundFaceImageWidth = 130.0
private const val alertsEffectBackgroundFaceImageHeight = 160.0

class AlertsEffectBackgroundLandmarkRectangle(
    topLeftX: Double,
    topLeftY: Double,
    bottomRightX: Double,
    bottomRightY: Double
) {
    val topLeftX: Double = topLeftX / alertsEffectBackgroundFaceImageWidth
    val topLeftY: Double = topLeftY / alertsEffectBackgroundFaceImageHeight
    val bottomRightX: Double = bottomRightX / alertsEffectBackgroundFaceImageWidth
    val bottomRightY: Double = bottomRightY / alertsEffectBackgroundFaceImageHeight

    fun width(): Double {
        return bottomRightX - topLeftX
    }

    fun height(): Double {
        return bottomRightY - topLeftY
    }
}

val alertsEffectBackgroundLeftEyeRectangle = AlertsEffectBackgroundLandmarkRectangle(
    topLeftX = 40.0,
    topLeftY = 89.0,
    bottomRightX = 62.0,
    bottomRightY = 103.0
)

val alertsEffectBackgroundRightEyeRectangle = AlertsEffectBackgroundLandmarkRectangle(
    topLeftX = 72.0,
    topLeftY = 89.0,
    bottomRightX = 94.0,
    bottomRightY = 103.0
)

val alertsEffectBackgroundMouthRectangle = AlertsEffectBackgroundLandmarkRectangle(
    topLeftX = 50.0,
    topLeftY = 120.0,
    bottomRightX = 82.0,
    bottomRightY = 130.0
)

val alertsEffectBackgroundFaceRectangle = AlertsEffectBackgroundLandmarkRectangle(
    topLeftX = 25.0,
    topLeftY = 80.0,
    bottomRightX = 105.0,
    bottomRightY = 147.0
)

enum class AlertsEffectFaceLandmark {
    FACE,
    LEFT_EYE,
    RIGHT_EYE,
    MOUTH
}

data class AlertsEffectLandmarkSettings(
    val landmark: AlertsEffectFaceLandmark,
    val height: Double,
    val centerX: Double,
    val centerY: Double
)

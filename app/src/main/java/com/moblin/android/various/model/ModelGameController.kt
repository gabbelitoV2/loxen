package com.moblin.android.various.model

import android.view.InputDevice
import com.moblin.android.localized
import com.moblin.android.various.Gimbal
import com.moblin.android.various.MainTimer
import com.moblin.android.various.settings.SettingsControllerFunction
import com.moblin.android.various.settings.SettingsControllerFunctionData
import com.moblin.android.various.settings.SettingsControllerThumbStickFunction
import com.moblin.android.various.settings.SettingsQuickButtonType
import kotlin.math.abs

private const val thumbStickDeadZone: Float = 0.1f

fun Model.handleControllerFunction(
    buttonId: String,
    function: SettingsControllerFunction,
    functionData: SettingsControllerFunctionData,
    pressed: Boolean
) {
    when (function) {
        SettingsControllerFunction.UNUSED -> {
        }
        SettingsControllerFunction.RECORD -> {
            if (!pressed) {
                toggleRecording()
            }
        }
        SettingsControllerFunction.STREAM -> {
            if (!pressed) {
                toggleStream()
            }
        }
        SettingsControllerFunction.ZOOM_IN -> {
            handleGameControllerButtonZoom(pressed = pressed, x = Float.POSITIVE_INFINITY)
        }
        SettingsControllerFunction.ZOOM_OUT -> {
            handleGameControllerButtonZoom(pressed = pressed, x = 0f)
        }
        SettingsControllerFunction.GIMBAL_UP -> {
            setGimbalMovement(x = if (pressed) 1f else 0f, y = 0f)
        }
        SettingsControllerFunction.GIMBAL_DOWN -> {
            setGimbalMovement(x = if (pressed) -1f else 0f, y = 0f)
        }
        SettingsControllerFunction.GIMBAL_LEFT -> {
            setGimbalMovement(x = 0f, y = if (pressed) 1f else 0f)
        }
        SettingsControllerFunction.GIMBAL_RIGHT -> {
            setGimbalMovement(x = 0f, y = if (pressed) -1f else 0f)
        }
        SettingsControllerFunction.GIMBAL_PRESET -> {
            val timer = gimbalPresetLongPressTimers.remove(buttonId)
            timer?.stop()
            if (!pressed) {
                val gimbalPresetId = functionData.gimbalPresetId
                if (gimbalPresetId != null && timer != null) {
                    moveToGimbalPreset(id = gimbalPresetId)
                }
            } else {
                val gimbalPresetId = functionData.gimbalPresetId
                if (gimbalPresetId != null) {
                    val timer = MainTimer()
                    timer.startSingleShot(timeout = 0.5) {
                        gimbalPresetLongPressTimers.remove(buttonId)
                        saveGimbalPreset(id = gimbalPresetId)
                        makeToast(title = localized("Gimbal preset updated"))
                    }
                    gimbalPresetLongPressTimers[buttonId] = timer
                }
            }
        }
        SettingsControllerFunction.GIMBAL_ANIMATE -> {
            if (!pressed) {
                Gimbal.shared?.animate(motion = functionData.gimbalMotion)
            }
        }
        SettingsControllerFunction.TORCH -> {
            if (!pressed) {
                toggleTorch()
                toggleQuickButton(type = SettingsQuickButtonType.torch)
            }
        }
        SettingsControllerFunction.MUTE -> {
            if (!pressed) {
                toggleMute()
                toggleQuickButton(type = SettingsQuickButtonType.mute)
            }
        }
        SettingsControllerFunction.BLACK_SCREEN -> {
            if (!pressed) {
                toggleStealthMode()
            }
        }
        SettingsControllerFunction.SCENE -> {
            val sceneId = functionData.sceneId
            if (sceneId != null && !pressed) {
                selectScene(id = sceneId)
            }
        }
        SettingsControllerFunction.SWITCH_SCENE -> {
            if (!pressed) {
                switchToNextSceneRoundRobin()
            }
        }
        SettingsControllerFunction.WIDGET -> {
            val widgetId = functionData.widgetId
            if (widgetId != null && !pressed) {
                toggleWidgetOnOff(id = widgetId)
            }
        }
        SettingsControllerFunction.MACRO -> {
            val macroId = functionData.macroId
            if (macroId != null && !pressed) {
                toggleMacroStartStop(id = macroId)
            }
        }
        SettingsControllerFunction.STREAM_DECK_LAYOUT -> {
            if (!pressed) {
                database.streamDecks.selectedId.value = functionData.streamDeckLayoutId
                setSelectedStreamDeck()
            }
        }
        SettingsControllerFunction.INSTANT_REPLAY -> {
            if (!pressed) {
                instantReplay()
            }
        }
        SettingsControllerFunction.STOP_REPLAY -> {
            if (!pressed) {
                replay.isPlaying.value = false
                replayCancel()
            }
        }
        SettingsControllerFunction.SNAPSHOT -> {
            if (!pressed) {
                takeSnapshot()
            }
        }
        SettingsControllerFunction.PAUSE_TTS -> {
            if (!pressed) {
                toggleTextToSpeechPaused()
            }
        }
        SettingsControllerFunction.PIXELLATE -> {
            if (!pressed) {
                togglePixellateQuickButton()
            }
        }
        SettingsControllerFunction.MOVIE -> {
            if (!pressed) {
                toggleFilterQuickButton(type = SettingsQuickButtonType.movie)
            }
        }
        SettingsControllerFunction.GRAY_SCALE -> {
            if (!pressed) {
                toggleFilterQuickButton(type = SettingsQuickButtonType.grayScale)
            }
        }
        SettingsControllerFunction.SEPIA -> {
            if (!pressed) {
                toggleFilterQuickButton(type = SettingsQuickButtonType.sepia)
            }
        }
        SettingsControllerFunction.TRIPLE -> {
            if (!pressed) {
                toggleFilterQuickButton(type = SettingsQuickButtonType.triple)
            }
        }
        SettingsControllerFunction.TWIN -> {
            if (!pressed) {
                toggleFilterQuickButton(type = SettingsQuickButtonType.twin)
            }
        }
        SettingsControllerFunction.CAMERA_MAN -> {
            if (!pressed) {
                toggleCameraManQuickButton()
            }
        }
        SettingsControllerFunction.FOUR_THREE -> {
            if (!pressed) {
                toggleFilterQuickButton(type = SettingsQuickButtonType.fourThree)
            }
        }
        SettingsControllerFunction.PINCH -> {
            if (!pressed) {
                togglePinchQuickButton()
            }
        }
        SettingsControllerFunction.WHIRLPOOL -> {
            if (!pressed) {
                toggleWhirlpoolQuickButton()
            }
        }
        SettingsControllerFunction.POLL -> {
            if (!pressed) {
                togglePollQuickButton()
            }
        }
        SettingsControllerFunction.BLUR_FACES -> {
            if (!pressed) {
                toggleBlurFaces()
            }
        }
        SettingsControllerFunction.PRIVACY -> {
            if (!pressed) {
                togglePrivacy()
            }
        }
        SettingsControllerFunction.BEAUTY -> {
            if (!pressed) {
                toggleBeautyQuickButton()
            }
        }
        SettingsControllerFunction.GIMBAL_TRACKING -> {
            if (!pressed) {
                toggleGimbalTracking()
            }
        }
        else -> {
        }
    }
}

fun Model.isGameControllerConnected(): Boolean {
    return numberOfGameControllers() > 0
}

private fun Model.handleGameControllerButtonZoom(pressed: Boolean, x: Float) {
    if (pressed) {
        setZoomX(x = x, rate = database.zoom.speed)
    } else {
        stopCameraZoom()?.let { x ->
            setZoomXWhenInRange(x = x)
        }
    }
}

fun Model.handleGameControllerThumbStick(
    function: SettingsControllerThumbStickFunction,
    xValue: Float,
    yValue: Float
) {
    if (function == SettingsControllerThumbStickFunction.UNUSED) {
        return
    }
    when (function) {
        SettingsControllerThumbStickFunction.UNUSED -> {
        }
        SettingsControllerThumbStickFunction.GIMBAL_PAN_TILT -> {
            val x = if (abs(xValue) > thumbStickDeadZone) xValue else 0f
            val y = if (abs(yValue) > thumbStickDeadZone) yValue else 0f
            setGimbalMovement(x = y, y = -x)
        }
        else -> {
        }
    }
}

private fun Model.getGameControllerIndex(gameController: InputDevice): Int? {
    val index = gameControllers.indexOf(gameController)
    if (index < 0) {
        return null
    }
    if (index >= database.gameControllers.size) {
        return null
    }
    return index
}

fun Model.handleGameControllerButton(
    gameController: InputDevice,
    buttonId: String,
    value: Float,
    pressed: Boolean
) {
    val index = getGameControllerIndex(gameController) ?: return
    val button = database.gameControllers[index].buttons.value.firstOrNull { it.name == buttonId } ?: return
    handleControllerFunction(
        buttonId = "gc:$index:$buttonId",
        function = button.function.value,
        functionData = button.functionData.value,
        pressed = pressed
    )
}

private fun Model.numberOfGameControllers(): Int {
    return gameControllers.count { it != null }
}

private fun Model.updateGameControllers() {
    statusTopRight.gameControllersTotal.value = numberOfGameControllers().toString()
}

private fun Model.gameControllerNumber(gameController: InputDevice): Int? {
    val gameControllerIndex = gameControllers.indexOf(gameController)
    if (gameControllerIndex >= 0) {
        return gameControllerIndex + 1
    }
    return null
}

fun Model.handleGameControllerDidConnect(device: InputDevice) {
    val emptyIndex = gameControllers.indexOf(null)
    if (emptyIndex >= 0) {
        gameControllers[emptyIndex] = device
    } else {
        gameControllers.add(device)
    }
    val number = gameControllerNumber(device)
    if (number != null) {
        makeToast(title = localized("Game controller $number connected"))
    }
    updateGameControllers()
}

fun Model.handleGameControllerDidDisconnect(device: InputDevice) {
    val number = gameControllerNumber(device)
    if (number != null) {
        makeToast(title = localized("Game controller $number disconnected"))
    }
    val index = gameControllers.indexOf(device)
    if (index >= 0) {
        gameControllers[index] = null
    }
    updateGameControllers()
}

fun Model.isShowingStatusGameController(): Boolean {
    return database.show.gameController && isGameControllerConnected()
}

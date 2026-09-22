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
        SettingsControllerFunction.unused -> {
        }
        SettingsControllerFunction.record -> {
            if (!pressed) {
                toggleRecording()
            }
        }
        SettingsControllerFunction.stream -> {
            if (!pressed) {
                toggleStream()
            }
        }
        SettingsControllerFunction.zoomIn -> {
            handleGameControllerButtonZoom(pressed = pressed, x = Float.POSITIVE_INFINITY)
        }
        SettingsControllerFunction.zoomOut -> {
            handleGameControllerButtonZoom(pressed = pressed, x = 0f)
        }
        SettingsControllerFunction.gimbalUp -> {
            setGimbalMovement(x = if (pressed) 1f else 0f, y = 0f)
        }
        SettingsControllerFunction.gimbalDown -> {
            setGimbalMovement(x = if (pressed) -1f else 0f, y = 0f)
        }
        SettingsControllerFunction.gimbalLeft -> {
            setGimbalMovement(x = 0f, y = if (pressed) 1f else 0f)
        }
        SettingsControllerFunction.gimbalRight -> {
            setGimbalMovement(x = 0f, y = if (pressed) -1f else 0f)
        }
        SettingsControllerFunction.gimbalPreset -> {
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
        SettingsControllerFunction.gimbalAnimate -> {
            if (!pressed) {
                Gimbal.shared?.animate(motion = functionData.gimbalMotion)
            }
        }
        SettingsControllerFunction.torch -> {
            if (!pressed) {
                toggleTorch()
                toggleQuickButton(type = SettingsQuickButtonType.torch)
            }
        }
        SettingsControllerFunction.mute -> {
            if (!pressed) {
                toggleMute()
                toggleQuickButton(type = SettingsQuickButtonType.mute)
            }
        }
        SettingsControllerFunction.blackScreen -> {
            if (!pressed) {
                toggleStealthMode()
            }
        }
        SettingsControllerFunction.scene -> {
            val sceneId = functionData.sceneId
            if (sceneId != null && !pressed) {
                selectScene(id = sceneId)
            }
        }
        SettingsControllerFunction.switchScene -> {
            if (!pressed) {
                switchToNextSceneRoundRobin()
            }
        }
        SettingsControllerFunction.widget -> {
            val widgetId = functionData.widgetId
            if (widgetId != null && !pressed) {
                toggleWidgetOnOff(id = widgetId)
            }
        }
        SettingsControllerFunction.macro -> {
            val macroId = functionData.macroId
            if (macroId != null && !pressed) {
                toggleMacroStartStop(id = macroId)
            }
        }
        SettingsControllerFunction.streamDeckLayout -> {
            if (!pressed) {
                database.streamDecks.selectedId = functionData.streamDeckLayoutId
                setSelectedStreamDeck()
            }
        }
        SettingsControllerFunction.instantReplay -> {
            if (!pressed) {
                instantReplay()
            }
        }
        SettingsControllerFunction.stopReplay -> {
            if (!pressed) {
                replay.isPlaying = false
                replayCancel()
            }
        }
        SettingsControllerFunction.snapshot -> {
            if (!pressed) {
                takeSnapshot()
            }
        }
        SettingsControllerFunction.pauseTts -> {
            if (!pressed) {
                toggleTextToSpeechPaused()
            }
        }
        SettingsControllerFunction.pixellate -> {
            if (!pressed) {
                togglePixellateQuickButton()
            }
        }
        SettingsControllerFunction.movie -> {
            if (!pressed) {
                toggleFilterQuickButton(type = SettingsQuickButtonType.movie)
            }
        }
        SettingsControllerFunction.grayScale -> {
            if (!pressed) {
                toggleFilterQuickButton(type = SettingsQuickButtonType.grayScale)
            }
        }
        SettingsControllerFunction.sepia -> {
            if (!pressed) {
                toggleFilterQuickButton(type = SettingsQuickButtonType.sepia)
            }
        }
        SettingsControllerFunction.triple -> {
            if (!pressed) {
                toggleFilterQuickButton(type = SettingsQuickButtonType.triple)
            }
        }
        SettingsControllerFunction.twin -> {
            if (!pressed) {
                toggleFilterQuickButton(type = SettingsQuickButtonType.twin)
            }
        }
        SettingsControllerFunction.cameraMan -> {
            if (!pressed) {
                toggleCameraManQuickButton()
            }
        }
        SettingsControllerFunction.fourThree -> {
            if (!pressed) {
                toggleFilterQuickButton(type = SettingsQuickButtonType.fourThree)
            }
        }
        SettingsControllerFunction.pinch -> {
            if (!pressed) {
                togglePinchQuickButton()
            }
        }
        SettingsControllerFunction.whirlpool -> {
            if (!pressed) {
                toggleWhirlpoolQuickButton()
            }
        }
        SettingsControllerFunction.poll -> {
            if (!pressed) {
                togglePollQuickButton()
            }
        }
        SettingsControllerFunction.blurFaces -> {
            if (!pressed) {
                toggleBlurFaces()
            }
        }
        SettingsControllerFunction.privacy -> {
            if (!pressed) {
                togglePrivacy()
            }
        }
        SettingsControllerFunction.beauty -> {
            if (!pressed) {
                toggleBeautyQuickButton()
            }
        }
        SettingsControllerFunction.gimbalTracking -> {
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
    if (function == SettingsControllerThumbStickFunction.unused) {
        return
    }
    when (function) {
        SettingsControllerThumbStickFunction.unused -> {
        }
        SettingsControllerThumbStickFunction.gimbalPanTilt -> {
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
    val button = database.gameControllers[index].buttons.firstOrNull { it.name == buttonId } ?: return
    handleControllerFunction(
        buttonId = "gc:$index:$buttonId",
        function = button.function,
        functionData = button.functionData,
        pressed = pressed
    )
}

private fun Model.numberOfGameControllers(): Int {
    return gameControllers.count { it != null }
}

private fun Model.updateGameControllers() {
    statusTopRight.gameControllersTotal = numberOfGameControllers().toString()
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

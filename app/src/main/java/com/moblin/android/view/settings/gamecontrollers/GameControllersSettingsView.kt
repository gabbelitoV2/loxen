package com.moblin.android.view.settings.gamecontrollers

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsGameController
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import kotlinx.coroutines.withTimeoutOrNull

private fun gameControllerIndex(database: Database, gameController: SettingsGameController): Int {
    val index = database.gameControllers.indexOfFirst { gameController2 ->
        gameController.id == gameController2.id
    }
    return if (index >= 0) index + 1 else 1
}

@Composable
fun GameControllersSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val gameControllers = database.gameControllers
    Form(title = "Game controllers") {
        Section {
            Text(localized("Use game controllers to zoom, set scene, and more, from a distance."))
        }
        Section(
            footerContent = {
                SwipeLeftToDeleteHelpView(kind = localized("a controller"))
            },
        ) {
            for (gameController in gameControllers) {
                key(gameController.id) {
                    NavigationLink(
                        destination = {
                            GameControllersControllerSettingsView(
                                model = model,
                                gameController = gameController,
                            )
                        },
                    ) {
                        Text(
                            text = "Controller ${gameControllerIndex(database, gameController)}",
                            modifier = Modifier.pointerInput(gameController.id) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    val up = withTimeoutOrNull(
                                        viewConfiguration.longPressTimeoutMillis,
                                    ) {
                                        waitForUpOrCancellation()
                                    }
                                    if (up == null) {
                                        database.gameControllers = database.gameControllers
                                            .filterNot { it.id == gameController.id }
                                            .toMutableList()
                                        waitForUpOrCancellation()?.consume()
                                    }
                                }
                            },
                        )
                    }
                }
            }
            CreateButtonView {
                database.gameControllers =
                    (database.gameControllers + SettingsGameController()).toMutableList()
            }
        }
    }
}

package com.moblin.android.view.settings.gamecontrollers

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsGameController
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

private fun gameControllerIndex(database: Database, gameController: SettingsGameController): Int {
    val index = database.gameControllers.value.indexOfFirst { gameController2 ->
        gameController.id == gameController2.id
    }
    return if (index >= 0) index + 1 else 1
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GameControllersSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val gameControllers by database.gameControllers.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Game controllers") })
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            item {
                Text(
                    text = "Use game controllers to zoom, set scene, and more, from a distance.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            item {
                HorizontalDivider()
            }
            items(gameControllers) { gameController ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                onNavigate("GameControllersControllerSettingsView/${gameController.id}")
                            },
                            onLongClick = {
                                database.gameControllers.value =
                                    database.gameControllers.value.filterNot { it.id == gameController.id }
                            },
                        )
                        .padding(16.dp),
                ) {
                    Text("Controller ${gameControllerIndex(database, gameController)}")
                }
                HorizontalDivider()
            }
            item {
                CreateButtonView {
                    database.gameControllers.value =
                        database.gameControllers.value + SettingsGameController()
                }
            }
            item {
                SwipeLeftToDeleteHelpView(kind = localized("a controller"))
            }
        }
    }
}

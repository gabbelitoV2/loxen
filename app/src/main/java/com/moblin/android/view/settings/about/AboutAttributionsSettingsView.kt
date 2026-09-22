package com.moblin.android.view.settings.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalOnNavigate

private data class Attribution(
    val name: String,
    val text: List<String>,
)

private val soundAttributions: List<Attribution> = listOf(
    Attribution(
        name = "Bad chili fart",
        text = listOf(
            "Bad Chili Fart.wav by deleted_user_1391979",
            "-- https://freesound.org/s/94989/",
            "-- License: Creative Commons 0",
        ),
    ),
    Attribution(
        name = "Boing",
        text = listOf(
            "Boing.wav by juskiddink",
            "-- https://freesound.org/s/140867/",
            "-- License: Attribution 4.0",
        ),
    ),
    Attribution(
        name = "Cash register",
        text = listOf(
            "Cash Register by kiddpark",
            "-- https://freesound.org/s/201159/",
            "-- License: Attribution 4.0",
        ),
    ),
    Attribution(
        name = "Coin dropping",
        text = listOf(
            "Coin dropping.wav by Jace",
            "-- https://freesound.org/s/17502/",
            "-- License: Creative Commons 0",
        ),
    ),
    Attribution(
        name = "Dingaling",
        text = listOf(
            "dingaling by morrisjm",
            "-- https://freesound.org/s/268756/",
            "-- License: Attribution 4.0",
        ),
    ),
    Attribution(
        name = "Fart",
        text = listOf(
            "FART.aif by Manicciola",
            "-- https://freesound.org/s/121783/",
            "-- License: Creative Commons 0",
        ),
    ),
    Attribution(
        name = "Fart 2",
        text = listOf(
            "Fart sound.wav by aditwayer",
            "-- https://freesound.org/s/520671/",
            "-- License: Creative Commons 0",
        ),
    ),
    Attribution(
        name = "Level up",
        text = listOf(
            "320655__rhodesmas__level-up-01.mp3 by shinephoenixstormcrow",
            "-- https://freesound.org/s/337049/",
            "-- License: Attribution 3.0",
        ),
    ),
    Attribution(
        name = "Notification",
        text = listOf(
            "Message Notification 4 by AnthonyRox",
            "-- https://freesound.org/s/740423/",
            "-- License: Creative Commons 0",
        ),
    ),
    Attribution(
        name = "Notification 2",
        text = listOf(
            "notification2-freesound.wav by Thoribass",
            "-- https://freesound.org/s/254819/",
            "-- License: Attribution 4.0",
        ),
    ),
    Attribution(
        name = "Nya",
        text = listOf(
            "Nya.wav by Mike_bes",
            "-- https://freesound.org/s/336012/",
            "-- License: Creative Commons 0",
        ),
    ),
    Attribution(
        name = "Perfect fart",
        text = listOf(
            "perfect-fart.mp3 by TV_LING",
            "-- https://freesound.org/s/523467/",
            "-- License: Creative Commons 0",
        ),
    ),
    Attribution(
        name = "SFX magic",
        text = listOf(
            "SFX Magic by renatalmar",
            "-- https://freesound.org/s/264981/",
            "-- License: Creative Commons 0",
        ),
    ),
    Attribution(
        name = "Silence",
        text = listOf(
            "C0000_silence5sec.mp3 by thanvannispen",
            "-- https://freesound.org/s/107061/",
            "-- License: Attribution 4.0",
        ),
    ),
    Attribution(
        name = "Whoosh",
        text = listOf(
            "Whoosh by qubodup",
            "-- https://freesound.org/s/60013/",
            "-- License: Creative Commons 0",
        ),
    ),
)

private val imageAttributions: List<Attribution> = listOf(
    Attribution(
        name = "-100",
        text = listOf(
            "Credit Richie Velasquez ",
            "https://www.deladeso.com/",
        ),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAttributionsSoundsSettingsView() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Sounds") })
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(16.dp),
        ) {
            items(soundAttributions, key = { it.name }) { attribution ->
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Text(
                        text = attribution.name,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Column(modifier = Modifier.padding(start = 5.dp, top = 5.dp)) {
                        attribution.text.forEach { line ->
                            Text(text = line)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAttributionsImagesSettingsView() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Images") })
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(16.dp),
        ) {
            items(imageAttributions, key = { it.name }) { attribution ->
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Text(
                        text = attribution.name,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Column(modifier = Modifier.padding(start = 5.dp, top = 5.dp)) {
                        attribution.text.forEach { line ->
                            Text(text = line)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAttributionsSettingsView(onNavigate: (String) -> Unit = LocalOnNavigate.current) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Attributions") })
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            item {
                TextButton(onClick = { onNavigate("AboutAttributionsSoundsSettingsView") }) {
                    Text("Sounds")
                }
            }
            item {
                TextButton(onClick = { onNavigate("AboutAttributionsImagesSettingsView") }) {
                    Text("Images")
                }
            }
        }
    }
}

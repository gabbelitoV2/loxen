package com.moblin.android.view.controlbar.quickbutton

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.platform.swiftui.NavigationTitle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsAlertsMediaGallery
import com.moblin.android.various.settings.SettingsAlertsMediaGalleryItem
import com.moblin.android.LocalModel
import com.moblin.android.various.model.playSoundBoardSound

@Composable
private fun SoundView(model: Model = LocalModel.current, sound: SettingsAlertsMediaGalleryItem) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(formPalette().cell)
            .border(1.dp, formPalette().separator, RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = null) {
                model.playSoundBoardSound(soundId = sound.id)
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = sound.name,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (pressed) 0.2f else 1f),
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
fun QuickButtonSoundBoardView(model: Model = LocalModel.current, gallery: SettingsAlertsMediaGallery) {
    NavigationTitle("Sound board")
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = gallery.allSounds().filter { it.name != "Silence" },
            key = { it.id.toString() },
        ) { sound ->
            SoundView(model = model, sound = sound)
        }
    }
}

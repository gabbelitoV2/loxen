package com.moblin.android.view.settings.store

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.common.various.controlBarButtonSize
import com.moblin.android.platform.Bundle
import com.moblin.android.platform.swiftui.Alert
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.Store
import com.moblin.android.view.utils.TextButtonView
import kotlinx.coroutines.launch

private const val TAG = "StoreSettingsIconsToBuy"

@Composable
private fun StoreSettingsRestoreView(model: Model = LocalModel.current) {
    var isRestoring by remember { mutableStateOf(false) }
    var showErrorAlert by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Section {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.alpha(if (isRestoring) 0.0f else 1.0f)) {
                TextButtonView(
                    title = "Restore purchases",
                    action = {
                        isRestoring = true
                        scope.launch {
                            runCatching {
                                Unit
                            }.onFailure {
                                showErrorAlert = true
                            }
                            isRestoring = false
                        }
                    }
                )
            }
            if (isRestoring) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = formPalette().accent,
                    strokeWidth = 2.dp
                )
            }
        }
    }
    Alert(
        title = "Restore purchases failed",
        isPresented = showErrorAlert,
        onDismissRequest = { showErrorAlert = false },
    ) {
        Button("Ok")
    }
}

@Composable
private fun StoreSettingsBoughtEverythingView() {
    Section(
        header = "Icons to buy",
        footer = "Many thanks from the Moblin developers!"
    ) {
        Text("You already bought everything! ❤️")
    }
}

@Composable
private fun StoreSettingsIconsToBuyView(model: Model = LocalModel.current, store: Store) {
    val iconsInStore by store.iconsInStore.collectAsState()
    var disabledPurchaseButtons by remember { mutableStateOf(setOf<String>()) }
    val scope = rememberCoroutineScope()
    Section(header = "Icons to buy") {
        iconsInStore.forEach { icon ->
            key(icon.id) {
                val isPurchasing = disabledPurchaseButtons.contains(icon.id)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("")
                    Bundle.image(icon.imageNoBackground())?.asImageBitmap()?.let { bitmap ->
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(controlBarButtonSize.dp)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(icon.name)
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .padding(start = 10.dp)
                                .alpha(if (isPurchasing) 0.0f else 1.0f)
                        ) {
                            TextButtonView(
                                title = icon.price,
                                action = {
                                    if (!disabledPurchaseButtons.contains(icon.id)) {
                                        disabledPurchaseButtons =
                                            disabledPurchaseButtons + icon.id
                                        scope.launch {
                                            runCatching {
                                                Unit
                                            }.onFailure { error ->
                                                Log.i(
                                                    TAG,
                                                    "store: Purchase failed with error $error"
                                                )
                                            }
                                            disabledPurchaseButtons =
                                                disabledPurchaseButtons - icon.id
                                        }
                                    }
                                }
                            )
                        }
                        if (isPurchasing) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(start = 10.dp)
                                    .size(20.dp),
                                color = formPalette().accent,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun setAppIcon(iconImage: String) {
    val name = if (iconImage == "AppIcon") null else iconImage
    Unit
}

@Composable
private fun StoreSettingsMyIconsView(model: Model = LocalModel.current, store: Store) {
    val myIcons by store.myIcons.collectAsState()
    val iconImage by store.iconImage.collectAsState()
    var previousIconImage by remember { mutableStateOf<String?>(null) }
    Section(
        header = "My icons",
        footer = "Displayed in main view and as app icon."
    ) {
        Picker(
            title = "",
            selection = iconImage,
            options = myIcons.map { it.image() },
            text = { image -> myIcons.firstOrNull { it.image() == image }?.name ?: image },
            onChange = { image -> store.iconImage.value = image }
        )
    }
    LaunchedEffect(iconImage) {
        val previous = previousIconImage
        if (previous != null && previous != iconImage) {
            model.database.iconImage = iconImage
            model.updateFaceFilterSettings()
            setAppIcon(iconImage)
        }
        previousIconImage = iconImage
    }
}

@Composable
fun StoreSettingsView(model: Model = LocalModel.current, store: Store) {
    val iconsInStore by store.iconsInStore.collectAsState()
    var disabledPurchaseButtons by remember { mutableStateOf(setOf<String>()) }
    Form(title = "Store") {
        Section {
            Text("Support Moblin developers by buying icons. ❤️")
        }
        if (iconsInStore.isNotEmpty()) {
            StoreSettingsIconsToBuyView(model = model, store = store)
        } else {
            StoreSettingsBoughtEverythingView()
        }
        StoreSettingsMyIconsView(model = model, store = store)
        StoreSettingsRestoreView(model = model)
    }
}

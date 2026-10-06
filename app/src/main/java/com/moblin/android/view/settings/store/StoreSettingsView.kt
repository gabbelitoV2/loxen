package com.moblin.android.view.settings.store

import com.moblin.android.platform.log.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.common.various.controlBarButtonSize
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Alert
import com.moblin.android.platform.swiftui.AssetImage
import com.moblin.android.platform.swiftui.Button
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.PickerStyle
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.platform.uikit.UIApplication
import com.moblin.android.platform.uikit.setAlternateIconName
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.Store
import com.moblin.android.various.model.purchaseProduct
import com.moblin.android.various.model.restorePurchases
import com.moblin.android.view.utils.TextButtonView
import kotlinx.coroutines.launch

@Composable
private fun StoreSettingsRestoreView(model: Model = LocalModel.current) {
    var isRestoring by remember { mutableStateOf(false) }
    var showErrorAlert by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Section {
        Box(contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.alpha(if (isRestoring) 0f else 1f)) {
                TextButtonView(title = "Restore purchases") {
                    if (!isRestoring) {
                        isRestoring = true
                        scope.launch {
                            try {
                                model.restorePurchases()
                            } catch (_: Throwable) {
                                showErrorAlert = true
                            }
                            isRestoring = false
                        }
                    }
                }
            }
            if (isRestoring) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = formPalette().secondaryLabel,
                    strokeWidth = 2.dp,
                )
            }
        }
        Alert(
            title = "Restore purchases failed",
            isPresented = showErrorAlert,
            onDismissRequest = { showErrorAlert = false },
        ) {
            Button("Ok") {}
        }
    }
}

@Composable
private fun StoreSettingsBoughtEverythingView() {
    Section(
        header = "Icons to buy",
        footer = "Many thanks from the Moblin developers!",
    ) {
        Text(localized("You already bought everything! ❤️"))
    }
}

@Composable
private fun StoreSettingsIconsToBuyView(store: Store, model: Model = LocalModel.current) {
    val iconsInStore by store.iconsInStore.collectAsState()
    val disabledPurchaseButtons = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()
    val palette = formPalette()
    Section(header = "Icons to buy") {
        iconsInStore.forEach { icon ->
            key(icon.id) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("")
                    AssetImage(
                        name = icon.imageNoBackground(),
                        modifier = Modifier.size(controlBarButtonSize.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(icon.name)
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .padding(start = 10.dp)
                                .alpha(if (disabledPurchaseButtons.contains(icon.id)) 0f else 1f),
                        ) {
                            Button(
                                title = icon.price,
                                enabled = !disabledPurchaseButtons.contains(icon.id),
                                action = {
                                    disabledPurchaseButtons.add(icon.id)
                                    scope.launch {
                                        try {
                                            model.purchaseProduct(id = icon.id)
                                        } catch (error: Throwable) {
                                            Log.i(
                                                "StoreSettingsIconsToBuyView",
                                                "store: Purchase failed with error $error",
                                            )
                                        }
                                        disabledPurchaseButtons.remove(icon.id)
                                    }
                                },
                            )
                        }
                        if (disabledPurchaseButtons.contains(icon.id)) {
                            Box(modifier = Modifier.padding(start = 10.dp)) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = palette.secondaryLabel,
                                    strokeWidth = 2.dp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun setAppIcon(iconImage: String) {
    val alternateIconName: String? = if (iconImage == "AppIcon") null else iconImage
    UIApplication.shared.setAlternateIconName(alternateIconName) { error ->
        if (error != null) {
            Log.d("StoreSettingsMyIconsView", "Failed to change app icon with error $error")
        }
    }
}

@Composable
private fun StoreSettingsMyIconsView(store: Store, model: Model = LocalModel.current) {
    val myIcons by store.myIcons.collectAsState()
    val iconImage by store.iconImage.collectAsState()
    Section(
        header = "My icons",
        footer = "Displayed in main view and as app icon.",
    ) {
        Picker(
            title = "",
            selection = iconImage,
            options = myIcons.map { it.image() },
            text = { image -> myIcons.firstOrNull { it.image() == image }?.name ?: image },
            pickerStyle = PickerStyle.inline,
        ) { image ->
            store.iconImage.value = image
            model.database.iconImage = image
            model.updateFaceFilterSettings()
            setAppIcon(iconImage = image)
        }
    }
}

@Composable
fun StoreSettingsView(model: Model = LocalModel.current, store: Store) {
    if (com.moblin.android.platform.loxen.Loxen.hidesStore) return com.moblin.android.platform.loxen.LoxenSettingsView()
    val disabledPurchaseButtons = remember { mutableStateListOf<String>() }
    val iconsInStore by store.iconsInStore.collectAsState()
    Form(title = "Store") {
        Section {
            Text(localized("Support Moblin developers by buying icons. ❤️"))
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

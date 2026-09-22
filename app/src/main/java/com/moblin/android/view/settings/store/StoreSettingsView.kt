package com.moblin.android.view.settings.store

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.controlBarButtonSize
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.Store
import com.moblin.android.view.utils.TextButtonView
import kotlinx.coroutines.launch
import com.moblin.android.LocalModel

private const val TAG = "StoreSettingsIconsToBuy"

@Composable
private fun StoreSettingsRestoreView(model: Model = LocalModel.current) {
    var isRestoring by remember { mutableStateOf(false) }
    var showErrorAlert by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column {
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
                                TODO("no Android counterpart for StoreKit restorePurchases")
                            }.onFailure {
                                showErrorAlert = true
                            }
                            isRestoring = false
                        }
                    }
                )
            }
            if (isRestoring) {
                CircularProgressIndicator()
            }
        }
        if (showErrorAlert) {
            AlertDialog(
                onDismissRequest = { showErrorAlert = false },
                title = { Text("Restore purchases failed") },
                confirmButton = {
                    TextButton(onClick = { showErrorAlert = false }) {
                        Text("Ok")
                    }
                }
            )
        }
    }
}

@Composable
private fun StoreSettingsBoughtEverythingView() {
    Column {
        Text("Icons to buy", style = MaterialTheme.typography.titleSmall)
        Text("You already bought everything! ❤️")
        Text(
            "Many thanks from the Moblin developers!",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun StoreSettingsIconsToBuyView(model: Model = LocalModel.current, store: Store) {
    val iconsInStore by store.iconsInStore.collectAsState()
    var disabledPurchaseButtons by remember { mutableStateOf(setOf<String>()) }
    val scope = rememberCoroutineScope()
    Column {
        Text("Icons to buy", style = MaterialTheme.typography.titleSmall)
        iconsInStore.forEach { icon ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("")
                Image(
                    painter = TODO("no Android counterpart for Image(icon.imageNoBackground())"),
                    contentDescription = null,
                    modifier = Modifier
                        .size(controlBarButtonSize.dp)
                        .clipToBounds(),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.weight(1f))
                Text(icon.name)
                Box(contentAlignment = Alignment.Center) {
                    Button(
                        onClick = {
                            disabledPurchaseButtons = disabledPurchaseButtons + icon.id
                            scope.launch {
                                runCatching {
                                    TODO("no Android counterpart for StoreKit purchaseProduct")
                                }.onFailure { error ->
                                    Log.i(
                                        TAG,
                                        "store: Purchase failed with error $error"
                                    )
                                }
                                disabledPurchaseButtons = disabledPurchaseButtons - icon.id
                            }
                        },
                        enabled = !disabledPurchaseButtons.contains(icon.id),
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .alpha(if (disabledPurchaseButtons.contains(icon.id)) 0.0f else 1.0f)
                    ) {
                        Text(icon.price)
                    }
                    if (disabledPurchaseButtons.contains(icon.id)) {
                        CircularProgressIndicator(modifier = Modifier.padding(start = 10.dp))
                    }
                }
            }
        }
    }
}

private fun setAppIcon(iconImage: String) {
    val name = if (iconImage == "AppIcon") null else iconImage
    TODO("no Android counterpart for UIApplication.shared.setAlternateIconName($name)")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoreSettingsMyIconsView(model: Model = LocalModel.current, store: Store) {
    val myIcons by store.myIcons.collectAsState()
    val iconImage by store.iconImage.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text("My icons", style = MaterialTheme.typography.titleSmall)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = myIcons.firstOrNull { it.image() == iconImage }?.name ?: iconImage,
                onValueChange = {},
                readOnly = true,
                label = { Text("") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                myIcons.forEach { icon ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("")
                                Image(
                                    painter = TODO("no Android counterpart for Image(icon.imageNoBackground())"),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(controlBarButtonSize.dp)
                                        .clipToBounds(),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(Modifier.weight(1f))
                                Text(icon.name)
                            }
                        },
                        onClick = {
                            expanded = false
                            store.iconImage.value = icon.image()
                        }
                    )
                }
            }
        }
        Text(
            "Displayed in main view and as app icon.",
            style = MaterialTheme.typography.bodySmall
        )
    }
    LaunchedEffect(iconImage) {
        model.database.iconImage = iconImage
        model.updateFaceFilterSettings()
        setAppIcon(iconImage)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreSettingsView(model: Model = LocalModel.current, store: Store) {
    val iconsInStore by store.iconsInStore.collectAsState()
    var disabledPurchaseButtons by remember { mutableStateOf(setOf<String>()) }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Store") })
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            item {
                Text("Support Moblin developers by buying icons. ❤️")
            }
            item {
                if (iconsInStore.isNotEmpty()) {
                    StoreSettingsIconsToBuyView(model = model, store = store)
                } else {
                    StoreSettingsBoughtEverythingView()
                }
            }
            item {
                StoreSettingsMyIconsView(model = model, store = store)
            }
            item {
                StoreSettingsRestoreView(model = model)
            }
        }
    }
}

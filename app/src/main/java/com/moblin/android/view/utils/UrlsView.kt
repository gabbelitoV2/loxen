package com.moblin.android.view.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.moblin.android.common.various.personalHotspotLocalAddress
import com.moblin.android.common.various.urlImage
import com.moblin.android.localized
import com.moblin.android.various.model.StatusOther
import com.moblin.android.various.utils.generateQrCode

private fun urlIconImageVector(name: String): ImageVector = when (name) {
    "wifi" -> Icons.Default.Wifi
    "personalhotspot" -> Icons.Default.WifiTethering
    "antenna.radiowaves.left.and.right" -> Icons.Default.SignalCellularAlt
    "qrcode" -> Icons.Default.QrCode
    else -> Icons.Default.Link
}

@Composable
fun UrlCopyView(url: String, image: String? = null) {
    val presentingQrCode = remember { mutableStateOf(false) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (image != null) {
                Icon(imageVector = urlIconImageVector(image), contentDescription = null)
                Spacer(modifier = Modifier.size(4.dp))
            }
            Text(text = url)
            Spacer(modifier = Modifier.weight(1f))
            Column(
                verticalArrangement = Arrangement.spacedBy(11.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CopyToClipboardButtonView(text = url)
                IconButton(onClick = { presentingQrCode.value = true }) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        if (presentingQrCode.value) {
            val qrCode = remember(url) { generateQrCode(url) }
            Dialog(
                onDismissRequest = { presentingQrCode.value = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .clickable { presentingQrCode.value = false },
                    contentAlignment = Alignment.Center,
                ) {
                    HCenter {
                        if (qrCode != null) {
                            Image(
                                bitmap = qrCode.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentScale = ContentScale.Fit,
                                filterQuality = FilterQuality.None,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UrlsIpv4View(
    status: StatusOther,
    formatUrl: (String) -> String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = localized("IPv4"), style = MaterialTheme.typography.titleMedium)
        val ipStatuses = status.ipStatuses.collectAsState().value
        ipStatuses.filter { it.ipType.rawValue == "ipv4" }.forEach { ipStatus ->
            UrlCopyView(
                url = formatUrl(ipStatus.ipType.formatAddress(ipStatus.ip)),
                image = urlImage(interfaceType = ipStatus.interfaceType),
            )
        }
        UrlCopyView(url = formatUrl(personalHotspotLocalAddress), image = "personalhotspot")
    }
}

@Composable
fun UrlsIpv6View(
    status: StatusOther,
    formatUrl: (String) -> String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = localized("IPv6"), style = MaterialTheme.typography.titleMedium)
        val ipStatuses = status.ipStatuses.collectAsState().value
        ipStatuses.filter { it.ipType.rawValue == "ipv6" }.forEach { ipStatus ->
            UrlCopyView(
                url = formatUrl(ipStatus.ipType.formatAddress(ipStatus.ip)),
                image = urlImage(interfaceType = ipStatus.interfaceType),
            )
        }
    }
}

@Composable
fun UrlsView(
    status: StatusOther,
    title: String = localized("URLs"),
    showIPv6: Boolean = true,
    formatUrl: (String) -> String,
    onNavigate: (String) -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate(title) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title)
        Spacer(modifier = Modifier.weight(1f))
        Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrlsViewDestination(
    status: StatusOther,
    formatUrl: (String) -> String,
    title: String = localized("URLs"),
    showIPv6: Boolean = true,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(text = title) }) },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            item {
                UrlsIpv4View(status = status, formatUrl = formatUrl)
            }
            if (showIPv6) {
                item {
                    UrlsIpv6View(status = status, formatUrl = formatUrl)
                }
            }
        }
    }
}

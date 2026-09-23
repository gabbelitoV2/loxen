package com.moblin.android.view.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.common.various.personalHotspotLocalAddress
import com.moblin.android.common.various.urlImage
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FullScreenCover
import com.moblin.android.platform.swiftui.LocalTint
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.StatusOther
import com.moblin.android.various.network.IPMonitor
import com.moblin.android.various.utils.generateQrCode

@Composable
fun UrlCopyView(url: String, image: String? = null) {
    val presentingQrCode = remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (image != null) {
            SystemImage(name = image, fontSize = 17.sp, tint = LocalContentColor.current)
        }
        Text(text = url)
        Spacer(modifier = Modifier.weight(1f))
        Column(
            verticalArrangement = Arrangement.spacedBy(11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CopyToClipboardButtonView(text = url)
            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()
            Box(
                modifier = Modifier
                    .alpha(if (pressed) 0.2f else 1f)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                    ) {
                        presentingQrCode.value = true
                    },
            ) {
                SystemImage(name = "qrcode", fontSize = 20.sp, tint = LocalTint.current.takeOrElse { formPalette().accent })
            }
        }
    }
    FullScreenCover(isPresented = presentingQrCode) {
        val qrCode = remember(url) { generateQrCode(url) }
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .alpha(if (pressed) 0.2f else 1f)
                    .clickable(interactionSource = interactionSource, indication = null) {
                        presentingQrCode.value = false
                    }
                    .background(Color.White),
            ) {
                HCenter {
                    if (qrCode != null) {
                        Image(
                            bitmap = qrCode.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.aspectRatio(qrCode.width.toFloat() / qrCode.height.toFloat()),
                            contentScale = ContentScale.Fit,
                            filterQuality = FilterQuality.None,
                        )
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
    val ipStatuses by status.ipStatuses.collectAsState()
    Column(modifier = modifier) {
        Section(header = localized("IPv4")) {
            ipStatuses.filter { it.ipType == IPMonitor.IPType.ipv4 }.forEach { ipStatus ->
                UrlCopyView(
                    url = formatUrl(ipStatus.ipType.formatAddress(ipStatus.ip)),
                    image = urlImage(interfaceType = ipStatus.interfaceType.ordinal),
                )
            }
            UrlCopyView(url = formatUrl(personalHotspotLocalAddress), image = "personalhotspot")
        }
    }
}

@Composable
fun UrlsIpv6View(
    status: StatusOther,
    formatUrl: (String) -> String,
    modifier: Modifier = Modifier,
) {
    val ipStatuses by status.ipStatuses.collectAsState()
    Column(modifier = modifier) {
        Section(header = localized("IPv6")) {
            ipStatuses.filter { it.ipType == IPMonitor.IPType.ipv6 }.forEach { ipStatus ->
                UrlCopyView(
                    url = formatUrl(ipStatus.ipType.formatAddress(ipStatus.ip)),
                    image = urlImage(interfaceType = ipStatus.interfaceType.ordinal),
                )
            }
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
    NavigationLink(
        destination = {
            UrlsViewDestination(
                status = status,
                formatUrl = formatUrl,
                title = title,
                showIPv6 = showIPv6,
            )
        },
    ) {
        Text(text = title)
    }
}

@Composable
fun UrlsViewDestination(
    status: StatusOther,
    formatUrl: (String) -> String,
    title: String = localized("URLs"),
    showIPv6: Boolean = true,
) {
    Form(title = title) {
        UrlsIpv4View(status = status, formatUrl = formatUrl)
        if (showIPv6) {
            UrlsIpv6View(status = status, formatUrl = formatUrl)
        }
    }
}

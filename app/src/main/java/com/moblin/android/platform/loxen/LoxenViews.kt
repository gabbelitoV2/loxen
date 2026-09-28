package com.moblin.android.platform.loxen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.moblin.android.platform.Bundle
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormButton
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formFootnoteStyle
import com.moblin.android.platform.uikit.UIApplication
import com.moblin.android.platform.uikit.open

@Composable
fun LoxenAboutSection() {
    Section(footerContent = { Text(text = Loxen.attribution) }) {
        NavigationLink("License") {
            LoxenLicenseView()
        }
        FormButton(title = "Source code") {
            UIApplication.shared.open(Loxen.repositoryUrl)
        }
    }
}

@Composable
fun LoxenLicenseView() {
    Form(title = "License") {
        Section {
            Text(text = Loxen.license, style = formFootnoteStyle)
        }
    }
}

@Composable
fun LoxenSettingsView() {
    val icon = remember { Bundle.image("AppIconNoBackground")?.asImageBitmap() }
    Form(title = Loxen.appName) {
        Section(footerContent = { Text(text = Loxen.noPurchases) }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (icon != null) {
                    Image(bitmap = icon, contentDescription = Loxen.appName, modifier = Modifier.size(96.dp))
                }
            }
        }
        LoxenAboutSection()
        Section {
            FormButton(title = "Privacy policy") {
                UIApplication.shared.open(Loxen.privacyPolicyUrl)
            }
        }
    }
}

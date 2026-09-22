package com.moblin.android.view.settings.deeplinkcreator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.various.settings.DeepLinkCreatorWebBrowser
import com.moblin.android.view.utils.TextEditNavigationView

private fun submitHome(webBrowser: DeepLinkCreatorWebBrowser, value: String) {
    webBrowser.home = value
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeepLinkCreatorWebBrowserSettingsView(webBrowser: DeepLinkCreatorWebBrowser) {
    val home = webBrowser.home
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized("Web browser")) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            LazyColumn {
                item {
                    TextEditNavigationView(
                        title = localized("Home"),
                        value = home,
                        onChange = { newValue ->
                            submitHome(webBrowser, newValue)
                            null
                        }
                    )
                }
            }
        }
    }
}

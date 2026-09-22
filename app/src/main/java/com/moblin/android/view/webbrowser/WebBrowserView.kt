package com.moblin.android.view.webbrowser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.Orientation
import com.moblin.android.various.model.WebBrowserState
import com.moblin.android.various.model.getWebBrowser
import com.moblin.android.various.model.loadWebBrowserPage
import com.moblin.android.various.model.loadWebBrowserUrl
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.WebBrowserBookmarkSettings
import com.moblin.android.various.settings.WebBrowserSettings
import com.moblin.android.view.stream.overlay.right.segmentHeight
import com.moblin.android.view.stream.overlay.right.segmentHeightBig
import com.moblin.android.view.utils.CloseToolbar
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.LocalModel

private val smallBrowserSide = 250.dp

@Composable
fun WebView(model: Model = LocalModel.current) {
    AndroidView(factory = { model.getWebBrowser() })
}

@Composable
private fun UrlView(model: Model = LocalModel.current) {
    val url by model.webBrowserUrl.collectAsState()
    OutlinedTextField(
        value = url,
        onValueChange = { model.webBrowserUrl.value = it },
        placeholder = { Text("Search with Google or enter address") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Go,
        ),
        keyboardActions = KeyboardActions(
            onGo = {
                model.webBrowserUrl.value = model.webBrowserUrl.value.trim()
                model.loadWebBrowserUrl()
            },
        ),
        modifier = Modifier
            .padding(5.dp)
            .border(1.dp, Color.Gray, RoundedCornerShape(5.dp)),
    )
}

@Composable
private fun NextPrevView(model: Model = LocalModel.current) {
    Row {
        IconButton(
            onClick = { model.getWebBrowser().goBack() },
            enabled = model.getWebBrowser().canGoBack(),
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowLeft,
                contentDescription = null,
                modifier = Modifier.padding(7.dp),
            )
        }
        IconButton(
            onClick = { model.getWebBrowser().goForward() },
            enabled = model.getWebBrowser().canGoForward(),
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.padding(7.dp),
            )
        }
    }
}

@Composable
private fun RefreshBookmarksView(
    model: Model = LocalModel.current,
    showingBookmarks: Boolean,
    onShowingBookmarksChange: (Boolean) -> Unit,
    isSmall: Boolean,
    onIsSmallChange: (Boolean) -> Unit,
) {
    Row {
        IconButton(onClick = { model.getWebBrowser().reload() }) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.padding(7.dp),
            )
        }
        IconButton(onClick = { onShowingBookmarksChange(true) }) {
            Icon(
                imageVector = Icons.Default.BookmarkBorder,
                contentDescription = null,
                modifier = Modifier.padding(7.dp),
            )
        }
        IconButton(onClick = { onIsSmallChange(!isSmall) }) {
            Icon(
                imageVector = Icons.Default.CloseFullscreen,
                contentDescription = null,
                modifier = Modifier.padding(7.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarksView(
    model: Model = LocalModel.current,
    webBrowser: WebBrowserSettings,
    presentingBookmarks: Boolean,
    onPresentingBookmarksChange: (Boolean) -> Unit,
) {
    val bookmarks = webBrowser.bookmarks
    val onDelete: (List<Int>) -> Unit = { offsets ->
        val newBookmarks = webBrowser.bookmarks.toMutableList()
        offsets.sortedDescending().forEach { index -> newBookmarks.removeAt(index) }
        webBrowser.bookmarks = newBookmarks
    }
    val onMove: (Int, Int) -> Unit = { froms, to ->
        val newBookmarks = webBrowser.bookmarks.toMutableList()
        val item = newBookmarks.removeAt(froms)
        newBookmarks.add(to, item)
        webBrowser.bookmarks = newBookmarks
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        TopAppBar(
            title = { Text("Bookmarks") },
            navigationIcon = {
                CloseToolbar(
                    presenting = presentingBookmarks,
                    onPresentingChange = onPresentingBookmarksChange,
                )
            },
        )
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(bookmarks, key = { it.id.toString() }) { bookmark ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DraggableItemPrefixView()
                    TextButton(
                        onClick = {
                            model.loadWebBrowserPage(url = bookmark.url)
                            onPresentingBookmarksChange(false)
                        },
                    ) {
                        Text(bookmark.url)
                    }
                }
                Unit
            }
        }
        SwipeLeftToDeleteHelpView(kind = localized("a bookmark"))
        HorizontalDivider()
        TextButtonView("Create bookmark") {
            val bookmark = WebBrowserBookmarkSettings()
            bookmark.url = model.webBrowserUrl.value
            webBrowser.bookmarks = webBrowser.bookmarks.toMutableList().apply { add(bookmark) }
        }
    }
}

@Composable
private fun mapSide(maximum: Dp): Dp {
    return minOf(maximum - 130.dp, smallBrowserSide)
}

@Composable
private fun offset(database: Database): Dp {
    return if (database.bigButtons) {
        (-(2 * segmentHeightBig + 10)).dp
    } else {
        (-(2 * segmentHeight + 10)).dp
    }
}

@Composable
private fun WebBrowserSmallView(
    model: Model = LocalModel.current,
    database: Database,
    webBrowserState: WebBrowserState,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val sideWidth = mapSide(maxWidth)
        val sideHeight = mapSide(maxHeight)
        Box(modifier = Modifier.offset(y = offset(database))) {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.weight(1f))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .padding(end = 3.dp)
                            .width(sideWidth)
                            .height(sideHeight)
                            .clip(RoundedCornerShape(7.dp)),
                    ) {
                        WebView(model = model)
                    }
                }
            }
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.weight(1f))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            webBrowserState.setIsSmall(!webBrowserState.isSmall.value)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .padding(end = 10.dp, bottom = 10.dp)
                                .padding(8.dp)
                                .size(12.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebBrowserBigView(
    model: Model = LocalModel.current,
    database: Database,
    orientation: Orientation,
    webBrowserState: WebBrowserState,
) {
    val isPortrait by orientation.isPortrait.collectAsState()
    val isSmall by webBrowserState.isSmall.collectAsState()
    var presentingBookmarks by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        if (isPortrait) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp)
                    .background(MaterialTheme.colorScheme.background),
            ) {
                UrlView(model = model)
                Row(modifier = Modifier.fillMaxWidth()) {
                    NextPrevView(model = model)
                    Spacer(modifier = Modifier.weight(1f))
                    RefreshBookmarksView(
                        model = model,
                        showingBookmarks = presentingBookmarks,
                        onShowingBookmarksChange = { presentingBookmarks = it },
                        isSmall = isSmall,
                        onIsSmallChange = { webBrowserState.setIsSmall(it) },
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp)
                    .background(MaterialTheme.colorScheme.background),
            ) {
                NextPrevView(model = model)
                UrlView(model = model)
                RefreshBookmarksView(
                    model = model,
                    showingBookmarks = presentingBookmarks,
                    onShowingBookmarksChange = { presentingBookmarks = it },
                    isSmall = isSmall,
                    onIsSmallChange = { webBrowserState.setIsSmall(it) },
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            WebView(model = model)
        }
    }
    if (presentingBookmarks) {
        ModalBottomSheet(onDismissRequest = { presentingBookmarks = false }) {
            BookmarksView(
                model = model,
                webBrowser = database.webBrowser,
                presentingBookmarks = presentingBookmarks,
                onPresentingBookmarksChange = { presentingBookmarks = it },
            )
        }
    }
}

@Composable
fun WebBrowserView(
    model: Model = LocalModel.current,
    database: Database,
    orientation: Orientation,
    webBrowserState: WebBrowserState,
) {
    val isSmall by webBrowserState.isSmall.collectAsState()
    if (isSmall) {
        WebBrowserSmallView(
            model = model,
            database = database,
            webBrowserState = webBrowserState,
        )
    } else {
        WebBrowserBigView(
            model = model,
            database = database,
            orientation = orientation,
            webBrowserState = webBrowserState,
        )
    }
}

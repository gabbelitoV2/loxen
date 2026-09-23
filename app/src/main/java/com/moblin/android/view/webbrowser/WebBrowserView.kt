package com.moblin.android.view.webbrowser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Button
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Sheet
import com.moblin.android.platform.swiftui.binding
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.platform.swiftui.moving
import com.moblin.android.platform.swiftui.removing
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
import com.moblin.android.view.utils.ContextMenuDeleteButton
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
private fun UrlView(model: Model = LocalModel.current, modifier: Modifier = Modifier) {
    val palette = formPalette()
    val url by model.webBrowserUrl.collectAsState()
    BasicTextField(
        value = url,
        onValueChange = { model.webBrowserUrl.value = it },
        modifier = modifier
            .border(1.dp, palette.secondaryLabel, RoundedCornerShape(5.dp))
            .padding(5.dp),
        textStyle = formBodyStyle.copy(color = palette.label),
        singleLine = true,
        cursorBrush = SolidColor(palette.accent),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Go,
        ),
        keyboardActions = KeyboardActions(
            onGo = {
                model.webBrowserUrl.value = model.webBrowserUrl.value.trim()
                model.loadWebBrowserUrl()
            },
        ),
        decorationBox = { innerTextField ->
            Box {
                if (url.isEmpty()) {
                    Text(
                        text = localized("Search with Google or enter address"),
                        style = formBodyStyle,
                        color = palette.tertiaryLabel,
                        maxLines = 1,
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun ToolbarButtonView(systemImage: String, enabled: Boolean = true, action: () -> Unit) {
    Button(action = action, enabled = enabled) {
        SystemImage(
            name = systemImage,
            fontSize = 17.sp,
            tint = LocalContentColor.current,
            modifier = Modifier.padding(7.dp),
        )
    }
}

@Composable
private fun NextPrevView(model: Model = LocalModel.current) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolbarButtonView(systemImage = "chevron.left", enabled = model.getWebBrowser().canGoBack()) {
            model.getWebBrowser().goBack()
        }
        ToolbarButtonView(systemImage = "chevron.right", enabled = model.getWebBrowser().canGoForward()) {
            model.getWebBrowser().goForward()
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
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolbarButtonView(systemImage = "arrow.clockwise") {
            model.getWebBrowser().reload()
        }
        ToolbarButtonView(systemImage = "bookmark") {
            onShowingBookmarksChange(true)
        }
        ToolbarButtonView(systemImage = "arrow.down.right.and.arrow.up.left") {
            onIsSmallChange(!isSmall)
        }
    }
}

@Composable
private fun BookmarksView(
    model: Model = LocalModel.current,
    webBrowser: WebBrowserSettings,
    presentingBookmarks: Boolean,
    onPresentingBookmarksChange: (Boolean) -> Unit,
) {
    var bookmarks by binding({ webBrowser.bookmarks }) { webBrowser.bookmarks = it }
    Form(
        title = "Bookmarks",
        toolbar = {
            CloseToolbar(
                presenting = presentingBookmarks,
                onPresentingChange = onPresentingBookmarksChange,
            )
        },
    ) {
        Section(
            footerContent = {
                SwipeLeftToDeleteHelpView(kind = localized("a bookmark"))
            },
        ) {
            ForEach(
                bookmarks,
                id = { it.id },
                onDelete = { offsets ->
                    bookmarks = bookmarks.removing(atOffsets = offsets)
                },
                onMove = { froms, to ->
                    bookmarks = bookmarks.moving(fromOffsets = froms, toOffset = to)
                },
            ) { bookmark ->
                ContextMenuDeleteButton(
                    action = {
                        bookmarks = bookmarks.filterNot { it.id == bookmark.id }
                    },
                ) {
                    FormRow(
                        onClick = {
                            model.loadWebBrowserPage(url = bookmark.url)
                            onPresentingBookmarksChange(false)
                        },
                    ) {
                        DraggableItemPrefixView()
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = bookmark.url, color = formPalette().accent)
                    }
                }
            }
        }
        Section {
            TextButtonView("Create bookmark") {
                val bookmark = WebBrowserBookmarkSettings()
                bookmark.url = model.webBrowserUrl.value
                bookmarks = bookmarks + bookmark
            }
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
                    Button(
                        action = {
                            webBrowserState.setIsSmall(!webBrowserState.isSmall.value)
                        },
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(end = 10.dp, bottom = 10.dp)
                                .padding(16.dp)
                                .size(12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            SystemImage(
                                name = "arrow.up.left.and.arrow.down.right",
                                fontSize = 17.sp,
                                tint = formPalette().label,
                                modifier = Modifier.wrapContentSize(unbounded = true),
                            )
                        }
                    }
                }
            }
        }
    }
}

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
    val background = if (isSystemInDarkTheme()) Color.Black else Color.White
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        if (isPortrait) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(background)
                    .padding(3.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                UrlView(model = model, modifier = Modifier.fillMaxWidth())
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
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
                    .background(background)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NextPrevView(model = model)
                UrlView(model = model, modifier = Modifier.weight(1f))
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
    Sheet(isPresented = presentingBookmarks, onDismissRequest = { presentingBookmarks = false }) {
        BookmarksView(
            model = model,
            webBrowser = database.webBrowser,
            presentingBookmarks = presentingBookmarks,
            onPresentingBookmarksChange = { presentingBookmarks = it },
        )
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

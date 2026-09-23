package com.moblin.android.platform.swiftui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.moblinwatch.shared.WatchSettings
import com.moblin.android.platform.SystemImage
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.appModeChanged
import com.moblin.android.various.model.sendInitToWatch
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsAppMode
import com.moblin.android.various.utils.isMac
import com.moblin.android.various.utils.isPad
import com.moblin.android.various.utils.isPhone
import com.moblin.android.view.settings.about.AboutSettingsView
import com.moblin.android.view.settings.applemusic.AppleMusicSettingsView
import com.moblin.android.view.settings.audio.AudioSettingsView
import com.moblin.android.view.settings.blacksharkcoolers.BlackSharkCoolerDevicesSettingsView
import com.moblin.android.view.settings.camera.CameraSettingsView
import com.moblin.android.view.settings.catprinters.CatPrintersSettingsView
import com.moblin.android.view.settings.chat.ChatSettingsView
import com.moblin.android.view.settings.debug.DebugSettingsView
import com.moblin.android.view.settings.deeplinkcreator.DeepLinkCreatorSettingsView
import com.moblin.android.view.settings.display.DisplaySettingsView
import com.moblin.android.view.settings.djidevices.DjiDevicesSettingsView
import com.moblin.android.view.settings.gamecontrollers.GameControllersSettingsView
import com.moblin.android.view.settings.gimbal.GimbalSettingsView
import com.moblin.android.view.settings.gopro.GoProSettingsView
import com.moblin.android.view.settings.helpandsupport.HelpAndSupportSettingsView
import com.moblin.android.view.settings.importexport.ImportExportSettingsView
import com.moblin.android.view.settings.ingests.IngestsSettingsView
import com.moblin.android.view.settings.keyboard.KeyboardSettingsView
import com.moblin.android.view.settings.location.LocationSettingsView
import com.moblin.android.view.settings.macros.MacrosSettingsView
import com.moblin.android.view.settings.mediaplayer.MediaPlayersSettingsView
import com.moblin.android.view.settings.moblink.MoblinkSettingsView
import com.moblin.android.view.settings.recordings.RecordingsSettingsView
import com.moblin.android.view.settings.remotecontrol.RemoteControlSettingsView
import com.moblin.android.view.settings.savereset.SettingsResetView
import com.moblin.android.view.settings.savereset.SettingsSaveView
import com.moblin.android.view.settings.scenes.ScenesSettingsView
import com.moblin.android.view.settings.selfiestick.SelfieStickSettingsView
import com.moblin.android.view.settings.store.StoreSettingsView
import com.moblin.android.view.settings.streamdeck.StreamDecksSettingsView
import com.moblin.android.view.settings.streaminghistory.StreamingHistorySettingsView
import com.moblin.android.view.settings.streams.StreamsSettingsView
import com.moblin.android.view.settings.talkback.TalkbackSettingsView
import com.moblin.android.view.settings.tesla.TeslaSettingsView
import com.moblin.android.view.settings.watch.WatchSettingsView
import com.moblin.android.view.settings.workoutdevices.WorkoutDevicesSettingsView
import com.moblin.android.view.utils.InfoBannerView
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class FormPalette(
    val groupedBackground: Color,
    val cell: Color,
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val separator: Color,
    val accent: Color,
    val green: Color,
    val red: Color,
    val gray: Color,
    val highlight: Color,
    val switchOff: Color,
    val sliderTrack: Color,
    val bar: Color,
    val menu: Color,
)

private val lightFormPalette = FormPalette(
    groupedBackground = Color(0xFFF2F2F7),
    cell = Color(0xFFFFFFFF),
    label = Color(0xFF000000),
    secondaryLabel = Color(0x993C3C43),
    tertiaryLabel = Color(0x4D3C3C43),
    separator = Color(0x4A3C3C43),
    accent = Color(0xFF007AFF),
    green = Color(0xFF34C759),
    red = Color(0xFFFF3B30),
    gray = Color(0xFF8E8E93),
    highlight = Color(0xFFD1D1D6),
    switchOff = Color(0xFFE9E9EB),
    sliderTrack = Color(0x33787880),
    bar = Color(0xF0F9F9F9),
    menu = Color(0xFFEFEFF0),
)

private val darkFormPalette = FormPalette(
    groupedBackground = Color(0xFF000000),
    cell = Color(0xFF1C1C1E),
    label = Color(0xFFFFFFFF),
    secondaryLabel = Color(0x99EBEBF5),
    tertiaryLabel = Color(0x4DEBEBF5),
    separator = Color(0x99545458),
    accent = Color(0xFF0A84FF),
    green = Color(0xFF30D158),
    red = Color(0xFFFF453A),
    gray = Color(0xFF8E8E93),
    highlight = Color(0xFF3A3A3C),
    switchOff = Color(0xFF39393D),
    sliderTrack = Color(0x5C787880),
    bar = Color(0xF0161616),
    menu = Color(0xFF252527),
)

private val darkElevatedFormPalette = FormPalette(
    groupedBackground = Color(0xFF1C1C1E),
    cell = Color(0xFF2C2C2E),
    label = Color(0xFFFFFFFF),
    secondaryLabel = Color(0x99EBEBF5),
    tertiaryLabel = Color(0x4DEBEBF5),
    separator = Color(0x99545458),
    accent = Color(0xFF0A84FF),
    green = Color(0xFF30D158),
    red = Color(0xFFFF453A),
    gray = Color(0xFF8E8E93),
    highlight = Color(0xFF3A3A3C),
    switchOff = Color(0xFF39393D),
    sliderTrack = Color(0x5C787880),
    bar = Color(0xF0252527),
    menu = Color(0xFF252527),
)

private val LocalElevated = staticCompositionLocalOf { false }

@Composable
fun formPalette(): FormPalette = if (!isSystemInDarkTheme()) {
    lightFormPalette
} else if (LocalElevated.current) {
    darkElevatedFormPalette
} else {
    darkFormPalette
}

val formBodyStyle = TextStyle(fontSize = 17.sp, lineHeight = 22.sp)

val formFootnoteStyle = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)

private val formMargin = 16.dp
private val formRowPadding = 16.dp
private val formRowMinHeight = 44.dp
private val formRowVerticalPadding = 11.dp
private val formSectionSpacing = 35.dp
private val formCornerRadius = 10.dp
private val formLabelIconWidth = 28.dp
private val formLabelSpacing = 14.dp

val LocalTint = compositionLocalOf { Color.Unspecified }

private val LocalInSection = compositionLocalOf { false }

@Stable
class FormRowInfo {
    var separatorInset by mutableStateOf(formRowPadding)
}

val LocalFormRowInfo = compositionLocalOf<FormRowInfo?> { null }

@Stable
class NavigationEntry internal constructor(
    internal val key: Int,
    internal val parent: NavigationEntry?,
    internal val content: (@Composable () -> Unit)?,
) {
    internal val depth: Int = if (parent == null) 0 else parent.depth + 1
    var title by mutableStateOf("")
    var toolbar by mutableStateOf<(@Composable RowScope.() -> Unit)?>(null)
    var scrolled by mutableStateOf(false)
}

@Stable
class Navigator internal constructor() {
    internal val entries = mutableStateListOf(NavigationEntry(key = 0, parent = null, content = null))
    private var nextKey = 1

    fun push(destination: @Composable () -> Unit) {
        entries.add(NavigationEntry(key = nextKey++, parent = entries.last(), content = destination))
    }

    fun pop() {
        if (entries.size > 1) {
            entries.removeAt(entries.lastIndex)
        }
    }
}

val LocalNavigator = staticCompositionLocalOf<Navigator?> { null }

val LocalNavigationEntry = staticCompositionLocalOf<NavigationEntry?> { null }

private class PropertyBinding<T>(
    private val read: () -> T,
    private val write: (T) -> Unit,
    private val version: MutableIntState,
) : MutableState<T> {
    override var value: T
        get() = read()
        set(newValue) {
            write(newValue)
            version.intValue += 1
        }

    override operator fun component1(): T = value

    override operator fun component2(): (T) -> Unit = { value = it }
}

@Composable
fun <T> binding(get: () -> T, set: (T) -> Unit): MutableState<T> {
    val version = remember { mutableIntStateOf(0) }
    version.intValue
    return PropertyBinding(get, set, version)
}

@Composable
fun NavigationStack(content: @Composable () -> Unit) {
    if (LocalNavigator.current != null) {
        content()
        return
    }
    val navigator = remember { Navigator() }
    val stateHolder = rememberSaveableStateHolder()
    val palette = formPalette()
    val top = navigator.entries.last()
    BackHandler(enabled = navigator.entries.size > 1) {
        navigator.pop()
    }
    val keys = navigator.entries.map { it.key }.toSet()
    val knownKeys = remember { mutableSetOf<Int>() }
    SideEffect {
        for (key in knownKeys - keys) {
            stateHolder.removeState(key)
        }
        knownKeys.clear()
        knownKeys.addAll(keys)
    }
    AnimatedContent(
        targetState = top,
        modifier = Modifier
            .fillMaxSize()
            .background(palette.groupedBackground),
        transitionSpec = {
            if (targetState.depth > initialState.depth) {
                (slideInHorizontally(tween(300)) { it } togetherWith
                    slideOutHorizontally(tween(300)) { -it / 3 }).apply { targetContentZIndex = 1f }
            } else {
                (slideInHorizontally(tween(300)) { -it / 3 } togetherWith
                    slideOutHorizontally(tween(300)) { it }).apply { targetContentZIndex = -1f }
            }
        },
        label = "NavigationStack",
    ) { entry ->
        CompositionLocalProvider(
            LocalNavigator provides navigator,
            LocalNavigationEntry provides entry,
            LocalContentColor provides palette.label,
            LocalTextStyle provides formBodyStyle,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.groupedBackground),
            ) {
                NavigationBar(navigator = navigator, entry = entry)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    stateHolder.SaveableStateProvider(entry.key) {
                        val destination = entry.content
                        if (destination == null) {
                            content()
                        } else {
                            destination()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationBar(navigator: Navigator, entry: NavigationEntry) {
    val palette = formPalette()
    val scrolled = entry.scrolled
    val parent = entry.parent
    val toolbar = entry.toolbar
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (scrolled) palette.bar else palette.groupedBackground)
            .drawBehind {
                if (scrolled) {
                    drawRect(
                        color = palette.separator,
                        topLeft = Offset(0f, size.height - 1f),
                        size = Size(size.width, 1f),
                    )
                }
            }
            .height(44.dp),
    ) {
        if (parent != null) {
            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()
            val backTitle = if (parent.title.isNotEmpty() && parent.title.length <= 14) {
                parent.title
            } else {
                localized("Back")
            }
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { navigator.pop() },
                    )
                    .padding(end = 8.dp)
                    .alpha(if (pressed) 0.3f else 1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SystemImage(
                    name = "chevron.left",
                    fontSize = 34.sp,
                    tint = palette.accent,
                    modifier = Modifier.offset(x = (-4).dp),
                )
                Text(
                    text = backTitle,
                    color = palette.accent,
                    style = formBodyStyle,
                    maxLines = 1,
                    modifier = Modifier.offset(x = (-6).dp),
                )
            }
        }
        Text(
            text = entry.title,
            color = palette.label,
            style = formBodyStyle.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = if (parent != null || toolbar != null) 96.dp else 16.dp),
        )
        if (toolbar != null) {
            CompositionLocalProvider(LocalContentColor provides palette.accent) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    toolbar()
                }
            }
        }
    }
}

@Composable
fun NavigationTitle(title: String) {
    val entry = LocalNavigationEntry.current ?: return
    val text = localized(title)
    SideEffect {
        entry.title = text
    }
}

@Composable
fun NavigationToolbar(toolbar: @Composable RowScope.() -> Unit) {
    val entry = LocalNavigationEntry.current ?: return
    SideEffect {
        entry.toolbar = toolbar
    }
}

@Composable
fun Form(
    title: String? = null,
    modifier: Modifier = Modifier,
    toolbar: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val entry = LocalNavigationEntry.current
    if (entry == null) {
        NavigationStack {
            Form(title = title, modifier = modifier, toolbar = toolbar, content = content)
        }
        return
    }
    if (title != null) {
        NavigationTitle(title)
    }
    if (toolbar != null) {
        NavigationToolbar(toolbar)
    }
    val palette = formPalette()
    val scrollState = rememberScrollState()
    LaunchedEffect(entry, scrollState) {
        snapshotFlow { scrollState.value > 0 }.collect {
            entry.scrolled = it
        }
    }
    CompositionLocalProvider(
        LocalContentColor provides palette.label,
        LocalTextStyle provides formBodyStyle,
        LocalInSection provides false,
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(palette.groupedBackground)
                .verticalScroll(scrollState)
                .padding(bottom = formSectionSpacing),
            content = content,
        )
    }
}

private enum class SectionSlot {
    Header,
    Rows,
    Footer,
}

@Composable
fun Section(
    header: String? = null,
    footer: String? = null,
    headerContent: (@Composable () -> Unit)? = null,
    footerContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (LocalInSection.current) {
        content()
        return
    }
    val palette = formPalette()
    Layout(
        content = {
            Box(
                modifier = Modifier
                    .layoutId(SectionSlot.Header)
                    .fillMaxWidth(),
            ) {
                SectionHeader(header = header, headerContent = headerContent)
            }
            Box(
                modifier = Modifier
                    .layoutId(SectionSlot.Rows)
                    .fillMaxWidth()
                    .padding(horizontal = formMargin)
                    .clip(RoundedCornerShape(formCornerRadius))
                    .background(palette.cell),
            ) {
                CompositionLocalProvider(
                    LocalInSection provides true,
                    LocalContentColor provides palette.label,
                    LocalTextStyle provides formBodyStyle,
                ) {
                    FormRows(content = content)
                }
            }
            Box(
                modifier = Modifier
                    .layoutId(SectionSlot.Footer)
                    .fillMaxWidth(),
            ) {
                SectionFooter(footer = footer, footerContent = footerContent)
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val loose = constraints.copy(minHeight = 0)
        val rows = measurables.first { it.layoutId == SectionSlot.Rows }.measure(loose)
        if (rows.height == 0) {
            layout(constraints.minWidth, 0) {}
        } else {
            val header = measurables.first { it.layoutId == SectionSlot.Header }.measure(loose)
            val footer = measurables.first { it.layoutId == SectionSlot.Footer }.measure(loose)
            val width = maxOf(header.width, rows.width, footer.width)
            layout(width, header.height + rows.height + footer.height) {
                header.place(0, 0)
                rows.place(0, header.height)
                footer.place(0, header.height + rows.height)
            }
        }
    }
}

@Composable
private fun SectionHeader(header: String?, headerContent: (@Composable () -> Unit)?) {
    if (header == null && headerContent == null) {
        Spacer(modifier = Modifier.height(formSectionSpacing))
        return
    }
    val palette = formPalette()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = formMargin + formRowPadding,
                end = formMargin + formRowPadding,
                top = 22.dp,
                bottom = 7.dp,
            ),
    ) {
        CompositionLocalProvider(
            LocalContentColor provides palette.secondaryLabel,
            LocalTextStyle provides formFootnoteStyle,
        ) {
            if (header != null) {
                Text(text = localized(header).uppercase())
            } else {
                headerContent?.invoke()
            }
        }
    }
}

@Composable
private fun SectionFooter(footer: String?, footerContent: (@Composable () -> Unit)?) {
    if (footer == null && footerContent == null) {
        return
    }
    val palette = formPalette()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = formMargin + formRowPadding, end = formMargin + formRowPadding, top = 7.dp),
    ) {
        CompositionLocalProvider(
            LocalContentColor provides palette.secondaryLabel,
            LocalTextStyle provides formFootnoteStyle,
        ) {
            if (footer != null) {
                Text(text = localized(footer))
            } else {
                footerContent?.invoke()
            }
        }
    }
}

private class FormRowPlacement(
    val placeable: Placeable,
    val x: Int,
    val height: Int,
    val separatorInset: Int,
)

@Composable
private fun FormRows(content: @Composable () -> Unit) {
    val palette = formPalette()
    val separators = remember { mutableStateOf(IntArray(0)) }
    Layout(
        content = content,
        modifier = Modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                val lines = separators.value
                var index = 0
                while (index + 1 < lines.size) {
                    val y = lines[index].toFloat()
                    val x = lines[index + 1].toFloat()
                    drawRect(
                        color = palette.separator,
                        topLeft = Offset(x, y - 1f),
                        size = Size(size.width - x, 1f),
                    )
                    index += 2
                }
            },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val padding = formRowPadding.roundToPx()
        val minHeight = formRowMinHeight.roundToPx()
        val verticalPadding = formRowVerticalPadding.roundToPx()
        val rows = ArrayList<FormRowPlacement>(measurables.size)
        for (measurable in measurables) {
            val info = measurable.layoutId as? FormRowInfo
            if (info != null) {
                val placeable = measurable.measure(Constraints(minWidth = width, maxWidth = width))
                if (placeable.height > 0) {
                    rows.add(
                        FormRowPlacement(
                            placeable = placeable,
                            x = 0,
                            height = placeable.height,
                            separatorInset = info.separatorInset.roundToPx(),
                        ),
                    )
                }
            } else {
                val placeable = measurable.measure(
                    Constraints(maxWidth = (width - 2 * padding).coerceAtLeast(0)),
                )
                if (placeable.height > 0) {
                    rows.add(
                        FormRowPlacement(
                            placeable = placeable,
                            x = padding,
                            height = max(minHeight, placeable.height + 2 * verticalPadding),
                            separatorInset = padding,
                        ),
                    )
                }
            }
        }
        val lines = IntArray(max(0, rows.size - 1) * 2)
        var y = 0
        rows.forEachIndexed { index, row ->
            if (index > 0) {
                lines[(index - 1) * 2] = y
                lines[(index - 1) * 2 + 1] = rows[index - 1].separatorInset
            }
            y += row.height
        }
        if (!lines.contentEquals(separators.value)) {
            separators.value = lines
        }
        layout(width, y) {
            var top = 0
            for (row in rows) {
                row.placeable.place(row.x, top + (row.height - row.placeable.height) / 2)
                top += row.height
            }
        }
    }
}

@Composable
fun FormRow(
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    highlight: Boolean = true,
    endPadding: Dp = formRowPadding,
    content: @Composable RowScope.() -> Unit,
) {
    val palette = formPalette()
    val info = remember { FormRowInfo() }
    var modifier = Modifier
        .layoutId(info)
        .fillMaxWidth()
        .heightIn(min = formRowMinHeight)
    var contentAlpha = if (enabled) 1f else 0.4f
    if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        if (pressed && highlight) {
            modifier = modifier.background(palette.highlight)
        } else if (pressed) {
            contentAlpha = 0.3f
        }
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
    }
    CompositionLocalProvider(LocalFormRowInfo provides info) {
        Row(
            modifier = modifier
                .padding(start = formRowPadding, end = endPadding, top = 6.dp, bottom = 6.dp)
                .alpha(contentAlpha),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
fun Label(title: String, systemImage: String, modifier: Modifier = Modifier) {
    val palette = formPalette()
    val info = LocalFormRowInfo.current
    SideEffect {
        info?.separatorInset = formRowPadding + formLabelIconWidth + formLabelSpacing
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(formLabelIconWidth), contentAlignment = Alignment.Center) {
            SystemImage(
                name = systemImage,
                fontSize = 22.sp,
                tint = LocalTint.current.takeOrElse { palette.accent },
            )
        }
        Spacer(modifier = Modifier.width(formLabelSpacing))
        Text(text = localized(title))
    }
}

@Composable
fun NavigationLink(
    destination: @Composable () -> Unit,
    enabled: Boolean = true,
    label: @Composable RowScope.() -> Unit,
) {
    val palette = formPalette()
    val navigator = LocalNavigator.current
    FormRow(
        onClick = { navigator?.push(destination) },
        enabled = enabled,
        endPadding = 8.dp,
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            label()
        }
        SystemImage(name = "chevron.right", fontSize = 24.sp, tint = palette.tertiaryLabel)
    }
}

@Composable
fun NavigationLink(
    title: String,
    enabled: Boolean = true,
    destination: @Composable () -> Unit,
) {
    NavigationLink(destination = destination, enabled = enabled) {
        Text(text = localized(title))
    }
}

@Composable
fun Toggle(
    title: String,
    isOn: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Toggle(isOn = isOn, onChange = onChange, enabled = enabled) {
        Text(text = localized(title))
    }
}

@Composable
fun Toggle(title: String, isOn: MutableState<Boolean>, enabled: Boolean = true) {
    Toggle(title = title, isOn = isOn.value, enabled = enabled) {
        isOn.value = it
    }
}

@Composable
fun Toggle(
    isOn: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    label: @Composable RowScope.() -> Unit,
) {
    FormRow {
        Row(
            modifier = Modifier
                .weight(1f)
                .alpha(if (enabled) 1f else 0.4f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            label()
        }
        Spacer(modifier = Modifier.width(8.dp))
        IosSwitch(checked = isOn, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
fun IosSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, enabled: Boolean = true) {
    val palette = formPalette()
    val progress by animateFloatAsState(targetValue = if (checked) 1f else 0f, animationSpec = tween(200))
    val interactionSource = remember { MutableInteractionSource() }
    var modifier = Modifier
        .size(width = 51.dp, height = 31.dp)
        .alpha(if (enabled) 1f else 0.5f)
        .background(lerp(palette.switchOff, palette.green, progress), RoundedCornerShape(50))
    if (onCheckedChange != null) {
        modifier = modifier.toggleable(
            value = checked,
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        )
    }
    Box(modifier = modifier.padding(2.dp)) {
        Box(
            modifier = Modifier
                .offset { IntOffset((20.dp.toPx() * progress).roundToInt(), 0) }
                .size(27.dp)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .background(Color.White, CircleShape),
        )
    }
}

@Composable
fun <T> Picker(
    title: String,
    selection: T,
    options: List<T>,
    enabled: Boolean = true,
    text: (T) -> String = { it.toString() },
    onChange: (T) -> Unit,
) {
    val palette = formPalette()
    var expanded by remember { mutableStateOf(false) }
    FormRow(onClick = { expanded = true }, enabled = enabled, highlight = false) {
        Text(text = localized(title), modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(8.dp))
        Box {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = text(selection),
                    color = palette.secondaryLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(4.dp))
                SystemImage(name = "chevron.up.chevron.down", fontSize = 15.sp, tint = palette.secondaryLabel)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                shape = RoundedCornerShape(13.dp),
                containerColor = palette.menu,
                tonalElevation = 0.dp,
                shadowElevation = 12.dp,
            ) {
                options.forEachIndexed { index, option ->
                    if (index > 0) {
                        HorizontalDivider(thickness = 0.5.dp, color = palette.separator)
                    }
                    DropdownMenuItem(
                        text = {
                            Text(text = text(option), style = formBodyStyle, color = palette.label)
                        },
                        leadingIcon = {
                            Box(modifier = Modifier.width(20.dp), contentAlignment = Alignment.Center) {
                                if (option == selection) {
                                    SystemImage(name = "checkmark", fontSize = 17.sp, tint = palette.label)
                                }
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        onClick = {
                            expanded = false
                            if (option != selection) {
                                onChange(option)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun FormButton(
    title: String,
    destructive: Boolean = false,
    centered: Boolean = false,
    enabled: Boolean = true,
    action: () -> Unit,
) {
    val palette = formPalette()
    val color = if (destructive) palette.red else LocalTint.current.takeOrElse { palette.accent }
    FormRow(onClick = action, enabled = enabled) {
        Text(
            text = localized(title),
            color = color,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun FormSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val palette = formPalette()
    val tint = LocalTint.current.takeOrElse { palette.accent }
    val span = valueRange.endInclusive - valueRange.start
    val fraction = if (span > 0f) ((value - valueRange.start) / span).coerceIn(0f, 1f) else 0f
    var widthPx by remember { mutableIntStateOf(0) }
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)
    Box(
        modifier = modifier
            .height(28.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .onSizeChanged { widthPx = it.width }
            .then(
                if (enabled) {
                    Modifier.pointerInputSlider(
                        valueRange = valueRange,
                        onValueChange = { currentOnValueChange(it) },
                        onValueChangeFinished = { currentOnValueChangeFinished?.invoke() },
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val thumb = 28.dp.toPx()
            val track = 4.dp.toPx()
            val centerY = size.height / 2
            val x = thumb / 2 + (size.width - thumb) * fraction
            drawRoundRect(
                color = palette.sliderTrack,
                topLeft = Offset(0f, centerY - track / 2),
                size = Size(size.width, track),
                cornerRadius = CornerRadius(track / 2),
            )
            drawRoundRect(
                color = tint,
                topLeft = Offset(0f, centerY - track / 2),
                size = Size(x, track),
                cornerRadius = CornerRadius(track / 2),
            )
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(((widthPx - 28.dp.toPx()) * fraction).roundToInt(), 0) }
                .size(28.dp)
                .shadow(elevation = 3.dp, shape = CircleShape)
                .background(Color.White, CircleShape),
        )
    }
}

private fun Modifier.pointerInputSlider(
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
): Modifier = this.pointerInput(valueRange) {
    val thumb = 28.dp.toPx()
    fun update(x: Float) {
        val usable = (size.width - thumb).coerceAtLeast(1f)
        val fraction = ((x - thumb / 2) / usable).coerceIn(0f, 1f)
        onValueChange(valueRange.start + fraction * (valueRange.endInclusive - valueRange.start))
    }
    detectHorizontalDragGestures(
        onDragStart = { update(it.x) },
        onDragEnd = { onValueChangeFinished() },
        onDragCancel = { onValueChangeFinished() },
        onHorizontalDrag = { change, _ ->
            change.consume()
            update(change.position.x)
        },
    )
}

enum class ButtonRole {
    cancel,
    destructive,
}

enum class Visibility {
    automatic,
    visible,
    hidden,
}

internal class DialogButton(
    val title: String,
    val role: ButtonRole?,
    val action: () -> Unit,
)

internal class DialogTextField(
    val title: String,
    val text: String,
    val onTextChange: (String) -> Unit,
    val secure: Boolean,
)

class DialogActions internal constructor() {
    internal val buttons = ArrayList<DialogButton>()
    internal val textFields = ArrayList<DialogTextField>()

    fun Button(title: String, role: ButtonRole? = null, action: () -> Unit = {}) {
        buttons.add(DialogButton(title = localized(title), role = role, action = action))
    }

    fun TextField(title: String, text: String, onTextChange: (String) -> Unit) {
        textFields.add(
            DialogTextField(title = localized(title), text = text, onTextChange = onTextChange, secure = false),
        )
    }

    fun TextField(title: String, text: MutableState<String>) {
        TextField(title = title, text = text.value, onTextChange = { text.value = it })
    }

    fun SecureField(title: String, text: String, onTextChange: (String) -> Unit) {
        textFields.add(
            DialogTextField(title = localized(title), text = text, onTextChange = onTextChange, secure = true),
        )
    }

    fun SecureField(title: String, text: MutableState<String>) {
        SecureField(title = title, text = text.value, onTextChange = { text.value = it })
    }
}

private class DialogColors(
    val dimming: Color,
    val material: Color,
    val cancel: Color,
    val pressed: Color,
    val field: Color,
)

private val lightDialogColors = DialogColors(
    dimming = Color(0x33000000),
    material = Color(0xF2F2F2F2),
    cancel = Color(0xFFFFFFFF),
    pressed = Color(0x1F000000),
    field = Color(0xFFFFFFFF),
)

private val darkDialogColors = DialogColors(
    dimming = Color(0x7A000000),
    material = Color(0xF2252527),
    cancel = Color(0xFF2C2C2E),
    pressed = Color(0x29FFFFFF),
    field = Color(0xFF1C1C1E),
)

@Composable
private fun dialogColors(): DialogColors = if (isSystemInDarkTheme()) darkDialogColors else lightDialogColors

private val presentationEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

private val dialogCornerRadius = 14.dp

private val actionSheetButtonStyle = TextStyle(fontSize = 20.sp, lineHeight = 25.sp)

@Composable
private fun rememberPresentation(isPresented: Boolean): MutableTransitionState<Boolean> {
    val state = remember { MutableTransitionState(false) }
    state.targetState = isPresented
    return state
}

private fun MutableTransitionState<Boolean>.isShowing(): Boolean = currentState || targetState

private fun Modifier.blockPointerInput(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent()
        }
    }
}

@Composable
private fun hairline(): Dp = with(LocalDensity.current) { 1.toDp() }

@Composable
private fun PresentationDialog(
    state: MutableTransitionState<Boolean>,
    onBack: () -> Unit,
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.setDimAmount(0f)
            window?.setWindowAnimations(0)
        }
        AnimatedVisibility(
            visibleState = state,
            enter = EnterTransition.None,
            exit = ExitTransition.None,
            content = content,
        )
    }
}

@Composable
private fun DialogButtonCell(
    title: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val colors = dialogColors()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = modifier
            .background(if (pressed) colors.pressed else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = title, style = style, color = color, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Separator(modifier: Modifier = Modifier.fillMaxWidth().height(hairline())) {
    Box(modifier = modifier.background(formPalette().separator))
}

@Composable
fun Sheet(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    Sheet(isPresented = true, onDismissRequest = onDismissRequest, content = content)
}

@Composable
fun Sheet(isPresented: MutableState<Boolean>, content: @Composable () -> Unit) {
    Sheet(isPresented = isPresented.value, onDismissRequest = { isPresented.value = false }, content = content)
}

@Composable
fun Sheet(isPresented: Boolean, onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberPresentation(isPresented)
    if (!state.isShowing()) {
        return
    }
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    PresentationDialog(state = state, onBack = onDismissRequest) {
        CompositionLocalProvider(LocalElevated provides true) {
            val palette = formPalette()
            val colors = dialogColors()
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .animateEnterExit(enter = fadeIn(tween(400)), exit = fadeOut(tween(300)))
                        .background(colors.dimming)
                        .pointerInput(Unit) {
                            detectTapGestures {
                                if (state.targetState) {
                                    currentOnDismissRequest()
                                }
                            }
                        },
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .animateEnterExit(
                            enter = slideInVertically(tween(450, easing = presentationEasing)) { it },
                            exit = slideOutVertically(tween(300, easing = presentationEasing)) { it },
                        )
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                        )
                        .padding(top = 10.dp)
                        .widthIn(max = 700.dp)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = formCornerRadius, topEnd = formCornerRadius))
                        .background(palette.groupedBackground)
                        .blockPointerInput()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
                ) {
                    CompositionLocalProvider(
                        LocalNavigator provides null,
                        LocalNavigationEntry provides null,
                        LocalInSection provides false,
                        LocalContentColor provides palette.label,
                        LocalTextStyle provides formBodyStyle,
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun Alert(
    title: String,
    isPresented: MutableState<Boolean>,
    message: String? = null,
    actions: DialogActions.() -> Unit = {},
) {
    Alert(
        title = title,
        isPresented = isPresented.value,
        onDismissRequest = { isPresented.value = false },
        message = message,
        actions = actions,
    )
}

@Composable
fun Alert(
    title: String,
    isPresented: Boolean,
    onDismissRequest: () -> Unit,
    message: String? = null,
    actions: DialogActions.() -> Unit = {},
) {
    val state = rememberPresentation(isPresented)
    if (!state.isShowing()) {
        return
    }
    val dialogActions = DialogActions().apply(actions)
    val buttons: List<DialogButton> = dialogActions.buttons.ifEmpty {
        listOf(DialogButton(title = localized("OK"), role = null, action = {}))
    }
    val onButton: (DialogButton) -> Unit = { button ->
        if (state.targetState) {
            state.targetState = false
            button.action()
            onDismissRequest()
        }
    }
    val onBack: () -> Unit = {
        val cancel = buttons.firstOrNull { it.role == ButtonRole.cancel }
        if (cancel != null) {
            onButton(cancel)
        } else if (state.targetState) {
            state.targetState = false
            onDismissRequest()
        }
    }
    PresentationDialog(state = state, onBack = onBack) {
        val colors = dialogColors()
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .animateEnterExit(enter = fadeIn(tween(250)), exit = fadeOut(tween(250)))
                    .background(colors.dimming),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                AlertCard(
                    title = localized(title),
                    message = message?.let { localized(it) },
                    textFields = dialogActions.textFields,
                    buttons = buttons,
                    onButton = onButton,
                    modifier = Modifier.animateEnterExit(
                        enter = fadeIn(tween(250)) +
                            scaleIn(tween(350, easing = presentationEasing), initialScale = 1.15f),
                        exit = fadeOut(tween(200)),
                    ),
                )
            }
        }
    }
}

private fun alertButtonStyle(role: ButtonRole?): TextStyle = if (role == ButtonRole.cancel) {
    formBodyStyle.copy(fontWeight = FontWeight.SemiBold)
} else {
    formBodyStyle
}

@Composable
private fun AlertCard(
    title: String,
    message: String?,
    textFields: List<DialogTextField>,
    buttons: List<DialogButton>,
    onButton: (DialogButton) -> Unit,
    modifier: Modifier,
) {
    val palette = formPalette()
    val colors = dialogColors()
    val tint = LocalTint.current.takeOrElse { palette.accent }
    val measurer = rememberTextMeasurer()
    val maximumSideBySideWidth = with(LocalDensity.current) { 111.dp.roundToPx() }
    val sideBySide = buttons.size == 2 && buttons.all {
        measurer.measure(
            text = it.title,
            style = alertButtonStyle(it.role),
            softWrap = false,
            maxLines = 1,
        ).size.width <= maximumSideBySideWidth
    }
    val ordered = buttons.sortedBy { button ->
        when {
            button.role != ButtonRole.cancel -> 1
            sideBySide -> 0
            else -> 2
        }
    }
    val hasMessage = !message.isNullOrEmpty()
    val hasHeader = title.isNotEmpty() || hasMessage || textFields.isNotEmpty()
    Column(
        modifier = modifier
            .width(270.dp)
            .clip(RoundedCornerShape(dialogCornerRadius))
            .background(colors.material)
            .blockPointerInput(),
    ) {
        if (hasHeader) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 19.dp, bottom = 19.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (title.isNotEmpty()) {
                    Text(
                        text = title,
                        style = formBodyStyle.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.label,
                        textAlign = TextAlign.Center,
                    )
                }
                if (message != null && hasMessage) {
                    Text(
                        text = message,
                        style = formFootnoteStyle,
                        color = palette.label,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = if (title.isNotEmpty()) 2.dp else 0.dp),
                    )
                }
                if (textFields.isNotEmpty()) {
                    AlertTextFields(
                        textFields = textFields,
                        tint = tint,
                        modifier = Modifier.padding(top = if (title.isNotEmpty() || hasMessage) 16.dp else 0.dp),
                    )
                }
            }
            Separator()
        }
        if (sideBySide) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
            ) {
                ordered.forEachIndexed { index, button ->
                    if (index > 0) {
                        Separator(
                            modifier = Modifier
                                .width(hairline())
                                .fillMaxHeight(),
                        )
                    }
                    DialogButtonCell(
                        title = button.title,
                        style = alertButtonStyle(button.role),
                        color = if (button.role == ButtonRole.destructive) palette.red else tint,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        onButton(button)
                    }
                }
            }
        } else {
            ordered.forEachIndexed { index, button ->
                if (index > 0) {
                    Separator()
                }
                DialogButtonCell(
                    title = button.title,
                    style = alertButtonStyle(button.role),
                    color = if (button.role == ButtonRole.destructive) palette.red else tint,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp),
                ) {
                    onButton(button)
                }
            }
        }
    }
}

@Composable
private fun AlertTextFields(textFields: List<DialogTextField>, tint: Color, modifier: Modifier) {
    val palette = formPalette()
    val colors = dialogColors()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }
    val shape = RoundedCornerShape(7.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.field)
            .border(hairline(), palette.separator, shape),
    ) {
        textFields.forEachIndexed { index, field ->
            if (index > 0) {
                Separator()
            }
            AlertTextField(
                field = field,
                tint = tint,
                modifier = if (index == 0) Modifier.focusRequester(focusRequester) else Modifier,
            )
        }
    }
}

@Composable
private fun AlertTextField(field: DialogTextField, tint: Color, modifier: Modifier) {
    val palette = formPalette()
    var state by remember { mutableStateOf(TextFieldValue(field.text, TextRange(field.text.length))) }
    val value = if (state.text == field.text) {
        state
    } else {
        TextFieldValue(field.text, TextRange(field.text.length))
    }
    BasicTextField(
        value = value,
        onValueChange = {
            val textChanged = it.text != state.text
            state = it
            if (textChanged) {
                field.onTextChange(it.text)
            }
        },
        modifier = modifier.fillMaxWidth(),
        textStyle = formFootnoteStyle.copy(color = palette.label),
        singleLine = true,
        visualTransformation = if (field.secure) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        cursorBrush = SolidColor(tint),
        decorationBox = { innerTextField ->
            Box(modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)) {
                if (value.text.isEmpty()) {
                    Text(text = field.title, style = formFootnoteStyle, color = palette.tertiaryLabel)
                }
                innerTextField()
            }
        },
    )
}

@Composable
fun ConfirmationDialog(
    title: String,
    isPresented: MutableState<Boolean>,
    titleVisibility: Visibility = Visibility.automatic,
    message: String? = null,
    actions: DialogActions.() -> Unit,
) {
    ConfirmationDialog(
        title = title,
        isPresented = isPresented.value,
        onDismissRequest = { isPresented.value = false },
        titleVisibility = titleVisibility,
        message = message,
        actions = actions,
    )
}

@Composable
fun ConfirmationDialog(
    title: String,
    isPresented: Boolean,
    onDismissRequest: () -> Unit,
    titleVisibility: Visibility = Visibility.automatic,
    message: String? = null,
    actions: DialogActions.() -> Unit,
) {
    val state = rememberPresentation(isPresented)
    if (!state.isShowing()) {
        return
    }
    val dialogActions = DialogActions().apply(actions)
    val cancel = dialogActions.buttons.firstOrNull { it.role == ButtonRole.cancel }
        ?: DialogButton(title = localized("Cancel"), role = ButtonRole.cancel, action = {})
    val buttons = dialogActions.buttons.filter { it.role != ButtonRole.cancel }
    val onButton: (DialogButton) -> Unit = { button ->
        if (state.targetState) {
            state.targetState = false
            button.action()
            onDismissRequest()
        }
    }
    val onCancel by rememberUpdatedState(newValue = { onButton(cancel) })
    PresentationDialog(state = state, onBack = onCancel) {
        val colors = dialogColors()
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .animateEnterExit(enter = fadeIn(tween(300)), exit = fadeOut(tween(250)))
                    .background(colors.dimming)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            onCancel()
                        }
                    },
            )
            ActionSheet(
                title = if (titleVisibility == Visibility.visible) localized(title) else "",
                message = message?.let { localized(it) },
                buttons = buttons,
                cancel = cancel,
                onButton = onButton,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .animateEnterExit(
                        enter = slideInVertically(tween(400, easing = presentationEasing)) { it },
                        exit = slideOutVertically(tween(250, easing = presentationEasing)) { it },
                    )
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(8.dp)
                    .widthIn(max = 400.dp)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ActionSheet(
    title: String,
    message: String?,
    buttons: List<DialogButton>,
    cancel: DialogButton,
    onButton: (DialogButton) -> Unit,
    modifier: Modifier,
) {
    val palette = formPalette()
    val colors = dialogColors()
    val tint = LocalTint.current.takeOrElse { palette.accent }
    val hasMessage = !message.isNullOrEmpty()
    val hasHeader = title.isNotEmpty() || hasMessage
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (hasHeader || buttons.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(dialogCornerRadius))
                    .background(colors.material)
                    .blockPointerInput()
                    .verticalScroll(rememberScrollState()),
            ) {
                if (hasHeader) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (title.isNotEmpty()) {
                            Text(
                                text = title,
                                style = formFootnoteStyle.copy(fontWeight = FontWeight.SemiBold),
                                color = palette.gray,
                                textAlign = TextAlign.Center,
                            )
                        }
                        if (message != null && hasMessage) {
                            Text(
                                text = message,
                                style = formFootnoteStyle,
                                color = palette.gray,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                buttons.forEachIndexed { index, button ->
                    if (index > 0 || hasHeader) {
                        Separator()
                    }
                    DialogButtonCell(
                        title = button.title,
                        style = actionSheetButtonStyle,
                        color = if (button.role == ButtonRole.destructive) palette.red else tint,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 57.dp),
                    ) {
                        onButton(button)
                    }
                }
            }
        }
        DialogButtonCell(
            title = cancel.title,
            style = actionSheetButtonStyle.copy(fontWeight = FontWeight.SemiBold),
            color = tint,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 57.dp)
                .clip(RoundedCornerShape(dialogCornerRadius))
                .background(colors.cancel),
        ) {
            onButton(cancel)
        }
    }
}

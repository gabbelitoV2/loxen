package com.moblin.android.platform.swiftui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

@Composable
fun formPalette(): FormPalette = if (isSystemInDarkTheme()) darkFormPalette else lightFormPalette

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

@Composable
fun Sheet(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    val palette = formPalette()
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 540.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .clip(RoundedCornerShape(formCornerRadius))
                .background(palette.groupedBackground),
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

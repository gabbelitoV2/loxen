package com.moblin.android.platform.swiftui

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MultiContentMeasurePolicy
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val coordinator = remember { ListCoordinator() }
    coordinator.scrollState = scrollState
    LaunchedEffect(entry, scrollState) {
        snapshotFlow { scrollState.value > 0 }.collect {
            entry.scrolled = it
        }
    }
    LaunchedEffect(coordinator, scrollState) {
        var previous = scrollState.value
        snapshotFlow { scrollState.value }.collect { value ->
            if (value != previous) {
                previous = value
                if (coordinator.ghost == null) {
                    coordinator.closeOpenRow()
                }
            }
        }
    }
    CompositionLocalProvider(
        LocalContentColor provides palette.label,
        LocalTextStyle provides formBodyStyle,
        LocalInSection provides false,
        LocalListCoordinator provides coordinator,
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.groupedBackground)
                    .onGloballyPositioned { coordinator.formCoordinates = it }
                    .listCoordinatorInput(coordinator)
                    .verticalScroll(scrollState)
                    .padding(bottom = formSectionSpacing),
                content = content,
            )
            ReorderGhostOverlay(coordinator)
        }
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
    when (LocalButtonPresentation.current) {
        ButtonPresentation.menu -> {
            MenuDivider()
            if (header != null) {
                MenuHeader(title = localized(header))
            }
            content()
            MenuDivider()
            return
        }
        ButtonPresentation.probe, ButtonPresentation.swipe -> {
            content()
            return
        }
        ButtonPresentation.plain -> {}
    }
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
    val info = LocalRowContainerInfo.current ?: remember { FormRowInfo() }
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
    CompositionLocalProvider(
        LocalFormRowInfo provides info,
        LocalRowContainerInfo provides null,
        LocalListRowSlot provides null,
    ) {
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
    when (LocalLabelPresentation.current) {
        LabelPresentation.iconOnly -> {
            SystemImage(
                name = systemImage,
                fontSize = 22.sp,
                tint = LocalContentColor.current,
                modifier = modifier,
            )
            return
        }
        LabelPresentation.iconAboveTitle -> {
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SystemImage(name = systemImage, fontSize = 22.sp, tint = LocalContentColor.current)
                Text(text = localized(title), style = swipeCaptionStyle, maxLines = 1)
            }
            return
        }
        LabelPresentation.menu -> {
            Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = localized(title), modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                SystemImage(name = systemImage, fontSize = 20.sp, tint = LocalContentColor.current)
            }
            return
        }
        LabelPresentation.standard -> {}
    }
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
    systemImage: ((T) -> String)? = null,
    pickerStyle: PickerStyle = PickerStyle.automatic,
    onChange: (T) -> Unit,
) {
    val palette = formPalette()
    val presentation = rememberMenuPresentation()
    val selectedImage = systemImage?.invoke(selection)
    if (pickerStyle == PickerStyle.menu && !LocalInSection.current) {
        val color = LocalTint.current.takeOrElse { palette.accent }
        PlainButtonFrame(
            enabled = enabled,
            onClick = { presentation.present() },
            modifier = Modifier.menuAnchor(presentation),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectedImage != null) {
                    SystemImage(name = selectedImage, fontSize = 17.sp, tint = color)
                }
                val selectedText = text(selection)
                if (selectedText.isNotEmpty()) {
                    if (selectedImage != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(text = selectedText, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.width(4.dp))
                SystemImage(name = "chevron.up.chevron.down", fontSize = 13.sp, tint = color)
            }
        }
    } else if (LocalInSection.current && LocalFormRowInfo.current != null) {
        PlainButtonFrame(
            enabled = enabled,
            onClick = { presentation.present() },
            modifier = Modifier.menuAnchor(presentation),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (title.isNotEmpty()) {
                    Text(text = localized(title))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (selectedImage != null) {
                    SystemImage(name = selectedImage, fontSize = 17.sp, tint = palette.secondaryLabel)
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = text(selection),
                    color = palette.secondaryLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(4.dp))
                SystemImage(name = "chevron.up.chevron.down", fontSize = 15.sp, tint = palette.secondaryLabel)
            }
        }
    } else {
        FormRow(onClick = { presentation.present() }, enabled = enabled, highlight = false) {
            Text(text = localized(title), modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                modifier = Modifier.menuAnchor(presentation),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selectedImage != null) {
                    SystemImage(name = selectedImage, fontSize = 17.sp, tint = palette.secondaryLabel)
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = text(selection),
                    color = palette.secondaryLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(4.dp))
                SystemImage(name = "chevron.up.chevron.down", fontSize = 15.sp, tint = palette.secondaryLabel)
            }
        }
    }
    PullDownMenu(presentation = presentation, checkmarks = true) {
        for (option in options) {
            val image = systemImage?.invoke(option)
            MenuItemRow(
                role = null,
                enabled = true,
                checked = option == selection,
                action = {
                    if (option != selection) {
                        onChange(option)
                    }
                },
            ) {
                if (image != null) {
                    Label(title = text(option), systemImage = image)
                } else {
                    Text(text = text(option))
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

private val LocalHostSafeArea = staticCompositionLocalOf<WindowInsets?> { null }

@Composable
private fun hostSafeArea(): WindowInsets {
    val host = LocalHostSafeArea.current ?: return WindowInsets.safeDrawing
    return host.union(WindowInsets.safeDrawing)
}

@Composable
private fun PresentationDialog(
    state: MutableTransitionState<Boolean>,
    onBack: () -> Unit,
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    val hostSafeArea = LocalHostSafeArea.current ?: WindowInsets.safeDrawing
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
            window?.setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            )
            if (window != null && android.os.Build.VERSION.SDK_INT >= 30) {
                val attributes = window.attributes
                if (attributes.fitInsetsTypes != 0 ||
                    attributes.layoutInDisplayCutoutMode !=
                    android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                ) {
                    attributes.fitInsetsTypes = 0
                    attributes.layoutInDisplayCutoutMode =
                        android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    window.attributes = attributes
                }
            }
        }
        CompositionLocalProvider(LocalHostSafeArea provides hostSafeArea) {
            AnimatedVisibility(
                visibleState = state,
                enter = EnterTransition.None,
                exit = ExitTransition.None,
                content = content,
            )
        }
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
                            hostSafeArea().only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                        )
                        .padding(top = 10.dp)
                        .widthIn(max = 700.dp)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = formCornerRadius, topEnd = formCornerRadius))
                        .background(palette.groupedBackground)
                        .blockPointerInput()
                        .windowInsetsPadding(hostSafeArea().only(WindowInsetsSides.Bottom)),
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
fun FullScreenCover(isPresented: MutableState<Boolean>, content: @Composable () -> Unit) {
    FullScreenCover(
        isPresented = isPresented.value,
        onDismissRequest = { isPresented.value = false },
        content = content,
    )
}

@Composable
fun FullScreenCover(isPresented: Boolean, onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberPresentation(isPresented)
    if (!state.isShowing()) {
        return
    }
    PresentationDialog(state = state, onBack = onDismissRequest) {
        CompositionLocalProvider(LocalElevated provides false) {
            val palette = formPalette()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .animateEnterExit(
                        enter = slideInVertically(tween(450, easing = presentationEasing)) { it },
                        exit = slideOutVertically(tween(300, easing = presentationEasing)) { it },
                    )
                    .background(if (isSystemInDarkTheme()) Color.Black else Color.White)
                    .windowInsetsPadding(hostSafeArea()),
            ) {
                CompositionLocalProvider(
                    LocalNavigator provides null,
                    LocalNavigationEntry provides null,
                    LocalInSection provides false,
                    LocalFormRowInfo provides null,
                    LocalRowContainerInfo provides null,
                    LocalListRowSlot provides null,
                    LocalListCoordinator provides null,
                    LocalButtonPresentation provides ButtonPresentation.plain,
                    LocalLabelPresentation provides LabelPresentation.standard,
                    LocalContentColor provides palette.label,
                    LocalTextStyle provides formBodyStyle,
                ) {
                    content()
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
                    .windowInsetsPadding(hostSafeArea())
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
                    .windowInsetsPadding(hostSafeArea())
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

typealias IndexSet = Set<Int>

enum class HorizontalEdge {
    leading,
    trailing,
}

enum class PickerStyle {
    automatic,
    menu,
}

fun <T> MutableList<T>.move(fromOffsets: Collection<Int>, toOffset: Int) {
    val sources = fromOffsets.filter { it in indices }.distinct().sorted()
    if (sources.isEmpty()) {
        return
    }
    val moved = sources.map { this[it] }
    val destination = toOffset - sources.count { it < toOffset }
    for (index in sources.asReversed()) {
        removeAt(index)
    }
    addAll(destination.coerceIn(0, size), moved)
}

fun <T> MutableList<T>.remove(atOffsets: Collection<Int>) {
    for (index in atOffsets.distinct().sortedDescending()) {
        if (index in indices) {
            removeAt(index)
        }
    }
}

fun <T> List<T>.moving(fromOffsets: Collection<Int>, toOffset: Int): List<T> =
    toMutableList().apply { move(fromOffsets = fromOffsets, toOffset = toOffset) }

fun <T> List<T>.removing(atOffsets: Collection<Int>): List<T> =
    toMutableList().apply { remove(atOffsets = atOffsets) }

private enum class ButtonPresentation {
    plain,
    probe,
    menu,
    swipe,
}

private enum class LabelPresentation {
    standard,
    iconOnly,
    iconAboveTitle,
    menu,
}

private enum class MenuChild {
    item,
    divider,
    header,
}

private object SwipeLabelTag

private object MenuProbeTag

@PublishedApi
internal object NoDragKey

private val LocalButtonPresentation = staticCompositionLocalOf { ButtonPresentation.plain }

private val LocalLabelPresentation = staticCompositionLocalOf { LabelPresentation.standard }

private val LocalSwipeHost = staticCompositionLocalOf<SwipeHost?> { null }

private val LocalMenuHost = staticCompositionLocalOf<MenuHost?> { null }

private val LocalListRowSlot = compositionLocalOf<ListRowState?> { null }

private val LocalRowContainerInfo = compositionLocalOf<FormRowInfo?> { null }

private val LocalListCoordinator = staticCompositionLocalOf<ListCoordinator?> { null }

private val swipeButtonMinimumWidth = 74.dp
private val swipeButtonPadding = 15.dp
private val swipeTallRowHeight = 91.dp
private val swipeFullSwipeExtra = 30.dp
private val swipeFlingVelocity = 300.dp
private const val swipeFullSwipeFraction = 0.6f
private val swipeTitleStyle = TextStyle(fontSize = 17.sp, lineHeight = 22.sp)
private val swipeCaptionStyle = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
private const val longPressMillis = 500L
private val menuMinimumWidth = 250.dp
private val menuMaximumWidth = 300.dp
private val menuCornerRadius = 13.dp
private val menuGap = 8.dp
private val menuMargin = 16.dp
private val menuRowMinHeight = 44.dp
private val reorderAutoscrollZone = 56.dp
private val reorderAutoscrollSpeed = 1000.dp

private fun <T> swipeSettleSpring() = spring<T>(dampingRatio = 1f, stiffness = 400f)

private fun View.hapticLift() {
    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
}

private fun View.hapticThreshold(activate: Boolean) {
    val constant = if (Build.VERSION.SDK_INT >= 34) {
        if (activate) {
            HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE
        } else {
            HapticFeedbackConstants.GESTURE_THRESHOLD_DEACTIVATE
        }
    } else {
        HapticFeedbackConstants.CONTEXT_CLICK
    }
    performHapticFeedback(constant)
}

private fun View.hapticSelection() {
    val constant = if (Build.VERSION.SDK_INT >= 34) {
        HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
    } else {
        HapticFeedbackConstants.CLOCK_TICK
    }
    performHapticFeedback(constant)
}

private fun crossWindowBlurEnabled(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < 31) {
        return false
    }
    return context.getSystemService(WindowManager::class.java)?.isCrossWindowBlurEnabled == true
}

private fun LayoutCoordinates.boundsOnScreen(): Rect {
    if (!isAttached) {
        return Rect.Zero
    }
    return Rect(localToScreen(Offset.Zero), size.toSize())
}

private fun <T> List<T>.movingItem(from: Int, to: Int): List<T> {
    if (from !in indices) {
        return this
    }
    val list = toMutableList()
    val item = list.removeAt(from)
    list.add(to.coerceIn(0, list.size), item)
    return list
}

@Stable
class ForEachState @PublishedApi internal constructor() {
    internal var version by mutableIntStateOf(0)
    internal var onDelete: ((IndexSet) -> Unit)? = null
    internal var onMove: ((IndexSet, Int) -> Unit)? = null
    internal var deletable by mutableStateOf(false)
    internal var movable by mutableStateOf(false)
    internal var keys: List<Any?> = emptyList()
    internal var dragKey by mutableStateOf<Any?>(NoDragKey)
    internal var dragTarget by mutableIntStateOf(0)
    internal val heights = HashMap<Any?, Int>()
    internal var animatePlacementUntil = 0L
    private var pendingKeys: List<Any?>? = null
    private var pendingFrom = 0
    private var pendingTarget = 0
    private var pendingSince = 0L

    @PublishedApi
    internal fun <T> arrange(
        items: List<T>,
        id: (T) -> Any?,
        onDelete: ((IndexSet) -> Unit)?,
        onMove: ((IndexSet, Int) -> Unit)?,
    ): List<T> {
        version
        this.onDelete = onDelete
        this.onMove = onMove
        if (deletable != (onDelete != null)) {
            deletable = onDelete != null
        }
        if (movable != (onMove != null)) {
            movable = onMove != null
        }
        val keys = items.map(id)
        this.keys = keys
        val dragging = dragKey
        if (dragging !== NoDragKey) {
            val from = keys.indexOf(dragging)
            if (from >= 0) {
                return items.movingItem(from, dragTarget)
            }
        }
        val pending = pendingKeys
        if (pending != null) {
            if (pending == keys && SystemClock.uptimeMillis() - pendingSince < 500) {
                return items.movingItem(pendingFrom, pendingTarget)
            }
            pendingKeys = null
        }
        return items
    }

    internal fun bump() {
        version += 1
    }

    internal fun animatesPlacement(): Boolean =
        dragKey !== NoDragKey || SystemClock.uptimeMillis() < animatePlacementUntil

    internal fun delete(key: Any?) {
        val index = keys.indexOf(key)
        if (index >= 0) {
            onDelete?.invoke(sortedSetOf(index))
        }
        bump()
    }

    internal fun endDrag(key: Any?, target: Int) {
        val now = SystemClock.uptimeMillis()
        val from = keys.indexOf(key)
        animatePlacementUntil = now + 450
        if (from >= 0 && target != from && target in keys.indices) {
            pendingKeys = keys
            pendingFrom = from
            pendingTarget = target
            pendingSince = now
            onMove?.invoke(sortedSetOf(from), if (target > from) target + 1 else target)
        }
        dragKey = NoDragKey
        bump()
    }
}

@PublishedApi
@Composable
internal fun rememberForEachState(): ForEachState = remember { ForEachState() }

@PublishedApi
@Composable
@ReadOnlyComposable
internal fun forEachWrapsRows(): Boolean = LocalInSection.current &&
    LocalFormRowInfo.current == null &&
    LocalButtonPresentation.current == ButtonPresentation.plain

@PublishedApi
@Composable
internal fun ForEachRow(state: ForEachState, key: Any?, content: @Composable () -> Unit) {
    ListRow(forEach = state, key = key, content = content)
}

@Composable
inline fun <T> ForEach(
    items: List<T>,
    noinline id: (T) -> Any? = { it },
    noinline onDelete: ((IndexSet) -> Unit)? = null,
    noinline onMove: ((IndexSet, Int) -> Unit)? = null,
    noinline content: @Composable (T) -> Unit,
) {
    val state = rememberForEachState()
    val rows = forEachWrapsRows()
    for (item in state.arrange(items, id, onDelete, onMove)) {
        val itemKey = id(item)
        key(itemKey) {
            if (rows) {
                ForEachRow(state, itemKey) {
                    content(item)
                }
            } else {
                content(item)
            }
        }
    }
}

@Composable
fun SwipeActions(
    edge: HorizontalEdge = HorizontalEdge.trailing,
    allowsFullSwipe: Boolean = true,
    actions: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val row = LocalListRowSlot.current
    if (row == null) {
        if (forEachWrapsRows()) {
            ListRow(forEach = null, key = null) {
                SwipeActions(edge = edge, allowsFullSwipe = allowsFullSwipe, actions = actions, content = content)
            }
        } else {
            content()
        }
        return
    }
    val config = remember(edge, allowsFullSwipe, actions) {
        SwipeActionsConfig(allowsFullSwipe = allowsFullSwipe, content = actions)
    }
    SideEffect {
        if (edge == HorizontalEdge.trailing) {
            if (row.trailing !== config) {
                row.trailing = config
            }
        } else if (row.leading !== config) {
            row.leading = config
        }
    }
    DisposableEffect(row, edge) {
        onDispose {
            if (edge == HorizontalEdge.trailing) {
                row.trailing = null
            } else {
                row.leading = null
            }
        }
    }
    content()
}

@Composable
fun ContextMenu(menu: @Composable () -> Unit, content: @Composable () -> Unit) {
    val row = LocalListRowSlot.current
    if (row == null) {
        if (forEachWrapsRows()) {
            ListRow(forEach = null, key = null) {
                ContextMenu(menu = menu, content = content)
            }
        } else {
            content()
        }
        return
    }
    SideEffect {
        if (row.menu !== menu) {
            row.menu = menu
        }
    }
    DisposableEffect(row) {
        onDispose {
            row.menu = null
        }
    }
    content()
}

@Composable
fun DeleteDisabled(disabled: Boolean, content: @Composable () -> Unit) {
    val row = LocalListRowSlot.current
    if (row != null) {
        SideEffect {
            if (row.deleteDisabled != disabled) {
                row.deleteDisabled = disabled
            }
        }
        DisposableEffect(row) {
            onDispose {
                row.deleteDisabled = false
            }
        }
    }
    content()
}

@Composable
fun MoveDisabled(disabled: Boolean, content: @Composable () -> Unit) {
    val row = LocalListRowSlot.current
    if (row != null) {
        SideEffect {
            if (row.moveDisabled != disabled) {
                row.moveDisabled = disabled
            }
        }
        DisposableEffect(row) {
            onDispose {
                row.moveDisabled = false
            }
        }
    }
    content()
}

@Composable
fun Button(
    action: () -> Unit,
    role: ButtonRole? = null,
    enabled: Boolean = true,
    label: @Composable () -> Unit,
) {
    when (LocalButtonPresentation.current) {
        ButtonPresentation.probe -> MenuProbeEntry()
        ButtonPresentation.menu -> MenuItemRow(
            role = role,
            enabled = enabled,
            checked = null,
            action = action,
            label = label,
        )
        ButtonPresentation.swipe -> SwipeActionButton(role = role, action = action, label = label)
        ButtonPresentation.plain -> PlainButton(role = role, enabled = enabled, action = action, label = label)
    }
}

@Composable
fun Button(title: String, role: ButtonRole? = null, enabled: Boolean = true, action: () -> Unit) {
    Button(action = action, role = role, enabled = enabled) {
        Text(text = localized(title))
    }
}

@Composable
fun Button(
    title: String,
    systemImage: String,
    role: ButtonRole? = null,
    enabled: Boolean = true,
    action: () -> Unit,
) {
    Button(action = action, role = role, enabled = enabled) {
        Label(title = title, systemImage = systemImage)
    }
}

@Composable
fun Menu(
    title: String,
    systemImage: String? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Menu(content = content, enabled = enabled) {
        if (systemImage != null) {
            Label(title = title, systemImage = systemImage)
        } else {
            Text(text = localized(title))
        }
    }
}

@Composable
fun Menu(content: @Composable () -> Unit, enabled: Boolean = true, label: @Composable () -> Unit) {
    when (LocalButtonPresentation.current) {
        ButtonPresentation.probe -> {
            content()
            return
        }
        ButtonPresentation.menu -> {
            MenuDivider()
            content()
            MenuDivider()
            return
        }
        ButtonPresentation.swipe -> return
        ButtonPresentation.plain -> {}
    }
    val palette = formPalette()
    val presentation = rememberMenuPresentation()
    val color = LocalTint.current.takeOrElse { palette.accent }
    if (LocalInSection.current && LocalFormRowInfo.current == null) {
        FormRow(onClick = { presentation.present() }, enabled = enabled) {
            Box(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.menuAnchor(presentation)) {
                    CompositionLocalProvider(LocalContentColor provides color, LocalTint provides color) {
                        label()
                    }
                }
            }
        }
    } else {
        PlainButtonFrame(
            enabled = enabled,
            onClick = { presentation.present() },
            modifier = Modifier.menuAnchor(presentation),
        ) {
            CompositionLocalProvider(LocalContentColor provides color, LocalTint provides color) {
                label()
            }
        }
    }
    PullDownMenu(presentation = presentation, checkmarks = false, content = content)
}

@Composable
fun Divider() {
    when (LocalButtonPresentation.current) {
        ButtonPresentation.menu -> MenuDivider()
        ButtonPresentation.probe, ButtonPresentation.swipe -> {}
        ButtonPresentation.plain -> Separator()
    }
}

@Composable
private fun PlainButtonFrame(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = modifier
            .alpha(
                when {
                    !enabled -> 0.4f
                    pressed -> 0.2f
                    else -> 1f
                },
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun PlainButton(
    role: ButtonRole?,
    enabled: Boolean,
    action: () -> Unit,
    label: @Composable () -> Unit,
) {
    val palette = formPalette()
    val color = if (role == ButtonRole.destructive) {
        palette.red
    } else {
        LocalTint.current.takeOrElse { palette.accent }
    }
    if (LocalInSection.current && LocalFormRowInfo.current == null) {
        FormRow(onClick = action, enabled = enabled) {
            Box(modifier = Modifier.weight(1f)) {
                CompositionLocalProvider(LocalContentColor provides color, LocalTint provides color) {
                    label()
                }
            }
        }
    } else {
        PlainButtonFrame(enabled = enabled, onClick = action) {
            CompositionLocalProvider(LocalContentColor provides color, LocalTint provides color) {
                label()
            }
        }
    }
}

private class SwipeActionsConfig(
    val allowsFullSwipe: Boolean,
    val content: @Composable () -> Unit,
)

private class SwipeEntry(
    val role: ButtonRole?,
    val action: () -> Unit,
)

private class SwipeHost(
    val row: ListRowState,
    val edge: HorizontalEdge,
)

@Stable
private class ListCoordinator {
    var openRow: ListRowState? = null
    var scrollState: ScrollState? = null
    var formCoordinates: LayoutCoordinates? = null
    var overlayCoordinates: LayoutCoordinates? = null
    var ghost by mutableStateOf<ReorderGhost?>(null)

    fun opened(row: ListRowState) {
        val previous = openRow
        if (previous !== row) {
            previous?.close()
        }
        openRow = row
    }

    fun closeOpenRow() {
        val row = openRow ?: return
        openRow = null
        row.close()
    }
}

@Stable
private class ReorderGhost(
    val row: ListRowState,
    val layer: GraphicsLayer,
    val size: IntSize,
) {
    var topLeftRoot by mutableStateOf(Offset.Zero)
    val lift = Animatable(0f)
}

private sealed interface RowDecision {
    object Up : RowDecision

    object Cancel : RowDecision

    object LongPress : RowDecision

    class Swipe(val overSlop: Float) : RowDecision
}

@Stable
private class ListRowState(val scope: CoroutineScope, val view: View) {
    var forEach: ForEachState? = null
    var key: Any? = null
    var coordinator: ListCoordinator? = null
    var layer: GraphicsLayer? = null
    var density: Density = Density(1f)
    var elevated = false
    val offset = Animatable(0f)
    var tracking by mutableStateOf(false)
    var trackedOffset by mutableFloatStateOf(0f)
    val expansion = Animatable(0f)
    val collapse = Animatable(1f)
    val press = Animatable(1f)
    val placement = Animatable(0f)
    var trailing by mutableStateOf<SwipeActionsConfig?>(null)
    var leading by mutableStateOf<SwipeActionsConfig?>(null)
    var menu by mutableStateOf<(@Composable () -> Unit)?>(null)
    var deleteDisabled by mutableStateOf(false)
    var moveDisabled by mutableStateOf(false)
    var hidden by mutableStateOf(false)
    var rowHeight by mutableIntStateOf(0)
    var width = 0
    var trailingEntries: List<SwipeEntry> = emptyList()
    var trailingWidth = 0
    var leadingEntries: List<SwipeEntry> = emptyList()
    var leadingWidth = 0
    var menuItemCount = 0
    var busy = false
    var placedCoordinates: LayoutCoordinates? = null
    var inputCoordinates: LayoutCoordinates? = null
    private var lastPlacedY = Float.NaN
    val menuTransition = MutableTransitionState(false)
    var menuSnapshot: ImageBitmap? = null
    var menuRowScreen = Rect.Zero
    var menuTouchScreen = Offset.Zero
    val menuHost = MenuHost(view)

    val deleteActions = SwipeActionsConfig(allowsFullSwipe = true) {
        Button(title = "Delete", role = ButtonRole.destructive) {
            forEach?.delete(key)
        }
    }

    val visibleOffset: Float
        get() = if (tracking) trackedOffset else offset.value

    fun trailingActions(): SwipeActionsConfig? {
        val custom = trailing
        if (custom != null) {
            return custom
        }
        if (leading != null) {
            return null
        }
        val forEach = forEach ?: return null
        return if (forEach.deletable && !deleteDisabled) deleteActions else null
    }

    fun canSwipe(): Boolean = (trailingActions() != null && trailingWidth > 0) ||
        (leading != null && leadingWidth > 0) ||
        visibleOffset != 0f

    fun close() {
        scope.launch {
            expansion.snapTo(0f)
            offset.animateTo(0f, swipeSettleSpring())
        }
    }

    fun releasePress() {
        if (press.value != 1f || press.isRunning) {
            scope.launch {
                press.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 600f))
            }
        }
    }

    private fun rubberBand(distance: Float, dimension: Float): Float =
        (1f - 1f / (distance * 0.55f / dimension + 1f)) * dimension

    private fun setArmed(armed: Boolean) {
        view.hapticThreshold(armed)
        scope.launch {
            expansion.animateTo(if (armed) 1f else 0f, spring(dampingRatio = 0.85f, stiffness = 500f))
        }
    }

    fun track(raw: Float, armed: Boolean): Boolean {
        val config = if (raw < 0f) trailingActions() else leading
        val total = (if (raw < 0f) trailingWidth else leadingWidth).toFloat()
        if (config == null || total <= 0f || raw == 0f) {
            trackedOffset = 0f
            if (armed) {
                setArmed(false)
            }
            return false
        }
        val reveal = abs(raw)
        val rowWidth = width.toFloat().coerceAtLeast(total)
        val extra = with(density) { swipeFullSwipeExtra.toPx() }
        val threshold = max(total + extra, rowWidth * swipeFullSwipeFraction)
        val nowArmed = config.allowsFullSwipe && reveal > threshold
        val visible = when {
            config.allowsFullSwipe -> min(reveal, rowWidth)
            reveal <= total -> reveal
            else -> total + rubberBand(reveal - total, rowWidth)
        }
        trackedOffset = sign(raw) * visible
        if (nowArmed != armed) {
            setArmed(nowArmed)
        }
        return nowArmed
    }

    fun release(velocity: Float, armed: Boolean) {
        val current = trackedOffset
        val edge = if (current < 0f) HorizontalEdge.trailing else HorizontalEdge.leading
        if (armed) {
            val entry = (if (current < 0f) trailingEntries else leadingEntries).firstOrNull()
            scope.launch {
                offset.snapTo(current)
                tracking = false
                if (entry != null) {
                    perform(entry, edge)
                } else {
                    expansion.snapTo(0f)
                    offset.animateTo(0f, swipeSettleSpring())
                }
            }
            return
        }
        val total = (if (current < 0f) trailingWidth else leadingWidth).toFloat()
        val fling = with(density) { swipeFlingVelocity.toPx() }
        val open = total > 0f && when {
            current < 0f -> velocity < -fling || (velocity <= fling && -current > total / 2f)
            current > 0f -> velocity > fling || (velocity >= -fling && current > total / 2f)
            else -> false
        }
        val target = if (open) sign(current) * total else 0f
        val coordinator = coordinator
        if (open) {
            coordinator?.opened(this)
        } else if (coordinator?.openRow === this) {
            coordinator.openRow = null
        }
        scope.launch {
            offset.snapTo(current)
            tracking = false
            if (!open) {
                expansion.snapTo(0f)
            }
            offset.animateTo(target, swipeSettleSpring(), initialVelocity = velocity)
        }
    }

    fun perform(entry: SwipeEntry, edge: HorizontalEdge) {
        if (busy) {
            return
        }
        val coordinator = coordinator
        if (coordinator?.openRow === this) {
            coordinator.openRow = null
        }
        if (entry.role != ButtonRole.destructive) {
            close()
            entry.action()
            forEach?.bump()
            return
        }
        busy = true
        scope.launch {
            val direction = if (edge == HorizontalEdge.trailing) -1f else 1f
            offset.snapTo(visibleOffset)
            tracking = false
            launch {
                expansion.animateTo(1f, tween(200))
            }
            offset.animateTo(direction * width, tween(250, easing = FastOutSlowInEasing))
            collapse.animateTo(0f, tween(250, easing = FastOutSlowInEasing))
            entry.action()
            forEach?.bump()
            withFrameNanos {}
            withFrameNanos {}
            offset.snapTo(0f)
            expansion.snapTo(0f)
            collapse.animateTo(1f, tween(250, easing = FastOutSlowInEasing))
            busy = false
        }
    }

    fun placed(coordinates: LayoutCoordinates) {
        placedCoordinates = coordinates
        val y = coordinates.positionInParent().y
        val previous = lastPlacedY
        lastPlacedY = y
        val forEach = forEach ?: return
        if (previous.isNaN() || previous == y || hidden || !forEach.animatesPlacement()) {
            return
        }
        val start = previous - y + placement.value
        scope.launch {
            placement.snapTo(start)
            placement.animateTo(0f, swipeSettleSpring())
        }
    }

    fun presentMenu(touch: Offset) {
        val layer = layer ?: return
        val coordinates = placedCoordinates ?: return
        if (!coordinates.isAttached) {
            return
        }
        view.hapticLift()
        menuTouchScreen = inputCoordinates?.takeIf { it.isAttached }?.localToScreen(touch) ?: Offset.Zero
        menuRowScreen = coordinates.boundsOnScreen()
        menuHost.reset()
        menuHost.onSelect = { action ->
            dismissMenu()
            action()
            forEach?.bump()
        }
        releasePress()
        scope.launch {
            menuSnapshot = try {
                layer.toImageBitmap()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            hidden = true
            menuTransition.targetState = true
        }
    }

    fun dismissMenu() {
        menuHost.reset()
        menuTransition.targetState = false
    }

    fun dispose() {
        val coordinator = coordinator
        if (coordinator != null) {
            if (coordinator.openRow === this) {
                coordinator.openRow = null
            }
            if (coordinator.ghost?.row === this) {
                coordinator.ghost = null
            }
        }
        val forEach = forEach
        if (forEach != null && forEach.dragKey !== NoDragKey && forEach.dragKey == key) {
            forEach.dragKey = NoDragKey
        }
    }
}

private class ContentPlacement(
    val placeable: Placeable,
    val x: Int,
    val y: Int,
)

private fun interpolate(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction

private class ListRowMeasurePolicy(val row: ListRowState) : MultiContentMeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<List<Measurable>>,
        constraints: Constraints,
    ): MeasureResult {
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth
        val padding = formRowPadding.roundToPx()
        val minHeight = formRowMinHeight.roundToPx()
        val verticalPadding = formRowVerticalPadding.roundToPx()
        val content = ArrayList<ContentPlacement>()
        var y = 0
        for (measurable in measurables[0]) {
            if (measurable.layoutId is FormRowInfo) {
                val placeable = measurable.measure(Constraints(minWidth = width, maxWidth = width))
                content.add(ContentPlacement(placeable, 0, y))
                y += placeable.height
            } else {
                val placeable = measurable.measure(Constraints(maxWidth = (width - 2 * padding).coerceAtLeast(0)))
                if (placeable.height > 0) {
                    val height = max(minHeight, placeable.height + 2 * verticalPadding)
                    content.add(ContentPlacement(placeable, padding, y + (height - placeable.height) / 2))
                    y += height
                }
            }
        }
        val rowHeight = y
        row.width = width
        if (row.rowHeight != rowHeight) {
            row.rowHeight = rowHeight
        }
        row.forEach?.heights?.put(row.key, rowHeight)
        row.menuItemCount = measurables[3].count { it.layoutId === MenuProbeTag }
        val offset = row.visibleOffset
        val swipe = ArrayList<ContentPlacement>()
        measureSwipe(measurables[1], HorizontalEdge.trailing, rowHeight, width, offset, swipe)
        measureSwipe(measurables[2], HorizontalEdge.leading, rowHeight, width, offset, swipe)
        val height = (rowHeight * row.collapse.value).roundToInt()
        return layout(width, height) {
            val x = offset.roundToInt()
            for (placement in content) {
                placement.placeable.place(placement.x + x, placement.y)
            }
            for (placement in swipe) {
                placement.placeable.place(placement.x, placement.y)
            }
        }
    }

    private fun MeasureScope.measureSwipe(
        measurables: List<Measurable>,
        edge: HorizontalEdge,
        rowHeight: Int,
        width: Int,
        offset: Float,
        placements: MutableList<ContentPlacement>,
    ) {
        val entries = ArrayList<SwipeEntry>()
        val backgrounds = ArrayList<Measurable>()
        val labels = ArrayList<Measurable>()
        var index = 0
        while (index < measurables.size) {
            val entry = measurables[index].layoutId as? SwipeEntry
            if (entry != null && index + 1 < measurables.size && measurables[index + 1].layoutId === SwipeLabelTag) {
                entries.add(entry)
                backgrounds.add(measurables[index])
                labels.add(measurables[index + 1])
                index += 2
            } else {
                index += 1
            }
        }
        val minimumWidth = swipeButtonMinimumWidth.roundToPx()
        val buttonPadding = swipeButtonPadding.roundToPx()
        val widths = labels.map { max(minimumWidth, it.maxIntrinsicWidth(rowHeight) + 2 * buttonPadding) }
        val total = widths.sum()
        if (edge == HorizontalEdge.trailing) {
            row.trailingEntries = entries
            row.trailingWidth = total
        } else {
            row.leadingEntries = entries
            row.leadingWidth = total
        }
        val revealed = if (edge == HorizontalEdge.trailing) offset < 0f else offset > 0f
        if (!revealed || entries.isEmpty() || total == 0 || rowHeight == 0) {
            return
        }
        val reveal = abs(offset)
        val scale = reveal / total
        val expansion = row.expansion.value
        val prefix = FloatArray(widths.size + 1)
        for (i in widths.indices) {
            prefix[i + 1] = prefix[i] + widths[i]
        }
        for (i in widths.indices.reversed()) {
            val natural = widths[i].toFloat()
            val regionWidth = natural * scale
            val backgroundStart: Float
            val backgroundWidth: Float
            val labelStart: Float
            val labelWidth: Float
            if (edge == HorizontalEdge.trailing) {
                val right = width - scale * prefix[i]
                val left = right - regionWidth
                if (i == 0) {
                    val expandedLeft = interpolate(left, width - reveal, expansion)
                    backgroundStart = expandedLeft
                    labelStart = expandedLeft
                    labelWidth = interpolate(regionWidth, natural, expansion)
                } else {
                    backgroundStart = left
                    labelStart = left
                    labelWidth = regionWidth
                }
                backgroundWidth = width - backgroundStart
            } else {
                val left = scale * prefix[i]
                val right = left + regionWidth
                val backgroundEnd = if (i == 0) interpolate(right, reveal, expansion) else right
                backgroundStart = 0f
                backgroundWidth = backgroundEnd
                if (i == 0) {
                    labelWidth = interpolate(regionWidth, natural, expansion)
                    labelStart = backgroundEnd - labelWidth
                } else {
                    labelStart = left
                    labelWidth = regionWidth
                }
            }
            val background = backgrounds[i].measure(
                Constraints.fixed(backgroundWidth.roundToInt().coerceAtLeast(0), rowHeight),
            )
            val label = labels[i].measure(Constraints.fixed(labelWidth.roundToInt().coerceAtLeast(0), rowHeight))
            placements.add(ContentPlacement(background, backgroundStart.roundToInt(), 0))
            placements.add(ContentPlacement(label, labelStart.roundToInt(), 0))
        }
    }
}

@Composable
private fun ListRow(forEach: ForEachState?, key: Any?, content: @Composable () -> Unit) {
    val coordinator = LocalListCoordinator.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val row = remember { ListRowState(scope, view) }
    val layer = rememberGraphicsLayer()
    val info = remember { FormRowInfo() }
    row.forEach = forEach
    row.key = key
    row.coordinator = coordinator
    row.layer = layer
    row.density = LocalDensity.current
    row.elevated = LocalElevated.current
    val trailing = row.trailingActions()
    val leading = row.leading
    val menu = row.menu
    DisposableEffect(row) {
        onDispose {
            row.dispose()
        }
    }
    val contentSlot: @Composable () -> Unit = {
        CompositionLocalProvider(LocalListRowSlot provides row, LocalRowContainerInfo provides info) {
            content()
        }
    }
    val trailingSlot: @Composable () -> Unit = {
        if (trailing != null) {
            SwipeActionsContent(row = row, edge = HorizontalEdge.trailing, config = trailing)
        }
    }
    val leadingSlot: @Composable () -> Unit = {
        if (leading != null) {
            SwipeActionsContent(row = row, edge = HorizontalEdge.leading, config = leading)
        }
    }
    val probeSlot: @Composable () -> Unit = {
        if (menu != null) {
            MenuProbe(menu = menu)
        }
    }
    Layout(
        contents = listOf(contentSlot, trailingSlot, leadingSlot, probeSlot),
        modifier = Modifier
            .layoutId(info)
            .onPlaced { row.placed(it) }
            .graphicsLayer {
                translationY = row.placement.value
                val scale = row.press.value
                scaleX = scale
                scaleY = scale
                clip = true
            }
            .onGloballyPositioned { row.inputCoordinates = it }
            .drawWithContent {
                layer.record {
                    this@drawWithContent.drawContent()
                }
                if (!row.hidden) {
                    drawLayer(layer)
                }
            }
            .pointerInput(row) {
                listRowGestures(row)
            },
        measurePolicy = remember(row) { ListRowMeasurePolicy(row) },
    )
    ContextMenuPresentation(row = row)
}

@Composable
private fun SwipeActionsContent(row: ListRowState, edge: HorizontalEdge, config: SwipeActionsConfig) {
    val host = remember(row, edge) { SwipeHost(row, edge) }
    val tall = with(LocalDensity.current) { row.rowHeight >= swipeTallRowHeight.roundToPx() }
    CompositionLocalProvider(
        LocalButtonPresentation provides ButtonPresentation.swipe,
        LocalSwipeHost provides host,
        LocalLabelPresentation provides if (tall) LabelPresentation.iconAboveTitle else LabelPresentation.iconOnly,
        LocalInSection provides false,
    ) {
        config.content()
    }
}

@Composable
private fun SwipeActionButton(role: ButtonRole?, action: () -> Unit, label: @Composable () -> Unit) {
    val host = LocalSwipeHost.current ?: return
    val palette = formPalette()
    val tint = LocalTint.current
    val color = when {
        tint.isSpecified -> tint
        role == ButtonRole.destructive -> palette.red
        else -> palette.gray
    }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val entry = SwipeEntry(role = role, action = action)
    Box(
        modifier = Modifier
            .layoutId(entry)
            .background(color)
            .drawWithContent {
                drawContent()
                if (pressed) {
                    drawRect(Color.Black.copy(alpha = 0.15f))
                }
            }
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Button) {
                host.row.perform(entry, host.edge)
            },
    )
    Box(
        modifier = Modifier
            .layoutId(SwipeLabelTag)
            .clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides Color.White,
            LocalTint provides Color.White,
            LocalTextStyle provides swipeTitleStyle,
        ) {
            label()
        }
    }
}

@Composable
private fun MenuProbe(menu: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalButtonPresentation provides ButtonPresentation.probe,
        LocalInSection provides false,
    ) {
        menu()
    }
}

@Composable
private fun MenuProbeEntry() {
    Layout(modifier = Modifier.layoutId(MenuProbeTag)) { _, _ ->
        layout(0, 0) {}
    }
}

private suspend fun AwaitPointerEventScope.awaitRowDecision(
    pointer: PointerId,
    slop: Float,
    canSwipe: Boolean,
): RowDecision {
    var total = Offset.Zero
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == pointer } ?: return RowDecision.Cancel
        if (change.changedToUpIgnoreConsumed()) {
            return RowDecision.Up
        }
        if (change.isConsumed) {
            return RowDecision.Cancel
        }
        total += change.positionChange()
        val dx = abs(total.x)
        val dy = abs(total.y)
        if (canSwipe && dx > slop && dx > dy) {
            change.consume()
            return RowDecision.Swipe(total.x - sign(total.x) * slop)
        }
        if (dx > slop || dy > slop) {
            return RowDecision.Cancel
        }
    }
}

private suspend fun PointerInputScope.listRowGestures(row: ListRowState) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (row.busy || row.menuTransition.isShowing()) {
            return@awaitEachGesture
        }
        val menuAvailable = row.menu != null && row.menuItemCount > 0
        val forEach = row.forEach
        val canMove = forEach != null &&
            forEach.movable &&
            !row.moveDisabled &&
            forEach.keys.size > 1 &&
            row.coordinator != null
        val canSwipe = row.canSwipe()
        if (!menuAvailable && !canMove && !canSwipe) {
            return@awaitEachGesture
        }
        val slop = viewConfiguration.touchSlop
        val pressJob = if (menuAvailable) {
            row.scope.launch {
                delay(150)
                row.press.animateTo(0.97f, tween(350, easing = LinearEasing))
            }
        } else {
            null
        }
        val decision = if (menuAvailable || canMove) {
            withTimeoutOrNull(longPressMillis) {
                awaitRowDecision(down.id, slop, canSwipe)
            } ?: RowDecision.LongPress
        } else {
            awaitRowDecision(down.id, slop, canSwipe)
        }
        pressJob?.cancel()
        when (decision) {
            is RowDecision.Swipe -> {
                row.releasePress()
                swipeLoop(row, down.id, decision.overSlop)
            }
            RowDecision.LongPress -> if (menuAvailable) {
                row.presentMenu(down.position)
                contextMenuLoop(row, down.id)
            } else {
                row.releasePress()
                reorderLoop(row, down.id, down.position)
            }
            else -> row.releasePress()
        }
    }
}

private suspend fun AwaitPointerEventScope.swipeLoop(row: ListRowState, pointer: PointerId, overSlop: Float) {
    val coordinator = row.coordinator
    if (coordinator != null && coordinator.openRow !== row) {
        coordinator.closeOpenRow()
    }
    val start = row.visibleOffset
    row.trackedOffset = start
    row.tracking = true
    var raw = start + overSlop
    var armed = row.track(raw, false)
    val tracker = VelocityTracker()
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == pointer } ?: break
        tracker.addPosition(change.uptimeMillis, change.position)
        if (change.changedToUpIgnoreConsumed()) {
            change.consume()
            break
        }
        raw += change.positionChangeIgnoreConsumed().x
        change.consume()
        armed = row.track(raw, armed)
    }
    row.release(tracker.calculateVelocity().x, armed)
}

private suspend fun AwaitPointerEventScope.contextMenuLoop(row: ListRowState, pointer: PointerId) {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        val change = event.changes.firstOrNull { it.id == pointer } ?: break
        change.consume()
        val coordinates = row.inputCoordinates
        if (coordinates != null && coordinates.isAttached) {
            row.menuHost.track(coordinates.localToScreen(change.position), moved = true)
        }
        if (!change.pressed) {
            row.menuHost.release()
            break
        }
    }
}

private class ReorderSession(
    val row: ListRowState,
    val forEach: ForEachState,
    val coordinator: ListCoordinator,
    val ghost: ReorderGhost,
    val grab: Offset,
    val startBlockTop: Float,
    val startScroll: Int,
) {
    var finger = Offset.Zero

    fun update() {
        val topLeft = finger - grab
        ghost.topLeftRoot = topLeft
        val key = row.key
        val scroll = coordinator.scrollState?.value ?: startScroll
        val blockTop = startBlockTop - (scroll - startScroll)
        val heights = forEach.heights
        val draggedHeight = (heights[key] ?: ghost.size.height).toFloat()
        val center = topLeft.y + draggedHeight / 2f - blockTop
        val others = forEach.keys.filter { it != key }
        var target = forEach.dragTarget.coerceIn(0, others.size)
        var gapTop = 0f
        for (index in 0 until target) {
            gapTop += heights[others[index]] ?: 0
        }
        while (target > 0) {
            val height = (heights[others[target - 1]] ?: 0).toFloat()
            if (center < gapTop - height / 2f) {
                target -= 1
                gapTop -= height
            } else {
                break
            }
        }
        while (target < others.size) {
            val height = (heights[others[target]] ?: 0).toFloat()
            if (center > gapTop + draggedHeight + height / 2f) {
                gapTop += height
                target += 1
            } else {
                break
            }
        }
        if (target != forEach.dragTarget) {
            forEach.dragTarget = target
        }
    }

    suspend fun autoscroll(density: Density) {
        val zone = with(density) { reorderAutoscrollZone.toPx() }
        val maximumSpeed = with(density) { reorderAutoscrollSpeed.toPx() }
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val seconds = (now - last) / 1_000_000_000f
            last = now
            val form = coordinator.formCoordinates ?: continue
            val scrollState = coordinator.scrollState ?: continue
            if (!form.isAttached) {
                continue
            }
            val top = form.positionInRoot().y
            val bottom = top + form.size.height
            val y = finger.y
            val speed = when {
                y < top + zone -> -maximumSpeed * ((top + zone - y) / zone).coerceIn(0f, 1f)
                y > bottom - zone -> maximumSpeed * ((y - (bottom - zone)) / zone).coerceIn(0f, 1f)
                else -> 0f
            }
            if (speed != 0f && scrollState.dispatchRawDelta(speed * seconds) != 0f) {
                update()
            }
        }
    }

    fun drop() {
        val target = forEach.dragTarget
        row.scope.launch {
            try {
                withFrameNanos {}
                val coordinates = row.placedCoordinates
                val destination = if (coordinates != null && coordinates.isAttached) {
                    coordinates.positionInRoot()
                } else {
                    ghost.topLeftRoot
                }
                val position = Animatable(ghost.topLeftRoot, Offset.VectorConverter)
                val lift = launch {
                    ghost.lift.animateTo(0f, tween(250))
                }
                position.animateTo(destination, spring(dampingRatio = 1f, stiffness = 500f)) {
                    ghost.topLeftRoot = value
                }
                lift.join()
                forEach.endDrag(row.key, target)
            } finally {
                if (forEach.dragKey !== NoDragKey && forEach.dragKey == row.key) {
                    forEach.dragKey = NoDragKey
                }
                row.hidden = false
                if (coordinator.ghost === ghost) {
                    coordinator.ghost = null
                }
            }
            delay(600)
            forEach.bump()
        }
    }
}

private suspend fun AwaitPointerEventScope.reorderLoop(row: ListRowState, pointer: PointerId, grab: Offset) {
    val forEach = row.forEach ?: return
    val coordinator = row.coordinator ?: return
    val layer = row.layer ?: return
    val coordinates = row.inputCoordinates ?: return
    if (!coordinates.isAttached) {
        return
    }
    val from = forEach.keys.indexOf(row.key)
    if (from < 0) {
        return
    }
    coordinator.closeOpenRow()
    row.view.hapticLift()
    val topLeft = coordinates.positionInRoot()
    var blockTop = topLeft.y
    for (index in 0 until from) {
        blockTop -= forEach.heights[forEach.keys[index]] ?: 0
    }
    val ghost = ReorderGhost(row = row, layer = layer, size = coordinates.size)
    ghost.topLeftRoot = topLeft
    val session = ReorderSession(
        row = row,
        forEach = forEach,
        coordinator = coordinator,
        ghost = ghost,
        grab = grab,
        startBlockTop = blockTop,
        startScroll = coordinator.scrollState?.value ?: 0,
    )
    session.finger = topLeft + grab
    coordinator.ghost = ghost
    forEach.dragTarget = from
    forEach.dragKey = row.key
    row.hidden = true
    row.scope.launch {
        ghost.lift.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 400f))
    }
    val sessionDensity = Density(density, fontScale)
    val autoscroll = row.scope.launch {
        session.autoscroll(sessionDensity)
    }
    try {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == pointer } ?: break
            change.consume()
            if (!change.pressed) {
                break
            }
            val current = row.inputCoordinates
            if (current != null && current.isAttached) {
                session.finger = current.localToRoot(change.position)
                session.update()
            }
        }
    } finally {
        autoscroll.cancel()
        session.drop()
    }
}

private fun Modifier.listCoordinatorInput(coordinator: ListCoordinator): Modifier = pointerInput(coordinator) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val open = coordinator.openRow ?: return@awaitEachGesture
        val form = coordinator.formCoordinates
        val rowCoordinates = open.inputCoordinates
        if (form != null && rowCoordinates != null && form.isAttached && rowCoordinates.isAttached) {
            val local = rowCoordinates.localPositionOf(form, down.position)
            val offset = open.visibleOffset
            val size = rowCoordinates.size
            val inside = local.y >= 0f && local.y <= size.height && when {
                offset < 0f -> local.x >= size.width + offset && local.x <= size.width
                offset > 0f -> local.x >= 0f && local.x <= offset
                else -> false
            }
            if (inside) {
                return@awaitEachGesture
            }
        }
        down.consume()
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) {
                change.consume()
                break
            }
        }
        if (!open.tracking && coordinator.openRow === open) {
            coordinator.closeOpenRow()
        }
    }
}

@Composable
private fun ReorderGhostOverlay(coordinator: ListCoordinator) {
    val palette = formPalette()
    val ghost = coordinator.ghost
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinator.overlayCoordinates = it },
    ) {
        if (ghost != null) {
            val density = LocalDensity.current
            val shape = RoundedCornerShape(formCornerRadius)
            Box(
                modifier = Modifier
                    .offset {
                        val overlay = coordinator.overlayCoordinates
                        if (overlay != null && overlay.isAttached) {
                            overlay.localPositionOf(overlay.findRootCoordinates(), ghost.topLeftRoot).round()
                        } else {
                            IntOffset.Zero
                        }
                    }
                    .size(
                        width = with(density) { ghost.size.width.toDp() },
                        height = with(density) { ghost.size.height.toDp() },
                    )
                    .graphicsLayer {
                        val lift = ghost.lift.value
                        val scale = 1f + 0.03f * lift
                        scaleX = scale
                        scaleY = scale
                        shadowElevation = 16.dp.toPx() * lift.coerceIn(0f, 1f)
                        ambientShadowColor = Color.Black.copy(alpha = 0.3f)
                        spotShadowColor = Color.Black.copy(alpha = 0.3f)
                        this.shape = shape
                        clip = true
                    }
                    .drawBehind {
                        drawRect(palette.cell)
                        if (!ghost.layer.isReleased) {
                            drawLayer(ghost.layer)
                        }
                    },
            )
        }
    }
}

private class MenuItemHandle {
    var action: () -> Unit = {}
    var enabled = true
    var coordinates: LayoutCoordinates? = null

    fun contains(screen: Offset): Boolean {
        val coordinates = coordinates ?: return false
        return coordinates.isAttached && coordinates.boundsOnScreen().contains(screen)
    }
}

@Stable
private class MenuHost(val view: View) {
    val items = ArrayList<MenuItemHandle>()
    var highlighted by mutableStateOf<MenuItemHandle?>(null)
    var checkmarks = false
    var panelCoordinates: LayoutCoordinates? = null
    var onSelect: (() -> Unit) -> Unit = {}

    fun track(screen: Offset, moved: Boolean) {
        val hit = if (screen.isSpecified) {
            items.firstOrNull { it.enabled && it.contains(screen) }
        } else {
            null
        }
        if (hit !== highlighted) {
            highlighted = hit
            if (moved && hit != null) {
                view.hapticSelection()
            }
        }
    }

    fun release() {
        val hit = highlighted ?: return
        highlighted = null
        onSelect(hit.action)
    }

    fun reset() {
        highlighted = null
    }
}

@Stable
private class MenuPresentation(view: View) {
    val transition = MutableTransitionState(false)
    var anchor by mutableStateOf(Rect.Zero)
    var anchorCoordinates: LayoutCoordinates? = null
    val host = MenuHost(view)

    fun present() {
        val coordinates = anchorCoordinates
        if (coordinates != null && coordinates.isAttached) {
            anchor = coordinates.boundsOnScreen()
        }
        host.reset()
        host.onSelect = { action ->
            dismiss()
            action()
        }
        transition.targetState = true
    }

    fun dismiss() {
        host.reset()
        transition.targetState = false
    }
}

@Composable
private fun rememberMenuPresentation(): MenuPresentation {
    val view = LocalView.current
    return remember(view) { MenuPresentation(view) }
}

private fun Modifier.menuAnchor(presentation: MenuPresentation): Modifier =
    onGloballyPositioned { presentation.anchorCoordinates = it }

private fun Modifier.menuTracking(host: MenuHost): Modifier = pointerInput(host) {
    awaitEachGesture {
        val down = awaitFirstDown()
        val coordinates = host.panelCoordinates
        val toScreen: (Offset) -> Offset = { position ->
            if (coordinates != null && coordinates.isAttached) {
                coordinates.localToScreen(position)
            } else {
                Offset.Unspecified
            }
        }
        host.track(toScreen(down.position), moved = false)
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (change.changedToUpIgnoreConsumed()) {
                host.release()
                break
            }
            if (change.isConsumed) {
                host.reset()
                break
            }
            host.track(toScreen(change.position), moved = true)
        }
    }
}

@Composable
private fun MenuDivider() {
    val color = if (isSystemInDarkTheme()) Color(0x4D000000) else Color(0x14000000)
    Box(
        modifier = Modifier
            .layoutId(MenuChild.divider)
            .fillMaxWidth()
            .height(8.dp)
            .background(color),
    )
}

@Composable
private fun MenuHeader(title: String) {
    Text(
        text = title,
        style = formFootnoteStyle,
        color = formPalette().secondaryLabel,
        modifier = Modifier
            .layoutId(MenuChild.header)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 6.dp),
    )
}

@Composable
private fun MenuItemRow(
    role: ButtonRole?,
    enabled: Boolean,
    checked: Boolean?,
    action: () -> Unit,
    label: @Composable () -> Unit,
) {
    val host = LocalMenuHost.current ?: return
    val palette = formPalette()
    val colors = dialogColors()
    val handle = remember { MenuItemHandle() }
    handle.action = action
    handle.enabled = enabled
    DisposableEffect(host, handle) {
        host.items.add(handle)
        onDispose {
            host.items.remove(handle)
        }
    }
    val color = when {
        !enabled -> palette.tertiaryLabel
        role == ButtonRole.destructive -> palette.red
        else -> palette.label
    }
    val checkmarks = host.checkmarks
    Row(
        modifier = Modifier
            .layoutId(MenuChild.item)
            .fillMaxWidth()
            .heightIn(min = menuRowMinHeight)
            .background(if (host.highlighted === handle) colors.pressed else Color.Transparent)
            .onGloballyPositioned { handle.coordinates = it }
            .padding(start = if (checkmarks) 12.dp else 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (checkmarks) {
            Box(modifier = Modifier.width(28.dp), contentAlignment = Alignment.CenterStart) {
                if (checked == true) {
                    SystemImage(name = "checkmark", fontSize = 17.sp, tint = color)
                }
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 11.dp),
        ) {
            CompositionLocalProvider(
                LocalContentColor provides color,
                LocalTint provides color,
                LocalLabelPresentation provides LabelPresentation.menu,
                LocalTextStyle provides formBodyStyle,
            ) {
                label()
            }
        }
    }
}

private class MenuListMeasurePolicy(val separators: MutableState<IntArray>) : MeasurePolicy {
    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        val contentWidth = measurables
            .filter { it.layoutId != MenuChild.divider }
            .maxOfOrNull { it.maxIntrinsicWidth(Constraints.Infinity) } ?: 0
        val width = if (constraints.hasBoundedWidth) {
            contentWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        } else {
            max(contentWidth, constraints.minWidth)
        }
        val fixed = Constraints(minWidth = width, maxWidth = width)
        val placements = ArrayList<ContentPlacement>()
        val lines = ArrayList<Int>()
        var y = 0
        var previous: MenuChild? = null
        var pendingDivider: Measurable? = null
        for (measurable in measurables) {
            val kind = measurable.layoutId as? MenuChild ?: MenuChild.item
            if (kind == MenuChild.divider) {
                if (previous != null) {
                    pendingDivider = measurable
                }
                continue
            }
            val placeable = measurable.measure(fixed)
            if (placeable.height == 0) {
                continue
            }
            val divider = pendingDivider
            if (divider != null) {
                val dividerPlaceable = divider.measure(fixed)
                placements.add(ContentPlacement(dividerPlaceable, 0, y))
                y += dividerPlaceable.height
                pendingDivider = null
            } else if (previous == MenuChild.item && kind == MenuChild.item) {
                lines.add(y)
            }
            placements.add(ContentPlacement(placeable, 0, y))
            y += placeable.height
            previous = kind
        }
        val array = lines.toIntArray()
        if (!array.contentEquals(separators.value)) {
            separators.value = array
        }
        return layout(width, y) {
            for (placement in placements) {
                placement.placeable.place(placement.x, placement.y)
            }
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int = measurables.maxOfOrNull { it.minIntrinsicWidth(height) } ?: 0

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int = measurables.maxOfOrNull { it.maxIntrinsicWidth(height) } ?: 0

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int = measurables.sumOf { it.minIntrinsicHeight(width) }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int = measurables.sumOf { it.maxIntrinsicHeight(width) }
}

@Composable
private fun MenuList(modifier: Modifier, content: @Composable () -> Unit) {
    val palette = formPalette()
    val separators = remember { mutableStateOf(IntArray(0)) }
    val hairline = hairline()
    Layout(
        content = content,
        modifier = modifier.drawWithContent {
            drawContent()
            val thickness = hairline.toPx()
            for (y in separators.value) {
                drawRect(
                    color = palette.separator,
                    topLeft = Offset(0f, y.toFloat()),
                    size = Size(size.width, thickness),
                )
            }
        },
        measurePolicy = remember(separators) { MenuListMeasurePolicy(separators) },
    )
}

@Composable
private fun MenuPanel(host: MenuHost, modifier: Modifier, content: @Composable () -> Unit) {
    val palette = formPalette()
    val scroll = rememberScrollState()
    val shape = RoundedCornerShape(menuCornerRadius)
    CompositionLocalProvider(
        LocalButtonPresentation provides ButtonPresentation.menu,
        LocalMenuHost provides host,
        LocalLabelPresentation provides LabelPresentation.standard,
        LocalInSection provides false,
        LocalListRowSlot provides null,
        LocalContentColor provides palette.label,
        LocalTextStyle provides formBodyStyle,
        LocalTint provides Color.Unspecified,
    ) {
        MenuList(
            modifier = modifier
                .shadow(
                    elevation = 24.dp,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = 0.2f),
                    spotColor = Color.Black.copy(alpha = 0.2f),
                )
                .clip(shape)
                .background(palette.menu)
                .onGloballyPositioned { host.panelCoordinates = it }
                .menuTracking(host)
                .verticalScroll(scroll, enabled = scroll.maxValue > 0),
            content = content,
        )
    }
}

@Composable
private fun PullDownMenu(
    presentation: MenuPresentation,
    checkmarks: Boolean,
    content: @Composable () -> Unit,
) {
    val state = presentation.transition
    if (!state.isShowing()) {
        return
    }
    presentation.host.checkmarks = checkmarks
    PresentationDialog(state = state, onBack = { presentation.dismiss() }) {
        val progress = transition.animateFloat(
            transitionSpec = {
                if (targetState == EnterExitState.Visible) {
                    spring(dampingRatio = 0.8f, stiffness = 350f)
                } else {
                    tween(180)
                }
            },
            label = "PullDownMenu",
        ) {
            if (it == EnterExitState.Visible) 1f else 0f
        }
        var origin by remember { mutableStateOf<Offset?>(null) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { origin = it.localToScreen(Offset.Zero) },
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(presentation) {
                        awaitEachGesture {
                            awaitFirstDown()
                            presentation.dismiss()
                        }
                    },
            )
            val screenOrigin = origin
            if (screenOrigin != null && screenOrigin.isSpecified) {
                PullDownMenuLayout(
                    presentation = presentation,
                    origin = screenOrigin,
                    progress = progress,
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun PullDownMenuLayout(
    presentation: MenuPresentation,
    origin: Offset,
    progress: State<Float>,
    content: @Composable () -> Unit,
) {
    val insets = hostSafeArea()
    val transformOrigin = remember { mutableStateOf(TransformOrigin(1f, 0f)) }
    Layout(
        content = {
            MenuPanel(
                host = presentation.host,
                modifier = Modifier.graphicsLayer {
                    val value = progress.value
                    val scale = 0.4f + 0.6f * value
                    scaleX = scale
                    scaleY = scale
                    alpha = value.coerceIn(0f, 1f)
                    this.transformOrigin = transformOrigin.value
                },
                content = content,
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val margin = menuMargin.toPx()
        val gap = menuGap.toPx()
        val top = insets.getTop(this) + margin
        val bottom = height - insets.getBottom(this) - margin
        val left = insets.getLeft(this, layoutDirection) + margin
        val right = width - insets.getRight(this, layoutDirection) - margin
        val anchor = presentation.anchor.translate(-origin)
        val below = bottom - (anchor.bottom + gap)
        val above = anchor.top - gap - top
        val available = max(below, above).coerceAtLeast(menuRowMinHeight.toPx())
        val maximumWidth = min(menuMaximumWidth.toPx(), right - left).coerceAtLeast(0f).roundToInt()
        val panel = measurables.first().measure(
            Constraints(
                minWidth = min(menuMinimumWidth.roundToPx(), maximumWidth),
                maxWidth = maximumWidth,
                maxHeight = available.roundToInt(),
            ),
        )
        val placeBelow = panel.height <= below || below >= above
        val y = if (placeBelow) anchor.bottom + gap else anchor.top - gap - panel.height
        val alignTrailing = anchor.center.x > width / 2f
        val preferredX = if (alignTrailing) anchor.right - panel.width else anchor.left
        val x = preferredX.coerceIn(left, max(left, right - panel.width))
        val originX = if (panel.width > 0) ((anchor.center.x - x) / panel.width).coerceIn(0f, 1f) else 0.5f
        val newOrigin = TransformOrigin(originX, if (placeBelow) 0f else 1f)
        if (transformOrigin.value != newOrigin) {
            transformOrigin.value = newOrigin
        }
        layout(width, height) {
            panel.place(x.roundToInt(), y.roundToInt())
        }
    }
}

@Composable
private fun ContextMenuPresentation(row: ListRowState) {
    val state = row.menuTransition
    if (!state.isShowing()) {
        return
    }
    DisposableEffect(row) {
        onDispose {
            row.hidden = false
        }
    }
    val menu = row.menu ?: {}
    PresentationDialog(state = state, onBack = { row.dismissMenu() }) {
        CompositionLocalProvider(LocalElevated provides row.elevated) {
            val view = LocalView.current
            val window = (view.parent as? DialogWindowProvider)?.window
            val blur = remember(view) { crossWindowBlurEnabled(view.context) }
            val dark = isSystemInDarkTheme()
            val progress = transition.animateFloat(
                transitionSpec = {
                    if (targetState == EnterExitState.Visible) {
                        spring(dampingRatio = 0.8f, stiffness = 300f)
                    } else {
                        tween(250)
                    }
                },
                label = "ContextMenu",
            ) {
                if (it == EnterExitState.Visible) 1f else 0f
            }
            val fade = transition.animateFloat(transitionSpec = { tween(250) }, label = "ContextMenuFade") {
                if (it == EnterExitState.Visible) 1f else 0f
            }
            if (blur && window != null && Build.VERSION.SDK_INT >= 31) {
                val radius = with(LocalDensity.current) { (24.dp.toPx() * fade.value).roundToInt() }
                SideEffect {
                    window.setBackgroundBlurRadius(radius)
                }
            }
            val dim = when {
                blur && dark -> Color(0x4D000000)
                blur -> Color(0x1A000000)
                dark -> Color(0x80000000)
                else -> Color(0x40000000)
            }
            var origin by remember { mutableStateOf<Offset?>(null) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { origin = it.localToScreen(Offset.Zero) },
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBehind {
                            drawRect(dim.copy(alpha = dim.alpha * fade.value.coerceIn(0f, 1f)))
                        }
                        .pointerInput(row) {
                            detectTapGestures {
                                row.dismissMenu()
                            }
                        },
                )
                val screenOrigin = origin
                if (screenOrigin != null && screenOrigin.isSpecified) {
                    ContextMenuLayout(
                        row = row,
                        origin = screenOrigin,
                        progress = progress,
                        fade = fade,
                        menu = menu,
                    )
                }
            }
        }
    }
}

@Composable
private fun ContextMenuLayout(
    row: ListRowState,
    origin: Offset,
    progress: State<Float>,
    fade: State<Float>,
    menu: @Composable () -> Unit,
) {
    val palette = formPalette()
    val insets = hostSafeArea()
    val snapshot = row.menuSnapshot
    val shape = RoundedCornerShape(formCornerRadius)
    val transformOrigin = remember { mutableStateOf(TransformOrigin(0f, 0f)) }
    Layout(
        content = {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        val scale = 0.97f + 0.03f * progress.value
                        scaleX = scale
                        scaleY = scale
                        this.shape = shape
                        clip = true
                    }
                    .background(palette.cell)
                    .drawBehind {
                        if (snapshot != null) {
                            drawImage(snapshot)
                        }
                    },
            )
            MenuPanel(
                host = row.menuHost,
                modifier = Modifier.graphicsLayer {
                    val scale = 0.5f + 0.5f * progress.value
                    scaleX = scale
                    scaleY = scale
                    alpha = fade.value.coerceIn(0f, 1f)
                    this.transformOrigin = transformOrigin.value
                },
                content = menu,
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val margin = menuMargin.toPx()
        val gap = menuGap.toPx()
        val top = insets.getTop(this) + margin
        val bottom = height - insets.getBottom(this) - margin
        val left = insets.getLeft(this, layoutDirection) + margin
        val right = width - insets.getRight(this, layoutDirection) - margin
        val rowRect = row.menuRowScreen.translate(-origin)
        val preview = measurables[0].measure(
            Constraints.fixed(
                rowRect.width.roundToInt().coerceAtLeast(0),
                rowRect.height.roundToInt().coerceAtLeast(0),
            ),
        )
        val maximumWidth = min(menuMaximumWidth.toPx(), right - left).coerceAtLeast(0f).roundToInt()
        val available = (bottom - top - preview.height - gap).coerceAtLeast(menuRowMinHeight.toPx())
        val panel = measurables[1].measure(
            Constraints(
                minWidth = min(menuMinimumWidth.roundToPx(), maximumWidth),
                maxWidth = maximumWidth,
                maxHeight = available.roundToInt(),
            ),
        )
        val below = bottom - (rowRect.bottom + gap)
        val above = rowRect.top - gap - top
        var previewTop = rowRect.top.coerceAtLeast(top)
        val menuBelow: Boolean
        if (panel.height <= below) {
            menuBelow = true
        } else if (panel.height <= above) {
            menuBelow = false
        } else {
            menuBelow = true
            previewTop = max(top, bottom - panel.height - gap - preview.height)
        }
        val value = progress.value
        val animatedTop = rowRect.top + (previewTop - rowRect.top) * value
        val alignTrailing = row.menuTouchScreen.x - origin.x > rowRect.center.x
        val preferredX = if (alignTrailing) rowRect.right - panel.width else rowRect.left
        val menuX = preferredX.coerceIn(left, max(left, right - panel.width))
        val menuY = if (menuBelow) {
            animatedTop + preview.height + gap
        } else {
            animatedTop - gap - panel.height
        }
        val newOrigin = TransformOrigin(if (alignTrailing) 1f else 0f, if (menuBelow) 0f else 1f)
        if (transformOrigin.value != newOrigin) {
            transformOrigin.value = newOrigin
        }
        layout(width, height) {
            preview.place(rowRect.left.roundToInt(), animatedTop.roundToInt())
            panel.place(menuX.roundToInt(), menuY.roundToInt())
        }
    }
}

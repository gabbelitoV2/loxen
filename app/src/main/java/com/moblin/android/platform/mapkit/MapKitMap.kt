package com.moblin.android.platform.mapkit

import android.content.Context
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.corelocation.CLAuthorizationStatus
import com.moblin.android.platform.corelocation.CLLocationManager
import com.moblin.android.platform.corelocation.CLLocationManagerDelegate
import com.moblin.android.various.utils.CLLocationCoordinate2D
import com.moblin.android.various.utils.MKCoordinateRegion
import com.moblin.android.various.utils.MKCoordinateSpan
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponent
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.OnCameraTrackingChangedListener
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource

sealed class MapCameraPosition {
    data object automatic : MapCameraPosition()

    data class region(val region: MKCoordinateRegion) : MapCameraPosition()

    data class userLocation(
        val followsHeading: Boolean = false,
        val fallback: MapCameraPosition = automatic,
    ) : MapCameraPosition()

    val followsUserLocation: Boolean
        get() = this is userLocation

    val followsUserHeading: Boolean
        get() = (this as? userLocation)?.followsHeading == true
}

enum class MapInteractionModes {
    pan,
    zoom,
    rotate,
    pitch;

    companion object {
        val all: Set<MapInteractionModes> = entries.toSet()
    }
}

enum class CoordinateSpace {
    local,
    global,
}

class MapCameraUpdateContext internal constructor(val region: MKCoordinateRegion)

class MapProxy internal constructor() {
    internal var state: MapState? = null

    fun convert(point: CGPoint, from: CoordinateSpace = CoordinateSpace.local): CLLocationCoordinate2D? {
        val state = state ?: return null
        val local = if (from == CoordinateSpace.global) {
            CGPoint(x = point.x - state.originInWindow.x / state.density, y = point.y - state.originInWindow.y / state.density)
        } else {
            point
        }
        return state.coordinate(local)
    }

    fun convert(coordinate: CLLocationCoordinate2D, to: CoordinateSpace = CoordinateSpace.local): CGPoint? {
        val state = state ?: return null
        val pixels = state.pointPx(coordinate) ?: return null
        val offset = if (to == CoordinateSpace.global) state.originInWindow else Offset.Zero
        return CGPoint(x = (pixels.x + offset.x) / state.density.toDouble(), y = (pixels.y + offset.y) / state.density.toDouble())
    }
}

private val LocalMapProxy = staticCompositionLocalOf<MapProxy?> { null }

@Composable
fun MapReader(content: @Composable (MapProxy) -> Unit) {
    val proxy = remember { MapProxy() }
    CompositionLocalProvider(LocalMapProxy provides proxy) {
        content(proxy)
    }
}

class MapContentScope internal constructor(internal val state: MapState) {
    internal var selection by mutableStateOf<Any?>(null)
    internal var onSelect: ((Any?) -> Unit)? = null
}

@Composable
fun Map(
    position: MapCameraPosition,
    onPositionChange: (MapCameraPosition) -> Unit,
    modifier: Modifier = Modifier,
    interactionModes: Set<MapInteractionModes> = MapInteractionModes.all,
    onMapCameraChange: (MapCameraUpdateContext) -> Unit = {},
    onLongPress: ((CGPoint) -> Unit)? = null,
    content: @Composable MapContentScope.() -> Unit = {},
) {
    MapImpl(
        position = position,
        onPositionChange = onPositionChange,
        selection = null,
        onSelectionChange = null,
        modifier = modifier,
        interactionModes = interactionModes,
        onMapCameraChange = onMapCameraChange,
        onLongPress = onLongPress,
        content = content,
    )
}

@Suppress("UNCHECKED_CAST")
@Composable
fun <T> Map(
    position: MapCameraPosition,
    onPositionChange: (MapCameraPosition) -> Unit,
    selection: T?,
    onSelectionChange: (T?) -> Unit,
    modifier: Modifier = Modifier,
    interactionModes: Set<MapInteractionModes> = MapInteractionModes.all,
    onMapCameraChange: (MapCameraUpdateContext) -> Unit = {},
    onLongPress: ((CGPoint) -> Unit)? = null,
    content: @Composable MapContentScope.() -> Unit = {},
) {
    MapImpl(
        position = position,
        onPositionChange = onPositionChange,
        selection = selection,
        onSelectionChange = { onSelectionChange(it as T?) },
        modifier = modifier,
        interactionModes = interactionModes,
        onMapCameraChange = onMapCameraChange,
        onLongPress = onLongPress,
        content = content,
    )
}

@Composable
private fun MapImpl(
    position: MapCameraPosition,
    onPositionChange: (MapCameraPosition) -> Unit,
    selection: Any?,
    onSelectionChange: ((Any?) -> Unit)?,
    modifier: Modifier,
    interactionModes: Set<MapInteractionModes>,
    onMapCameraChange: (MapCameraUpdateContext) -> Unit,
    onLongPress: ((CGPoint) -> Unit)?,
    content: @Composable MapContentScope.() -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val isDark = isSystemInDarkTheme()
    val proxy = LocalMapProxy.current
    val state = remember { MapState() }
    val scope = remember(state) { MapContentScope(state) }
    val available = remember { MapLibreSnapshots.ensureMapLibre(context.applicationContext) }
    state.density = density
    scope.selection = selection
    scope.onSelect = onSelectionChange
    SideEffect {
        state.onPositionChange = onPositionChange
        state.onMapCameraChange = onMapCameraChange
        state.onLongPress = onLongPress
        state.onTap = if (onSelectionChange != null) {
            { if (scope.selection != null) onSelectionChange(null) }
        } else {
            null
        }
        state.setInteractionModes(interactionModes)
        state.setStyleUrl(if (isDark) MapKitConfiguration.darkStyleUrl else MapKitConfiguration.styleUrl)
        state.setPosition(position)
    }
    DisposableEffect(proxy, state) {
        proxy?.state = state
        onDispose {
            if (proxy?.state === state) {
                proxy.state = null
            }
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { state.setSize(it) }
            .onGloballyPositioned { state.originInWindow = it.positionInWindow() },
    ) {
        if (available) {
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            AndroidView(
                factory = { state.createView(it) },
                modifier = Modifier.fillMaxSize(),
                onRelease = { state.release() },
            )
            DisposableEffect(lifecycle, state) {
                val observer = LifecycleEventObserver { _, event ->
                    state.setLifecycle(event.targetState)
                }
                lifecycle.addObserver(observer)
                onDispose {
                    lifecycle.removeObserver(observer)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isDark) Color(0xFF1C1C1E) else Color(0xFFE5E5EA))
                    .pointerInput(state) {
                        detectTapGestures(
                            onTap = { state.onTap?.invoke() },
                            onLongPress = { offset ->
                                state.onLongPress?.invoke(CGPoint(x = offset.x / density.toDouble(), y = offset.y / density.toDouble()))
                            },
                        )
                    },
            )
        }
        Box(modifier = Modifier.fillMaxSize()) {
            scope.content()
        }
    }
}

private val markerTint = Color(0xFFFF3B30)
private val systemBlue = Color(0xFF007AFF)
private val balloonWidth = 28.dp
private val balloonHeight = 36.dp

@Composable
fun MapContentScope.Marker(
    coordinate: CLLocationCoordinate2D,
    tag: Any? = null,
    tint: Color = markerTint,
    label: @Composable () -> Unit,
) {
    val state = state
    val isSelected = tag != null && selection == tag
    val onSelect = onSelect
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color(0xFF1C1C1E)
    val haloColor = if (isDark) Color.Black else Color.White
    val interactionSource = remember { MutableInteractionSource() }
    var modifier = Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        val anchor = balloonHeight.roundToPx()
        layout(placeable.width, placeable.height) {
            val point = state.pointPx(coordinate)
            if (point != null) {
                placeable.place(point.x.roundToInt() - placeable.width / 2, point.y.roundToInt() - anchor)
            }
        }
    }
    if (tag != null && onSelect != null) {
        modifier = modifier.clickable(interactionSource = interactionSource, indication = null) {
            if (selection != tag) {
                onSelect(tag)
            }
        }
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        MarkerBalloon(tint = tint, isSelected = isSelected)
        CompositionLocalProvider(
            LocalContentColor provides textColor,
            LocalTextStyle provides TextStyle(
                color = textColor,
                fontSize = if (isSelected) 13.sp else 11.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                shadow = Shadow(color = haloColor, offset = Offset.Zero, blurRadius = 4f),
            ),
        ) {
            Box(modifier = Modifier.widthIn(max = 160.dp), contentAlignment = Alignment.Center) {
                label()
            }
        }
    }
}

@Composable
fun MapContentScope.Marker(
    title: String,
    coordinate: CLLocationCoordinate2D,
    tag: Any? = null,
    tint: Color = markerTint,
) {
    Marker(coordinate = coordinate, tag = tag, tint = tint) {
        Text(title)
    }
}

@Composable
fun MapContentScope.Marker(item: MKMapItem, tag: Any? = null, tint: Color = markerTint) {
    Marker(coordinate = item.placemark.coordinate, tag = tag, tint = tint) {
        item.name?.let { Text(it) }
    }
}

@Composable
private fun MarkerBalloon(tint: Color, isSelected: Boolean) {
    Canvas(
        modifier = Modifier
            .size(balloonWidth, balloonHeight)
            .graphicsLayer {
                val scale = if (isSelected) 1.35f else 1f
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0.5f, 1f)
            },
    ) {
        val radius = size.width / 2
        val path = Path().apply {
            moveTo(size.width / 2, size.height)
            lineTo(size.width * 0.2f, radius * 1.55f)
            lineTo(size.width * 0.8f, radius * 1.55f)
            close()
        }
        drawPath(path, tint)
        drawCircle(Color.White, radius = radius, center = Offset(radius, radius))
        drawCircle(tint, radius = radius - 1.5.dp.toPx(), center = Offset(radius, radius))
        drawCircle(Color.White, radius = radius * 0.32f, center = Offset(radius, radius))
    }
}

@Composable
fun MapContentScope.UserAnnotation() {
    val state = state
    DisposableEffect(state) {
        state.addUserAnnotation()
        onDispose {
            state.removeUserAnnotation()
        }
    }
}

@Composable
fun MapContentScope.MapPolyline(route: MKRoute, stroke: Color = systemBlue, lineWidth: Double = 3.0) {
    MapPolyline(coordinates = route.polyline.coordinates, stroke = stroke, lineWidth = lineWidth)
}

@Composable
fun MapContentScope.MapPolyline(polyline: MKPolyline, stroke: Color = systemBlue, lineWidth: Double = 3.0) {
    MapPolyline(coordinates = polyline.coordinates, stroke = stroke, lineWidth = lineWidth)
}

@Composable
fun MapContentScope.MapPolyline(
    coordinates: List<CLLocationCoordinate2D>,
    stroke: Color = systemBlue,
    lineWidth: Double = 3.0,
) {
    val state = state
    val key = remember { Any() }
    SideEffect {
        state.setPolyline(key, MapPolylineData(coordinates = coordinates, stroke = stroke, lineWidth = lineWidth))
    }
    DisposableEffect(state, key) {
        onDispose {
            state.removePolyline(key)
        }
    }
}

internal data class MapPolylineData(
    val coordinates: List<CLLocationCoordinate2D>,
    val stroke: Color,
    val lineWidth: Double,
)

internal class MapState {
    var density = 1f
    var originInWindow = Offset.Zero
    val revision = mutableIntStateOf(0)
    val polylineCount: Int
        get() = polylines.size
    val userAnnotationCount: Int
        get() = userAnnotations

    var onPositionChange: (MapCameraPosition) -> Unit = {}
    var onMapCameraChange: (MapCameraUpdateContext) -> Unit = {}
    var onLongPress: ((CGPoint) -> Unit)? = null
    var onTap: (() -> Unit)? = null

    private val handler = Handler(Looper.getMainLooper())
    private var sizePx = IntSize.Zero
    private var interactionModes: Set<MapInteractionModes> = MapInteractionModes.all
    private var fallbackLatitude = 0.0
    private var fallbackLongitude = 0.0
    private var fallbackZoom = 1.0
    private var fallbackBearing = 0.0

    private var mapView: MapView? = null
    private var map: MapLibreMap? = null
    private var style: Style? = null
    private var styleUrl: String? = null
    private var created = false
    private var started = false
    private var resumed = false
    private var released = false

    private var desired: MapCameraPosition? = null
    private var applied: MapCameraPosition? = null
    private var written: MapCameraPosition? = null
    private var hasPositioned = false
    private var changingMode = false
    private var automaticAttempts = 0
    private var lastReportedRegion: MKCoordinateRegion? = null

    private var userAnnotations = 0
    private val polylines = LinkedHashMap<Any, MapPolylineData>()
    private var locationManager: CLLocationManager? = null

    private val automaticRetry = Runnable {
        if (!released && desired == MapCameraPosition.automatic && !hasPositioned) {
            applied = null
            applyIfReady()
        }
    }

    private val trackingListener = object : OnCameraTrackingChangedListener {
        override fun onCameraTrackingDismissed() {
            if (!changingMode) {
                userMovedCamera()
            }
        }

        override fun onCameraTrackingChanged(currentMode: Int) {
            if (changingMode) {
                return
            }
            val position = desired as? MapCameraPosition.userLocation ?: return
            if (position.followsHeading && currentMode == CameraMode.TRACKING) {
                write(position.copy(followsHeading = false))
            }
        }
    }

    private val authorizationDelegate = object : CLLocationManagerDelegate {
        override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
            updateUserLocation()
            applied = null
            applyIfReady()
        }
    }

    fun setSize(size: IntSize) {
        if (size == sizePx) {
            return
        }
        sizePx = size
        applyIfReady()
        if (map == null && mapView == null && lastReportedRegion == null) {
            reportCameraChange()
        }
    }

    fun setInteractionModes(modes: Set<MapInteractionModes>) {
        interactionModes = modes
        map?.let { applyInteractionModes(it) }
    }

    fun setStyleUrl(url: String) {
        if (url == styleUrl) {
            return
        }
        styleUrl = url
        map?.let { loadStyle(it, url) }
    }

    fun setPosition(position: MapCameraPosition) {
        if (position == desired) {
            return
        }
        desired = position
        if (position == written) {
            written = null
            applied = position
            return
        }
        written = null
        applyIfReady()
    }

    fun addUserAnnotation() {
        userAnnotations += 1
        updateUserLocation()
    }

    fun removeUserAnnotation() {
        userAnnotations -= 1
        updateUserLocation()
    }

    fun setPolyline(key: Any, data: MapPolylineData) {
        if (polylines[key] == data) {
            return
        }
        polylines[key] = data
        syncPolylines()
    }

    fun removePolyline(key: Any) {
        if (polylines.remove(key) != null) {
            syncPolylines()
        }
    }

    fun createView(context: Context): MapView {
        val options = MapLibreMapOptions.createFromAttributes(context)
            .textureMode(true)
            .attributionEnabled(MapKitConfiguration.showsAttribution)
            .logoEnabled(MapKitConfiguration.showsAttribution)
            .compassEnabled(true)
        val view = MapView(context, options)
        view.onCreate(null)
        created = true
        released = false
        mapView = view
        view.getMapAsync { map ->
            if (mapView === view && !released) {
                onMapReady(map)
            }
        }
        return view
    }

    fun setLifecycle(state: Lifecycle.State) {
        val view = mapView ?: return
        val shouldStart = state.isAtLeast(Lifecycle.State.STARTED)
        val shouldResume = state.isAtLeast(Lifecycle.State.RESUMED)
        if (shouldStart && !started) {
            view.onStart()
            started = true
        }
        if (shouldResume && !resumed) {
            view.onResume()
            resumed = true
        }
        if (!shouldResume && resumed) {
            view.onPause()
            resumed = false
        }
        if (!shouldStart && started) {
            view.onStop()
            started = false
        }
    }

    fun release() {
        released = true
        handler.removeCallbacks(automaticRetry)
        val view = mapView ?: return
        try {
            map?.locationComponent?.let {
                if (it.isLocationComponentActivated) {
                    it.removeOnCameraTrackingChangedListener(trackingListener)
                }
            }
            if (resumed) {
                view.onPause()
            }
            if (started) {
                view.onStop()
            }
            if (created) {
                view.onDestroy()
            }
        } catch (error: RuntimeException) {
            log("Map release failed: $error")
        }
        resumed = false
        started = false
        created = false
        mapView = null
        map = null
        style = null
    }

    fun pointPx(coordinate: CLLocationCoordinate2D): Offset? {
        revision.intValue
        val map = map
        if (map != null) {
            val point = map.projection.toScreenLocation(latLng(coordinate))
            return Offset(point.x, point.y)
        }
        if (mapView != null) {
            return null
        }
        val frame = frame() ?: return null
        val point = MapKitProjection.point(frame, coordinate)
        return Offset((point.x * density).toFloat(), (point.y * density).toFloat())
    }

    fun coordinate(point: CGPoint): CLLocationCoordinate2D? {
        val map = map
        if (map != null) {
            val latLng = map.projection.fromScreenLocation(PointF((point.x * density).toFloat(), (point.y * density).toFloat()))
            return CLLocationCoordinate2D(latitude = latLng.latitude, longitude = latLng.longitude)
        }
        if (mapView != null) {
            return null
        }
        val frame = frame() ?: return null
        return MapKitProjection.coordinate(frame, point)
    }

    fun currentRegion(): MKCoordinateRegion? {
        val map = map
        if (map != null) {
            if (sizePx.width == 0 || sizePx.height == 0) {
                return null
            }
            val bounds = map.projection.visibleRegion.latLngBounds
            val target = map.cameraPosition.target ?: return null
            return MKCoordinateRegion(
                center = CLLocationCoordinate2D(latitude = target.latitude, longitude = target.longitude),
                span = MKCoordinateSpan(latitudeDelta = bounds.latitudeSpan, longitudeDelta = bounds.longitudeSpan),
            )
        }
        val frame = frame() ?: return null
        return MapKitProjection.region(frame)
    }

    private fun frame(): MapFrame? {
        if (sizePx.width == 0 || sizePx.height == 0) {
            return null
        }
        return MapFrame(
            latitude = fallbackLatitude,
            longitude = fallbackLongitude,
            zoom = fallbackZoom,
            bearing = fallbackBearing,
            tilt = 0.0,
            widthPoints = (sizePx.width / density).roundToInt().coerceAtLeast(1),
            heightPoints = (sizePx.height / density).roundToInt().coerceAtLeast(1),
            scale = density.toDouble(),
        )
    }

    private fun onMapReady(map: MapLibreMap) {
        this.map = map
        applyInteractionModes(map)
        map.addOnCameraMoveListener {
            revision.intValue += 1
        }
        map.addOnCameraIdleListener {
            revision.intValue += 1
            reportCameraChange()
        }
        map.addOnCameraMoveStartedListener { reason ->
            if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                hasPositioned = true
                if (desired is MapCameraPosition.userLocation && activeLocationComponent()?.cameraMode.let {
                        it == null || it == CameraMode.NONE
                    }
                ) {
                    userMovedCamera()
                }
            }
        }
        map.addOnMapClickListener {
            onTap?.invoke()
            false
        }
        map.addOnMapLongClickListener { latLng ->
            val handler = onLongPress ?: return@addOnMapLongClickListener false
            val point = map.projection.toScreenLocation(latLng)
            handler(CGPoint(x = point.x / density.toDouble(), y = point.y / density.toDouble()))
            true
        }
        styleUrl?.let { loadStyle(map, it) }
        revision.intValue += 1
        applyIfReady()
    }

    private fun loadStyle(map: MapLibreMap, url: String) {
        style = null
        map.setStyle(Style.Builder().fromUri(url)) { loaded ->
            if (this.map !== map || styleUrl != url) {
                return@setStyle
            }
            style = loaded
            syncPolylines()
            updateUserLocation()
            applied = null
            applyIfReady()
        }
    }

    private fun applyInteractionModes(map: MapLibreMap) {
        val settings = map.uiSettings
        settings.isScrollGesturesEnabled = interactionModes.contains(MapInteractionModes.pan)
        settings.isZoomGesturesEnabled = interactionModes.contains(MapInteractionModes.zoom)
        settings.isDoubleTapGesturesEnabled = interactionModes.contains(MapInteractionModes.zoom)
        settings.isQuickZoomGesturesEnabled = interactionModes.contains(MapInteractionModes.zoom)
        settings.isRotateGesturesEnabled = interactionModes.contains(MapInteractionModes.rotate)
        settings.isTiltGesturesEnabled = interactionModes.contains(MapInteractionModes.pitch)
    }

    private fun isReady(): Boolean {
        if (sizePx.width == 0 || sizePx.height == 0 || released) {
            return false
        }
        return mapView == null || map != null
    }

    private fun applyIfReady() {
        val position = desired ?: return
        if (!isReady() || position == applied) {
            return
        }
        applied = position
        apply(position)
    }

    private fun apply(position: MapCameraPosition) {
        when (position) {
            MapCameraPosition.automatic -> {
                setTracking(null)
                if (!hasPositioned) {
                    centerOnUser()
                }
            }
            is MapCameraPosition.region -> {
                setTracking(null)
                val current = lastReportedRegion
                if (current == null || !isSameRegion(current, position.region)) {
                    move(position.region)
                }
            }
            is MapCameraPosition.userLocation -> {
                if (activeLocationComponent() != null) {
                    if (!hasPositioned) {
                        applyFallback(position.fallback)
                    }
                    setTracking(position.followsHeading)
                } else {
                    apply(position.fallback)
                }
            }
        }
    }

    private fun applyFallback(position: MapCameraPosition) {
        when (position) {
            is MapCameraPosition.region -> move(position.region)
            is MapCameraPosition.userLocation -> applyFallback(position.fallback)
            MapCameraPosition.automatic -> Unit
        }
    }

    private fun centerOnUser() {
        val location = activeLocationComponent()?.lastKnownLocation ?: MapKitCurrentLocation.latest()
        if (location != null) {
            move(
                MKCoordinateRegion(
                    center = location.coordinate,
                    span = MKCoordinateSpan(latitudeDelta = 0.1, longitudeDelta = 0.1),
                ),
            )
            return
        }
        if (automaticAttempts < 30) {
            automaticAttempts += 1
            handler.removeCallbacks(automaticRetry)
            handler.postDelayed(automaticRetry, 1_000)
        }
    }

    private fun move(region: MKCoordinateRegion) {
        val widthPoints = sizePx.width / density.toDouble()
        val heightPoints = sizePx.height / density.toDouble()
        val zoom = MapKitProjection.zoom(region, widthPoints, heightPoints)
        hasPositioned = true
        val map = map
        if (map != null) {
            val camera = CameraPosition.Builder(map.cameraPosition)
                .target(latLng(region.center))
                .zoom(zoom)
                .build()
            map.moveCamera(CameraUpdateFactory.newCameraPosition(camera))
            return
        }
        fallbackLatitude = region.center.latitude.coerceIn(-85.0, 85.0)
        fallbackLongitude = MapKitProjection.wrapLongitude(region.center.longitude)
        fallbackZoom = zoom
        revision.intValue += 1
        reportCameraChange()
    }

    private fun setTracking(followsHeading: Boolean?) {
        val component = activeLocationComponent() ?: return
        val mode = when (followsHeading) {
            null -> CameraMode.NONE
            true -> CameraMode.TRACKING_COMPASS
            false -> CameraMode.TRACKING
        }
        if (component.cameraMode == mode) {
            return
        }
        changingMode = true
        try {
            if (mode != CameraMode.NONE && !hasPositioned) {
                component.setCameraMode(mode, 750L, 15.0, null, null, null)
            } else {
                component.setCameraMode(mode)
            }
        } catch (error: RuntimeException) {
            log("Camera mode change failed: $error")
        } finally {
            changingMode = false
        }
        if (mode != CameraMode.NONE) {
            hasPositioned = true
        }
    }

    private fun activeLocationComponent(): LocationComponent? {
        val component = map?.locationComponent ?: return null
        return try {
            if (component.isLocationComponentActivated && component.isLocationComponentEnabled) component else null
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun updateUserLocation() {
        val map = map ?: return
        val style = style?.takeIf { it.isFullyLoaded } ?: return
        val context = mapView?.context ?: return
        val component = map.locationComponent
        val wanted = userAnnotations > 0 || desired is MapCameraPosition.userLocation
        try {
            if (!wanted) {
                if (component.isLocationComponentActivated && component.isLocationComponentEnabled) {
                    component.isLocationComponentEnabled = false
                }
                return
            }
            if (!MapKitCurrentLocation.hasPermission(context)) {
                requestAuthorization()
                return
            }
            if (!component.isLocationComponentActivated) {
                val request = LocationEngineRequest.Builder(1_000)
                    .setFastestInterval(500)
                    .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                    .build()
                component.activateLocationComponent(
                    LocationComponentActivationOptions.builder(context, style)
                        .useDefaultLocationEngine(true)
                        .locationEngineRequest(request)
                        .build(),
                )
                component.addOnCameraTrackingChangedListener(trackingListener)
            }
            if (!component.isLocationComponentEnabled) {
                component.isLocationComponentEnabled = true
            }
            component.setRenderMode(RenderMode.COMPASS)
            component.lastKnownLocation?.let { MapKitCurrentLocation.reported = it }
        } catch (error: SecurityException) {
            log("No location permission: $error")
        } catch (error: RuntimeException) {
            log("Location component failed: $error")
        }
    }

    private fun requestAuthorization() {
        if (locationManager != null) {
            return
        }
        val manager = CLLocationManager()
        manager.delegate = authorizationDelegate
        locationManager = manager
        if (manager.authorizationStatus == CLAuthorizationStatus.notDetermined) {
            manager.requestWhenInUseAuthorization()
        }
    }

    private fun userMovedCamera() {
        hasPositioned = true
        if (desired !is MapCameraPosition.userLocation) {
            return
        }
        val region = currentRegion() ?: return
        write(MapCameraPosition.region(region))
    }

    private fun write(position: MapCameraPosition) {
        written = position
        applied = position
        onPositionChange(position)
    }

    private fun reportCameraChange() {
        val region = currentRegion() ?: return
        lastReportedRegion = region
        activeLocationComponent()?.lastKnownLocation?.let { MapKitCurrentLocation.reported = it }
        onMapCameraChange(MapCameraUpdateContext(region = region))
    }

    private fun syncPolylines() {
        val style = style?.takeIf { it.isFullyLoaded } ?: return
        try {
            val json = polylineGeoJson(polylines.values.toList())
            val source = style.getSourceAs<GeoJsonSource>(polylineSourceId)
            if (source != null) {
                source.setGeoJson(json)
            } else {
                val newSource = GeoJsonSource(polylineSourceId)
                newSource.setGeoJson(json)
                style.addSource(newSource)
            }
            if (style.getLayer(polylineLayerId) == null) {
                val layer = LineLayer(polylineLayerId, polylineSourceId).withProperties(
                    PropertyFactory.lineColor(Expression.toColor(Expression.get("color"))),
                    PropertyFactory.lineWidth(Expression.get("width")),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                )
                val firstSymbol = style.layers.firstOrNull { it is SymbolLayer }
                if (firstSymbol != null) {
                    style.addLayerBelow(layer, firstSymbol.id)
                } else {
                    style.addLayer(layer)
                }
            }
        } catch (error: RuntimeException) {
            log("Polyline update failed: $error")
        }
    }

    private fun isSameRegion(a: MKCoordinateRegion, b: MKCoordinateRegion): Boolean {
        val latitudeTolerance = maxOf(a.span.latitudeDelta, b.span.latitudeDelta) * 0.001 + 1e-9
        val longitudeTolerance = maxOf(a.span.longitudeDelta, b.span.longitudeDelta) * 0.001 + 1e-9
        return abs(a.center.latitude - b.center.latitude) <= latitudeTolerance &&
            abs(a.center.longitude - b.center.longitude) <= longitudeTolerance &&
            abs(a.span.latitudeDelta - b.span.latitudeDelta) <= latitudeTolerance * 10 &&
            abs(a.span.longitudeDelta - b.span.longitudeDelta) <= longitudeTolerance * 10
    }

    private fun latLng(coordinate: CLLocationCoordinate2D): LatLng {
        val latitude = if (coordinate.latitude.isFinite()) coordinate.latitude.coerceIn(-90.0, 90.0) else 0.0
        val longitude = if (coordinate.longitude.isFinite()) coordinate.longitude else 0.0
        return LatLng(latitude, longitude)
    }

    private fun log(message: String) {
        try {
            Log.w("MoblinMap", message)
        } catch (_: Throwable) {
        }
    }

    companion object {
        private const val polylineSourceId = "moblin-map-polylines"
        private const val polylineLayerId = "moblin-map-polylines"
    }
}

internal fun polylineGeoJson(polylines: List<MapPolylineData>): String {
    val features = polylines.filter { it.coordinates.size >= 2 }.joinToString(",") { polyline ->
        val coordinates = polyline.coordinates.joinToString(",") {
            String.format(Locale.US, "[%.7f,%.7f]", it.longitude, it.latitude)
        }
        val color = String.format(
            Locale.US,
            "rgba(%d,%d,%d,%.3f)",
            (polyline.stroke.red * 255).roundToInt(),
            (polyline.stroke.green * 255).roundToInt(),
            (polyline.stroke.blue * 255).roundToInt(),
            polyline.stroke.alpha,
        )
        "{\"type\":\"Feature\",\"properties\":{\"color\":\"$color\",\"width\":${polyline.lineWidth}}," +
            "\"geometry\":{\"type\":\"LineString\",\"coordinates\":[$coordinates]}}"
    }
    return "{\"type\":\"FeatureCollection\",\"features\":[$features]}"
}

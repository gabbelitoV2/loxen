package com.moblin.android.videoeffects

import androidx.compose.ui.geometry.Size
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsWidgetMap
import java.time.Instant
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan
import kotlinx.coroutines.launch

data class MapCoordinate(val latitude: Double, val longitude: Double)

data class MapLocation(
    val coordinate: MapCoordinate = MapCoordinate(0.0, 0.0),
    val speed: Double = -1.0,
    val course: Double = -1.0,
    val timestamp: Instant = Instant.EPOCH,
)

class MapCamera {
    var heading: Double = 0.0
    var centerCoordinate: MapCoordinate = MapCoordinate(0.0, 0.0)
    var centerCoordinateDistance: Double = 0.0
    var pitch: Double = 0.0
}

private fun MapCoordinate.translateMeters(x: Double, y: Double): MapCoordinate =
    TODO("port of CLLocationCoordinate2D.translateMeters from the CoreLocation extension")

class MapEffect(widget: SettingsWidgetMap) : VideoEffect() {
    private var mapSnapshot: EffectImageCiImage? = null
    private val widget: SettingsWidgetMap
    private var sceneWidget: SettingsSceneWidget? = null
    private var location: MapLocation = MapLocation()
    private var size: Size = Size.Zero
    private var newLocations: ArrayDeque<MapLocation> = ArrayDeque(listOf(MapLocation()))
    private var mapSnapshotter: MapCamera? = null
    private val dot: EffectImageCgImage?
    private var dotOffsetRatio = 0.0
    private var zoomOutFactor: Int? = null
    private var isLocationUpdated: Boolean = true

    init {
        this.widget = widget.clone()
        dot = TODO("load the MapDot drawable and convert it to EffectImageCgImage")
    }

    fun zoomOutTemporarily() {
        processorPipelineQueue.launch {
            if (zoomOutFactor == null) {
                zoomOutFactor = 2
            }
        }
    }

    fun setSceneWidget(sceneWidget: SettingsSceneWidget?) {
        processorPipelineQueue.launch {
            this@MapEffect.sceneWidget = sceneWidget
        }
    }

    fun updateLocation(location: MapLocation) {
        processorPipelineQueue.launch {
            isLocationUpdated = true
            newLocations.addLast(location)
            if (newLocations.size > 10) {
                newLocations.removeFirst()
            }
        }
    }

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        val size = image.extent.size
        update(size)
        val sceneWidget = this.sceneWidget
        val dot = this.dot
        val mapSnapshot = this.mapSnapshot
        if (sceneWidget == null || dot == null || mapSnapshot == null) {
            return image
        }
        val height = toPixels(sceneWidget.layout.size, size.height)
        val width = toPixels(sceneWidget.layout.size, size.width)
        val side = maxOf(40f, minOf(height, width))
        return TODO("OpenGL ES port")
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        val size = image.extent.size
        update(size)
        val sceneWidget = this.sceneWidget
        val dot = this.dot
        val mapSnapshot = this.mapSnapshot
        if (sceneWidget == null || dot == null || mapSnapshot == null) {
            return image
        }
        return TODO("OpenGL ES port")
    }

    private fun nextNewLocation(): MapLocation {
        val now = Instant.now()
        val delay = widget.delay
        return newLocations.lastOrNull { it.timestamp.plusMillis((delay * 1000).toLong()) <= now }
            ?: newLocations.first()
    }

    private fun update(size: Size) {
        val newLocation = nextNewLocation()
        val zoomOutFactor = this.zoomOutFactor
        val isLocationUpdated = this.isLocationUpdated
        this.isLocationUpdated = false
        if (size == this.size
            && newLocation.coordinate.latitude == location.coordinate.latitude
            && newLocation.coordinate.longitude == location.coordinate.longitude
            && newLocation.speed == location.speed
            && !(zoomOutFactor != null && isLocationUpdated)
        ) {
            return
        }
        val (mapSnapshotter, dotOffsetRatio) = createSnapshotter(newLocation, zoomOutFactor)
        this.mapSnapshotter = mapSnapshotter
        startMapSnapshotter(mapSnapshotter, dotOffsetRatio)
        this.size = size
        location = newLocation
    }

    private fun createSnapshotter(newLocation: MapLocation, zoomOutFactor: Int?): Pair<MapCamera, Double> {
        var zoomOut = zoomOutFactor
        if (zoomOut == 10) {
            zoomOut = null
            this.zoomOutFactor = null
        }
        val camera = MapCamera()
        if (!widget.northUp) {
            camera.heading = newLocation.course
        }
        camera.centerCoordinate = newLocation.coordinate
        camera.centerCoordinateDistance = widget.size
        var dotOffsetRatio = 0.0
        if (newLocation.speed > 4 && zoomOut == null) {
            camera.centerCoordinateDistance += 150 * (newLocation.speed - 4)
            if (!widget.northUp) {
                val halfMapSideLength = tan(PI / 12) * camera.centerCoordinateDistance
                val maxDotOffsetFromCenter = halfMapSideLength / 2
                val maxDotSpeed = 20.0
                val k = maxDotOffsetFromCenter / (maxDotSpeed - 4)
                var dotOffsetInMeters = k * (newLocation.speed - 4)
                if (dotOffsetInMeters > maxDotOffsetFromCenter) {
                    dotOffsetInMeters = maxDotOffsetFromCenter
                }
                val course = Math.toRadians(maxOf(newLocation.course, 0.0))
                val latitudeOffsetInMeters = cos(course) * dotOffsetInMeters
                val longitudeOffsetInMeters = sin(course) * dotOffsetInMeters
                camera.centerCoordinate = newLocation.coordinate.translateMeters(
                    x = longitudeOffsetInMeters,
                    y = latitudeOffsetInMeters,
                )
                dotOffsetRatio = dotOffsetInMeters / halfMapSideLength
            }
        }
        camera.pitch = 0.0
        zoomOut?.let { factor ->
            camera.centerCoordinateDistance *= 5.0.pow(factor.toDouble())
            if (factor <= 9) {
                this.zoomOutFactor = factor + 1
            }
        }
        return MapCameraHolder(camera) to dotOffsetRatio
    }

    private fun startMapSnapshotter(mapSnapshotter: MapCamera, dotOffsetRatio: Double) {
        TODO("no Android counterpart for MapKit")
    }
}

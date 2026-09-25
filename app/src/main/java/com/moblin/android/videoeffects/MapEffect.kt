package com.moblin.android.videoeffects

import android.location.Location
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.Bundle
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.mapkit.MKMapCamera
import com.moblin.android.platform.mapkit.MKMapSnapshotter
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.metalpetal.MTIMultilayerCompositingFilter
import com.moblin.android.platform.metalpetal.MTILayer
import com.moblin.android.platform.uikit.cgImage
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsWidgetMap
import com.moblin.android.various.utils.CLLocationCoordinate2D
import com.moblin.android.various.utils.translateMeters
import java.time.Instant
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan
import kotlinx.coroutines.launch

class MapEffect(widget: SettingsWidgetMap) : VideoEffect() {
    private var mapSnapshot: EffectImageCiImage? = null
    private val widget: SettingsWidgetMap = widget.clone()
    private var sceneWidget: SettingsSceneWidget? = null
    private var location: Location = Location("")
    private var size: CGSize = CGSize.zero
    private var newLocations: ArrayDeque<Location> = ArrayDeque()
    private var mapSnapshotter: MKMapSnapshotter? = null
    private val dot: EffectImageCgImage? = Bundle.image("MapDot")?.cgImage?.toEffectImage()
    private var dotOffsetRatio = 0.0
    private var zoomOutFactor: Int? = null
    private var isLocationUpdated: Boolean = true
    private var isSnapshotInProgress: Boolean = false

    fun zoomOutTemporarily() {
        processorPipelineQueue.launch {
            if (this@MapEffect.zoomOutFactor == null) {
                this@MapEffect.zoomOutFactor = 2
            }
        }
    }

    fun setSceneWidget(sceneWidget: SettingsSceneWidget?) {
        processorPipelineQueue.launch {
            this@MapEffect.sceneWidget = sceneWidget
        }
    }

    fun updateLocation(location: Location) {
        processorPipelineQueue.launch {
            this@MapEffect.isLocationUpdated = true
            this@MapEffect.newLocations.addLast(location)
            if (this@MapEffect.newLocations.size > 10) {
                this@MapEffect.newLocations.removeFirst()
            }
        }
    }

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        val size = image.extent.size
        update(size = size)
        val sceneWidget = this.sceneWidget ?: return image
        val dot = this.dot ?: return image
        val mapSnapshot = this.mapSnapshot ?: return image
        val dotImage = dot.getCiImage()
        val mapImage = mapSnapshot.getCiImage()
        val height = toPixels(sceneWidget.layout.size, size.height)
        val width = toPixels(sceneWidget.layout.size, size.width)
        val side = maxOf(40.0, minOf(height, width))
        val mapWithDotImage = dotImage
            .translated(x = (side - 30) / 2, y = (side - 30) / 2 - dotOffsetRatio * side / 2)
            .composited(over = mapImage
                .scaled(x = side / mapImage.extent.width,
                    y = side / mapImage.extent.width))
        return applyEffectsResizeMirrorMove(mapWithDotImage, sceneWidget, false, image.extent, info)
            .composited(over = image)
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        val size = image.extent.size
        update(size = size)
        val sceneWidget = this.sceneWidget ?: return image
        val dot = this.dot ?: return image
        val mapSnapshot = this.mapSnapshot ?: return image
        val mapImage = mapSnapshot.getMetalPetalImage()
        val dotImage = dot.getMetalPetalImage()
        val mapWidth = mapImage.size.width
        val mapHeight = mapImage.size.height
        val height = toPixels(sceneWidget.layout.size, size.height)
        val width = toPixels(sceneWidget.layout.size, size.width)
        val side = maxOf(40.0, minOf(height, width))
        val dotSide = 30 * mapWidth / side
        val dotSize = CGSize(width = dotSide, height = dotSide)
        val dotX = mapWidth / 2
        val dotY = mapHeight / 2 + dotOffsetRatio * mapHeight / 2
        val filter = MTIMultilayerCompositingFilter()
        filter.inputBackgroundImage = mapImage
        filter.layers = listOf(
            MTILayer(content = dotImage, position = CGPoint(x = dotX, y = dotY), size = dotSize),
        )
        val mapWithDotImage = filter.outputImage ?: return image
        return applyEffectsResizeMirrorMoveMetalPetal(mapWithDotImage,
            sceneWidget,
            false,
            image,
            info)
    }

    private fun nextNewLocation(): Location? {
        val now = Instant.now()
        val delay = widget.delay
        return newLocations.lastOrNull {
            Instant.ofEpochMilli(it.time).plusMillis((delay * 1000.0).toLong()) <= now
        } ?: newLocations.firstOrNull()
    }

    private fun update(size: CGSize) {
        if (isSnapshotInProgress) {
            return
        }
        val newLocation = nextNewLocation()
        val zoomOutFactor = this.zoomOutFactor
        val isLocationUpdated = this.isLocationUpdated
        this.isLocationUpdated = false
        if (newLocation == null) {
            return
        }
        if (!(size.width != this.size.width
                || size.height != this.size.height
                || newLocation.latitude != location.latitude
                || newLocation.longitude != location.longitude
                || newLocation.speed != location.speed
                || (zoomOutFactor != null && isLocationUpdated))
        ) {
            return
        }
        val (mapSnapshotter, dotOffsetRatio) = createSnapshotter(
            newLocation = newLocation,
            zoomOutFactor = zoomOutFactor
        )
        this.mapSnapshotter = mapSnapshotter
        isSnapshotInProgress = true
        mapSnapshotter.start(with = null) { snapshot, error ->
            if (snapshot == null || error != null) {
                processorPipelineQueue.launch {
                    this@MapEffect.isSnapshotInProgress = false
                }
                return@start
            }
            val image = snapshot.image.cgImage
            val mapSnapshot = CIImage(cgImage = image).toEffectImage(isOpaque = true)
            processorPipelineQueue.launch {
                this@MapEffect.isSnapshotInProgress = false
                this@MapEffect.mapSnapshot = mapSnapshot
                this@MapEffect.dotOffsetRatio = dotOffsetRatio
            }
        }
        this.size = size
        location = newLocation
    }

    private fun createSnapshotter(newLocation: Location,
                                  zoomOutFactor: Int?): Pair<MKMapSnapshotter, Double>
    {
        var zoomOutFactor = zoomOutFactor
        if (zoomOutFactor == 10) {
            zoomOutFactor = null
            this.zoomOutFactor = null
        }
        val camera = MKMapCamera()
        if (!widget.northUp) {
            camera.heading = if (newLocation.hasBearing()) newLocation.bearing.toDouble() else -1.0
        }
        camera.centerCoordinate = CLLocationCoordinate2D(latitude = newLocation.latitude,
            longitude = newLocation.longitude)
        camera.centerCoordinateDistance = widget.size
        var dotOffsetRatio = 0.0
        if (newLocation.speed > 4 && zoomOutFactor == null) {
            camera.centerCoordinateDistance += 150.0 * (newLocation.speed - 4)
            if (!widget.northUp) {
                val halfMapSideLength = tan(PI / 12) * camera.centerCoordinateDistance
                val maxDotOffsetFromCenter = halfMapSideLength / 2
                val maxDotSpeed = 20.0
                val k = maxDotOffsetFromCenter / (maxDotSpeed - 4)
                var dotOffsetInMeters = k * (newLocation.speed - 4)
                if (dotOffsetInMeters > maxDotOffsetFromCenter) {
                    dotOffsetInMeters = maxDotOffsetFromCenter
                }
                val course = Math.toRadians(
                    maxOf(if (newLocation.hasBearing()) newLocation.bearing.toDouble() else -1.0, 0.0)
                )
                val latitudeOffsetInMeters = cos(course) * dotOffsetInMeters
                val longitudeOffsetInMeters = sin(course) * dotOffsetInMeters
                camera.centerCoordinate = CLLocationCoordinate2D(latitude = newLocation.latitude,
                    longitude = newLocation.longitude).translateMeters(
                    x = longitudeOffsetInMeters,
                    y = latitudeOffsetInMeters
                )
                dotOffsetRatio = dotOffsetInMeters / halfMapSideLength
            }
        }
        camera.pitch = 0.0
        val factor = zoomOutFactor
        if (factor != null) {
            camera.centerCoordinateDistance *= 5.0.pow(factor.toDouble())
            if (factor <= 9) {
                this.zoomOutFactor = factor + 1
            }
        }
        val options = MKMapSnapshotter.Options()
        options.camera = camera
        return Pair(MKMapSnapshotter(options = options), dotOffsetRatio)
    }
}

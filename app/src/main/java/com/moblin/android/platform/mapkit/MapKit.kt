package com.moblin.android.platform.mapkit

import android.graphics.Bitmap
import android.os.Looper
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.various.utils.CLLocationCoordinate2D
import java.lang.ref.WeakReference

object MapKitConfiguration {
    @Volatile
    var styleUrl: String = "https://tiles.openfreemap.org/styles/liberty"

    @Volatile
    var darkStyleUrl: String = "https://tiles.openfreemap.org/styles/dark"

    @Volatile
    var showsAttribution: Boolean = true
}

class MKError(message: String) : Exception(message)

class MKMapCamera {
    var centerCoordinate: CLLocationCoordinate2D = CLLocationCoordinate2D(0.0, 0.0)
    var centerCoordinateDistance: Double = 0.0
    var heading: Double = 0.0
    var pitch: Double = 0.0

    internal fun copy(): MKMapCamera {
        val camera = MKMapCamera()
        camera.centerCoordinate = centerCoordinate
        camera.centerCoordinateDistance = centerCoordinateDistance
        camera.heading = heading
        camera.pitch = pitch
        return camera
    }
}

class MKMapSnapshotter(options: Options) {
    class Options {
        var camera: MKMapCamera = MKMapCamera()
        var size: CGSize = CGSize(256.0, 256.0)
        var scale: Double = 3.0
        var mapType: Int = 0

        internal fun copy(): Options {
            val options = Options()
            options.camera = camera.copy()
            options.size = size
            options.scale = scale
            options.mapType = mapType
            return options
        }
    }

    class Snapshot internal constructor(val image: Bitmap, internal val frame: MapFrame) {
        fun point(`for`: CLLocationCoordinate2D): CGPoint {
            return MapKitProjection.point(frame, `for`)
        }
    }

    internal val options: Options = options.copy()

    @Volatile
    private var request: MapSnapshotRequest? = null

    val isLoading: Boolean
        get() = request?.isLoading == true

    fun start(with: Any?, completionHandler: (Snapshot?, Throwable?) -> Unit) {
        request?.let { MapLibreSnapshots.cancel(it) }
        val request = MapSnapshotRequest(
            owner = WeakReference(this),
            options = options.copy(),
            queue = with,
            completionHandler = completionHandler,
        )
        this.request = request
        MapLibreSnapshots.submit(request)
    }

    fun start(completionHandler: (Snapshot?, Throwable?) -> Unit) {
        start(with = Looper.getMainLooper(), completionHandler = completionHandler)
    }

    fun cancel() {
        request?.let { MapLibreSnapshots.cancel(it) }
    }
}

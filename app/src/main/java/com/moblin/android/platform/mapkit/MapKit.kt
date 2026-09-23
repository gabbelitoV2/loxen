package com.moblin.android.platform.mapkit

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coreimage.internal.EffectsLog
import com.moblin.android.various.utils.CLLocationCoordinate2D
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val mapKitExecutor: ExecutorService by lazy {
    Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "MoblinMap").apply {
            isDaemon = true
        }
    }
}

private fun deliver(queue: Any?, block: () -> Unit) {
    when (queue) {
        is CoroutineScope -> queue.launch { block() }
        is CoroutineDispatcher -> CoroutineScope(queue).launch { block() }
        is Handler -> queue.post(block)
        is Looper -> Handler(queue).post(block)
        else -> mapKitExecutor.execute(block)
    }
}

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

    class Snapshot internal constructor(val image: Bitmap) {
        @Suppress("UNUSED_PARAMETER")
        fun point(`for`: CLLocationCoordinate2D): CGPoint {
            EffectsLog.notImplemented("MKMapSnapshotter.Snapshot.point")
            return CGPoint.zero
        }
    }

    internal val options: Options = options.copy()

    @Volatile
    private var loading = false

    @Volatile
    private var generation = 0

    val isLoading: Boolean
        get() = loading

    fun start(with: Any?, completionHandler: (Snapshot?, Throwable?) -> Unit) {
        EffectsLog.notImplemented("MKMapSnapshotter.start")
        generation += 1
        val startedGeneration = generation
        loading = true
        deliver(with) {
            if (startedGeneration != generation) {
                return@deliver
            }
            loading = false
            completionHandler(null, null)
        }
    }

    fun start(completionHandler: (Snapshot?, Throwable?) -> Unit) {
        start(with = Dispatchers.Main, completionHandler = completionHandler)
    }

    fun cancel() {
        generation += 1
        loading = false
    }
}

package com.moblin.android.platform.mapkit

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.moblin.android.AppDelegate
import com.moblin.android.platform.core.PipelineStats
import java.lang.ref.WeakReference
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.snapshotter.MapSnapshot
import org.maplibre.android.snapshotter.MapSnapshotter

internal class MapSnapshotRequest(
    val owner: WeakReference<MKMapSnapshotter>,
    val options: MKMapSnapshotter.Options,
    val queue: Any?,
    val completionHandler: (MKMapSnapshotter.Snapshot?, Throwable?) -> Unit,
) {
    val sequence: Long = nextSequence.incrementAndGet()

    @Volatile
    var isLoading: Boolean = true

    @Volatile
    var isCancelled: Boolean = false

    private companion object {
        val nextSequence = AtomicLong()
    }
}

internal object MapLibreSnapshots {
    private const val TAG = "MoblinMap"
    private const val maximumRunning = 3
    private const val maximumIdle = 2
    private const val idleTimeoutMs = 30_000L
    private const val runningTimeoutMs = 30_000L
    private const val watchdogIntervalMs = 1_000L

    private class NativeSnapshotter(
        val id: Long,
        val snapshotter: MapSnapshotter,
        val key: String,
        var styleUrl: String,
    ) {
        var idleSinceMs: Long = 0
    }

    private class Running(
        val request: MapSnapshotRequest,
        val native: NativeSnapshotter,
        val frame: MapFrame,
        val isDark: Boolean,
        val startedAtMs: Long,
    )

    private val mainHandler: Handler by lazy { Handler(Looper.getMainLooper()) }
    private val completionExecutor: ExecutorService by lazy {
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "MoblinMap").apply {
                isDaemon = true
            }
        }
    }
    private val loggedMessages = HashSet<String>()
    private var mapLibreReady: Boolean? = null
    private var nextNativeId = 0L
    private val running = ArrayList<Running>()
    private val pending = ArrayList<MapSnapshotRequest>()
    private val idle = ArrayList<NativeSnapshotter>()
    private var watchdogScheduled = false
    private val watchdog = Runnable {
        watchdogScheduled = false
        tick()
    }

    fun submit(request: MapSnapshotRequest) {
        runOnMain {
            startOrQueue(request)
        }
    }

    fun cancel(request: MapSnapshotRequest) {
        request.isCancelled = true
        request.isLoading = false
        runOnMain {
            pending.remove(request)
            val entry = running.firstOrNull { it.request === request }
            if (entry != null) {
                running.remove(entry)
                cancelNative(entry.native)
                log(Log.DEBUG, "Snapshot ${request.sequence} cancelled")
            }
            startPending()
            publishGauges()
        }
    }

    fun deliver(queue: Any?, block: () -> Unit) {
        val guarded = {
            try {
                block()
            } catch (error: Throwable) {
                logOnce("handler:${error.javaClass.name}", "Snapshot completion handler failed: $error", error)
            }
        }
        when (queue) {
            is CoroutineScope -> queue.launch { guarded() }
            is CoroutineDispatcher -> CoroutineScope(queue).launch { guarded() }
            is Handler -> queue.post(guarded)
            is Looper -> Handler(queue).post(guarded)
            is Executor -> queue.execute(guarded)
            else -> completionExecutor.execute(guarded)
        }
    }

    private fun runOnMain(block: () -> Unit) {
        val mainLooper = Looper.getMainLooper()
        if (mainLooper != null && Looper.myLooper() === mainLooper) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    private fun startOrQueue(request: MapSnapshotRequest) {
        if (!isWanted(request)) {
            return
        }
        if (running.size >= maximumRunning || pending.isNotEmpty()) {
            pending.add(request)
            startPending()
            scheduleWatchdog()
            return
        }
        launch(request)
    }

    private fun isWanted(request: MapSnapshotRequest): Boolean {
        return !request.isCancelled && request.isLoading
    }

    private fun isQueuedWanted(request: MapSnapshotRequest): Boolean {
        if (!isWanted(request)) {
            return false
        }
        if (request.owner.get() == null) {
            request.isLoading = false
            log(Log.DEBUG, "Snapshot ${request.sequence} dropped before start (snapshotter released)")
            return false
        }
        return true
    }

    private fun launch(request: MapSnapshotRequest) {
        val context = applicationContext()
        if (context == null) {
            fail(request, "No application context")
            return
        }
        if (!ensureMapLibre(context)) {
            fail(request, "MapLibre is unavailable")
            return
        }
        val frame = MapKitProjection.frame(request.options)
        if (frame == null) {
            fail(request, "Invalid camera center coordinate")
            return
        }
        val isDark = isDarkMode(context)
        val styleUrl = if (isDark) MapKitConfiguration.darkStyleUrl else MapKitConfiguration.styleUrl
        val showsAttribution = MapKitConfiguration.showsAttribution
        val key = "${frame.widthPoints}x${frame.heightPoints}@${frame.scale}:$showsAttribution"
        val cameraPosition: CameraPosition
        try {
            cameraPosition = CameraPosition.Builder()
                .target(LatLng(frame.latitude, frame.longitude))
                .zoom(frame.zoom)
                .bearing(frame.bearing)
                .tilt(frame.tilt)
                .build()
        } catch (error: Throwable) {
            fail(request, "Invalid camera: $error")
            return
        }
        var native: NativeSnapshotter? = null
        try {
            native = takeIdle(key)
            if (native != null) {
                if (native.styleUrl != styleUrl) {
                    native.snapshotter.setStyleUrl(styleUrl)
                    native.styleUrl = styleUrl
                }
                native.snapshotter.setCameraPosition(cameraPosition)
            } else {
                val options = MapSnapshotter.Options(frame.widthPoints, frame.heightPoints)
                    .withPixelRatio(frame.scale.toFloat())
                    .withStyle(styleUrl)
                    .withCameraPosition(cameraPosition)
                    .withLogo(false)
                    .withAttribution(showsAttribution)
                nextNativeId += 1
                native = NativeSnapshotter(
                    id = nextNativeId,
                    snapshotter = MapSnapshotter(context, options),
                    key = key,
                    styleUrl = styleUrl,
                )
                log(
                    Log.INFO,
                    "Created snapshotter ${native.id} (${frame.widthPixels}x${frame.heightPixels}, " +
                        "running ${running.size}, idle ${idle.size})",
                )
            }
            val entry = Running(
                request = request,
                native = native,
                frame = frame,
                isDark = isDark,
                startedAtMs = SystemClock.elapsedRealtime(),
            )
            running.add(entry)
            native.snapshotter.start(
                object : MapSnapshotter.SnapshotReadyCallback {
                    override fun onSnapshotReady(snapshot: MapSnapshot) {
                        mainHandler.post {
                            onReady(entry, snapshot)
                        }
                    }
                },
                object : MapSnapshotter.ErrorHandler {
                    override fun onError(error: String) {
                        mainHandler.post {
                            onFailed(entry, error)
                        }
                    }
                },
            )
        } catch (error: Throwable) {
            running.removeAll { it.request === request }
            native?.let { cancelNative(it) }
            logOnce("start:${error.javaClass.name}", "Snapshot start failed: $error", error)
            fail(request, "Snapshot start failed: $error")
            return
        } finally {
            publishGauges()
        }
        scheduleWatchdog()
    }

    private fun onReady(entry: Running, snapshot: MapSnapshot) {
        if (!running.remove(entry)) {
            return
        }
        returnToIdle(entry.native)
        val request = entry.request
        val durationMs = SystemClock.elapsedRealtime() - entry.startedAtMs
        val bitmap = snapshot.bitmap
        if (!isWanted(request)) {
            log(Log.DEBUG, "Snapshot ${request.sequence} dropped after $durationMs ms (cancelled)")
        } else {
            request.isLoading = false
            val result = MKMapSnapshotter.Snapshot(image = bitmap, frame = entry.frame)
            PipelineStats.increment("mapSnaps")
            log(
                Log.DEBUG,
                String.format(
                    Locale.US,
                    "Snapshot %d %dx%d in %d ms (zoom %.2f, bearing %.0f, %s, running %d, idle %d)",
                    request.sequence,
                    bitmap.width,
                    bitmap.height,
                    durationMs,
                    entry.frame.zoom,
                    entry.frame.bearing,
                    if (entry.isDark) "dark" else "light",
                    running.size,
                    idle.size,
                ),
            )
            deliver(request.queue) {
                request.completionHandler(result, null)
            }
        }
        startPending()
        publishGauges()
    }

    private fun onFailed(entry: Running, error: String) {
        if (!running.remove(entry)) {
            return
        }
        cancelNative(entry.native)
        fail(entry.request, error)
        startPending()
        publishGauges()
    }

    private fun fail(request: MapSnapshotRequest, message: String) {
        if (!isWanted(request)) {
            return
        }
        request.isLoading = false
        logOnce("fail:$message", "Snapshot ${request.sequence} failed: $message")
        deliver(request.queue) {
            request.completionHandler(null, MKError(message))
        }
    }

    private fun startPending() {
        pending.removeAll { !isQueuedWanted(it) }
        while (running.size < maximumRunning && pending.isNotEmpty()) {
            launch(pending.removeAt(0))
        }
    }

    private fun takeIdle(key: String): NativeSnapshotter? {
        val index = idle.indexOfLast { it.key == key }
        if (index < 0) {
            return null
        }
        return idle.removeAt(index)
    }

    private fun returnToIdle(native: NativeSnapshotter) {
        native.idleSinceMs = SystemClock.elapsedRealtime()
        idle.add(native)
        while (idle.size > maximumIdle) {
            val dropped = idle.removeAt(0)
            log(Log.INFO, "Released snapshotter ${dropped.id} (pool full)")
        }
    }

    private fun cancelNative(native: NativeSnapshotter) {
        try {
            native.snapshotter.cancel()
        } catch (error: Throwable) {
            logOnce("cancel:${error.javaClass.name}", "Snapshotter cancel failed: $error", error)
        }
        log(Log.INFO, "Released snapshotter ${native.id} (cancelled)")
    }

    private fun tick() {
        val now = SystemClock.elapsedRealtime()
        for (entry in running.toList()) {
            if (now - entry.startedAtMs > runningTimeoutMs) {
                running.remove(entry)
                cancelNative(entry.native)
                fail(entry.request, "Snapshot timed out")
            }
        }
        val expired = idle.filter { now - it.idleSinceMs > idleTimeoutMs }
        for (native in expired) {
            idle.remove(native)
            log(Log.INFO, "Released snapshotter ${native.id} (idle)")
        }
        startPending()
        publishGauges()
        if (running.isNotEmpty() || pending.isNotEmpty() || idle.isNotEmpty()) {
            scheduleWatchdog()
        }
    }

    private fun scheduleWatchdog() {
        if (watchdogScheduled) {
            return
        }
        watchdogScheduled = true
        mainHandler.postDelayed(watchdog, watchdogIntervalMs)
    }

    private fun publishGauges() {
        PipelineStats.gauge("mapLive", (running.size + idle.size).toLong())
    }

    private fun applicationContext(): Context? {
        return try {
            AppDelegate.context
        } catch (_: Throwable) {
            null
        }
    }

    private fun ensureMapLibre(context: Context): Boolean {
        mapLibreReady?.let {
            return it
        }
        val ready = try {
            if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION, 0x400003)) {
                logOnce("vulkan", "MapLibre needs Vulkan 1.0.3, which this device lacks; map widgets stay empty")
                false
            } else {
                MapLibre.getInstance(context)
                log(Log.INFO, "MapLibre initialized")
                true
            }
        } catch (error: Throwable) {
            logOnce("init", "MapLibre initialization failed: $error", error)
            false
        }
        mapLibreReady = ready
        return ready
    }

    private fun isDarkMode(context: Context): Boolean {
        return try {
            val uiMode = context.resources.configuration.uiMode
            (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        } catch (_: Throwable) {
            false
        }
    }

    private fun log(priority: Int, message: String) {
        try {
            Log.println(priority, TAG, message)
        } catch (_: Throwable) {
        }
    }

    private fun logOnce(key: String, message: String, error: Throwable? = null) {
        val isNew = synchronized(loggedMessages) {
            if (loggedMessages.size > 200) {
                loggedMessages.clear()
            }
            loggedMessages.add(key)
        }
        if (!isNew) {
            return
        }
        try {
            if (error != null) {
                Log.w(TAG, message, error)
            } else {
                Log.w(TAG, message)
            }
        } catch (_: Throwable) {
        }
    }
}

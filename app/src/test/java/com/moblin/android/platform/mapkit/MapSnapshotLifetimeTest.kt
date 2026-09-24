package com.moblin.android.platform.mapkit

import android.os.Looper
import java.lang.ref.WeakReference
import java.util.concurrent.Executor
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class MapSnapshotLifetimeTest {
    private val direct = Executor { it.run() }

    @Test
    fun referencedSnapshotterCallsItsHandler() {
        val snapshotter = MKMapSnapshotter(options = MKMapSnapshotter.Options())
        var calls = 0
        var error: Throwable? = null
        snapshotter.start(with = direct) { snapshot, failure ->
            calls += 1
            error = failure
            assertNull(snapshot)
        }
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, calls)
        assertTrue(error is MKError)
        assertFalse(snapshotter.isLoading)
    }

    @Test
    fun releasedSnapshotterNeverCallsItsHandler() {
        var calls = 0
        val request = MapSnapshotRequest(
            owner = WeakReference<MKMapSnapshotter>(null),
            options = MKMapSnapshotter.Options(),
            queue = direct,
            completionHandler = { _, _ -> calls += 1 },
        )
        MapLibreSnapshots.submit(request)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(0, calls)
        assertFalse(request.isLoading)
    }

    @Test
    fun cancelledSnapshotterNeverCallsItsHandler() {
        val snapshotter = MKMapSnapshotter(options = MKMapSnapshotter.Options())
        var calls = 0
        val request = MapSnapshotRequest(
            owner = WeakReference(snapshotter),
            options = MKMapSnapshotter.Options(),
            queue = direct,
            completionHandler = { _, _ -> calls += 1 },
        )
        MapLibreSnapshots.cancel(request)
        MapLibreSnapshots.submit(request)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(0, calls)
        assertFalse(request.isLoading)
    }
}

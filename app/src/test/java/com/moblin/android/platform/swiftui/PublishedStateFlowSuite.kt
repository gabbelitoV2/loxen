package com.moblin.android.platform.swiftui

import androidx.compose.runtime.snapshots.Snapshot
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.RecordingProvider
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PublishedStateFlowSuite {
    private fun readsIn(block: () -> Unit): Set<Any> {
        val reads = mutableSetOf<Any>()
        Snapshot.observe(readObserver = { reads.add(it) }, block = block)
        return reads
    }

    private fun appliedChanges(block: () -> Unit): Set<Any> {
        Snapshot.sendApplyNotifications()
        val changes = mutableSetOf<Any>()
        val handle = Snapshot.registerApplyObserver { changed, _ -> changes.addAll(changed) }
        try {
            block()
            Snapshot.sendApplyNotifications()
        } finally {
            handle.dispose()
        }
        return changes
    }

    @Test
    fun readingTheValueIsASnapshotRead() {
        val flow = PublishedStateFlow("a")
        assertEquals(1, readsIn { flow.value }.size)
    }

    @Test
    fun aWriteIsSeenByTheValueTheFlowAndItsCollectors() = runBlocking {
        val flow = PublishedStateFlow("a")
        val collected = async(Dispatchers.Unconfined) { flow.take(2).toList() }
        flow.value = "b"
        assertEquals(listOf("a", "b"), collected.await())
        assertEquals("b", flow.value)
        assertEquals("b", flow.first())
        flow.emit("c")
        assertEquals("c", flow.value)
        assertTrue(flow.tryEmit("d"))
        assertEquals("d", flow.first())
        assertEquals("d", flow.value)
    }

    @Test
    fun compareAndSetUpdatesTheValueAndTheFlow() = runBlocking {
        val flow = PublishedStateFlow(1)
        assertTrue(flow.compareAndSet(1, 2))
        assertEquals(2, flow.value)
        assertEquals(2, flow.first())
        assertFalse(flow.compareAndSet(1, 3))
        assertEquals(2, flow.value)
        flow.update { it + 1 }
        assertEquals(3, flow.value)
        assertEquals(3, flow.first())
    }

    @Test
    fun onlyAChangedValueInvalidatesReaders() {
        val flow = PublishedStateFlow("a")
        assertEquals(emptySet(), appliedChanges { flow.value = "a" })
        assertEquals(1, appliedChanges { flow.value = "b" }.size)
    }

    @Test
    fun theStreamWizardAndTheRecordingLengthAreObservable() {
        val wizard = CreateStreamWizard()
        assertEquals(1, readsIn { wizard.customSrtUrl }.size)
        assertEquals(1, readsIn { wizard.presenting }.size)
        assertEquals(1, readsIn { wizard.backgroundStreaming }.size)
        assertEquals(1, appliedChanges { wizard.customSrtUrl = "srt://host:1" }.size)
        val recording = RecordingProvider()
        assertEquals(1, readsIn { recording.length }.size)
        assertEquals(1, appliedChanges { recording.length = "0:01" }.size)
    }
}

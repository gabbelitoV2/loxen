package com.moblin.android.various.storages

import com.moblin.android.TemporaryDirectory
import com.moblin.android.waitUntil
import java.io.File
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReplaysStorageSuite {
    @Test
    fun staleDatabaseEntryIsPrunedOnLoad() {
        val directory = TemporaryDirectory()
        val storage = ReplaysStorage(directory = directory.url)
        val stale = storage.createReplay()
        storage.append(replay = stale)
        storage.store()
        val reloaded = ReplaysStorage(directory = directory.url)
        reloaded.load()
        assertFalse(reloaded.database.replays.value.any { it.id == stale.id })
    }

    @Test
    fun replayWithExistingFileSurvivesLoad() {
        val directory = TemporaryDirectory()
        val storage = ReplaysStorage(directory = directory.url)
        val kept = storage.createReplay()
        storage.append(replay = kept)
        storage.store()
        storage.url(replay = kept).writeBytes(ByteArray(0))
        val reloaded = ReplaysStorage(directory = directory.url)
        reloaded.load()
        assertTrue(reloaded.database.replays.value.any { it.id == kept.id })
    }

    @Test
    fun orphanedFileIsRemovedAsynchronously() {
        val directory = TemporaryDirectory()
        val storage = ReplaysStorage(directory = directory.url)
        storage.store()
        val orphan = File(storage.defaultStorageDirectory(), "orphan-${UUID.randomUUID()}.mp4")
        orphan.writeBytes(ByteArray(0))
        val reloaded = ReplaysStorage(directory = directory.url)
        reloaded.load()
        assertTrue(
            waitUntil(timeout = Duration.ofSeconds(2)) {
                !orphan.exists()
            },
        )
    }

    @Test
    fun appendInsertsNewestFirst() {
        val directory = TemporaryDirectory()
        val storage = ReplaysStorage(directory = directory.url)
        val first = storage.createReplay()
        val second = storage.createReplay()
        storage.append(replay = first)
        storage.append(replay = second)
        assertEquals(listOf(second.id, first.id), storage.database.replays.value.map { it.id })
    }

    @Test
    fun isFullAtFiveHundredReplays() {
        val directory = TemporaryDirectory()
        val storage = ReplaysStorage(directory = directory.url)
        repeat(499) {
            storage.append(replay = storage.createReplay())
        }
        assertFalse(storage.isFull())
        storage.append(replay = storage.createReplay())
        assertTrue(storage.isFull())
    }

    @Test
    fun appendWhenFullRemovesOldestReplayAndItsFile() {
        val directory = TemporaryDirectory()
        val storage = ReplaysStorage(directory = directory.url)
        val oldest = storage.createReplay()
        storage.append(replay = oldest)
        storage.url(replay = oldest).writeBytes(ByteArray(0))
        repeat(499) {
            storage.append(replay = storage.createReplay())
        }
        val newest = storage.createReplay()
        storage.append(replay = newest)
        assertEquals(500, storage.database.replays.value.size)
        assertEquals(newest.id, storage.database.replays.value.first().id)
        assertFalse(storage.database.replays.value.any { it.id == oldest.id })
        assertFalse(storage.url(replay = oldest).exists())
    }

    @Test
    fun deleteRemovesOnlyThatReplay() {
        val directory = TemporaryDirectory()
        val storage = ReplaysStorage(directory = directory.url)
        val kept = storage.createReplay()
        val deleted = storage.createReplay()
        storage.append(replay = kept)
        storage.append(replay = deleted)
        storage.delete(id = deleted.id)
        assertEquals(listOf(kept.id), storage.database.replays.value.map { it.id })
    }

    @Test
    fun replaySettingsSurviveStoreAndLoad() {
        val directory = TemporaryDirectory()
        val storage = ReplaysStorage(directory = directory.url)
        val replay = storage.createReplay()
        replay.duration = 31.5
        replay.start = 12.0
        replay.stop = 25.0
        storage.append(replay = replay)
        storage.store()
        storage.url(replay = replay).writeBytes(ByteArray(0))
        val reloaded = ReplaysStorage(directory = directory.url)
        reloaded.load()
        val loaded = reloaded.database.replays.value.first()
        assertEquals(1, reloaded.database.replays.value.size)
        assertEquals(replay.id, loaded.id)
        assertEquals(31.5, loaded.duration)
        assertEquals(12.0, loaded.start)
        assertEquals(25.0, loaded.stop)
    }

    @Test
    fun loadOrderIsPreserved() {
        val directory = TemporaryDirectory()
        val storage = ReplaysStorage(directory = directory.url)
        repeat(3) {
            val replay = storage.createReplay()
            storage.append(replay = replay)
            storage.url(replay = replay).writeBytes(ByteArray(0))
        }
        storage.store()
        val reloaded = ReplaysStorage(directory = directory.url)
        reloaded.load()
        assertEquals(
            storage.database.replays.value.map { it.id },
            reloaded.database.replays.value.map { it.id },
        )
    }

    @Test
    fun corruptDatabaseLoadsEmpty() {
        val directory = TemporaryDirectory()
        val storage = ReplaysStorage(directory = directory.url)
        val url = File(File(directory.url, "Database"), "replays")
        url.parentFile?.mkdirs()
        url.writeText("not json")
        storage.load()
        assertTrue(storage.database.replays.value.isEmpty())
    }

    @Test
    fun missingDatabaseLoadsEmpty() {
        val directory = TemporaryDirectory()
        val storage = ReplaysStorage(directory = directory.url)
        storage.load()
        assertTrue(storage.database.replays.value.isEmpty())
    }

    @Test
    fun cleanupKeepsFilesOfKnownReplays() {
        val directory = TemporaryDirectory()
        val storage = ReplaysStorage(directory = directory.url)
        val kept = storage.createReplay()
        storage.append(replay = kept)
        storage.store()
        storage.url(replay = kept).writeBytes(ByteArray(0))
        val orphan = File(storage.defaultStorageDirectory(), "orphan-${UUID.randomUUID()}.mp4")
        orphan.writeBytes(ByteArray(0))
        val reloaded = ReplaysStorage(directory = directory.url)
        reloaded.load()
        assertTrue(
            waitUntil(timeout = Duration.ofSeconds(2)) {
                !orphan.exists()
            },
        )
        assertTrue(reloaded.url(replay = kept).exists())
    }
}

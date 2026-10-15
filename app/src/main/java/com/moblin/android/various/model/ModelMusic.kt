package com.moblin.android.various.model

import com.moblin.android.platform.log.Log
import com.moblin.android.localized
import com.moblin.android.various.storages.SimpleStringStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.net.URI
import java.util.concurrent.Executors

private const val TAG = "ModelMusic"

fun appleMusicSongId(url: String): String? {
    val uri = runCatching { URI(url) }.getOrNull() ?: return null
    val pathComponents = uri.path.orEmpty().split("/").filter { it.isNotEmpty() }
    if (pathComponents.contains("song")) {
        return pathComponents.lastOrNull()
    }
    return uri.rawQuery
        ?.split("&")
        ?.mapNotNull { item ->
            val index = item.indexOf('=')
            if (index < 0) null else item.substring(0, index) to item.substring(index + 1)
        }
        ?.firstOrNull { it.first == "i" }
        ?.second
}

private sealed class Action {
    data class Add(
        val title: String,
        val user: String?,
        val requestId: String?,
        val onCompleted: (MusicAddResult) -> Unit,
    ) : Action()

    data class Find(val title: String, val onCompleted: (Any?) -> Unit) : Action()

    data object Play : Action()

    data object Pause : Action()

    data class Next(val count: Int) : Action()

    data class Previous(val count: Int) : Action()

    data class Status(val onCompleted: (MusicStatus) -> Unit) : Action()

    data object Clear : Action()

    data class Remove(val entryId: String) : Action()

    data class Select(val entryId: String) : Action()

    data object Load : Action()
}

internal data class MusicMetadata(
    val addedBy: String?,
    val requestId: String?,
)

private data class StoredMusicEntry(
    val songId: String,
    val title: String,
    val artist: String?,
    val duration: Int?,
    val addedBy: String?,
    val requestId: String?,
)

enum class MusicQueueState {
    notLoaded,
    empty,
    loaded,
}

private val musicPlayer: Any? = null

private var isActionRunning = false

private val actions: ArrayDeque<Action> = ArrayDeque()

private var playRequested = true

private val playlistQueue =
    CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

private val playlistStorage = SimpleStringStorage(key = "musicPlaylist")

private val mainScope = CoroutineScope(Dispatchers.Main)

private suspend fun loadPlaylist(): List<StoredMusicEntry> = emptyList()

private fun storePlaylist(playlist: List<StoredMusicEntry>) {
    Unit
}

private fun makeSong(storedEntry: StoredMusicEntry): Any? = null

fun musicDuration(song: Any?): Int? = null

private fun musicSong(entry: Any?): Any? = null

data class MusicPlaylistSong(
    val id: String,
    val title: String,
    val artist: String?,
    val duration: Int?,
    val addedBy: String?,
)

open class MusicPlaylist {
    open val queue: MutableStateFlow<Any?> = MutableStateFlow(null)

    open val state: MutableStateFlow<MusicQueueState> = MutableStateFlow(MusicQueueState.notLoaded)

    internal var metadata: MutableMap<String, MusicMetadata> = mutableMapOf()

    open fun songs(): List<MusicPlaylistSong> = emptyList()

    open fun currentIndex(): Int? = null
}

data class MusicStatusSong(
    val id: String,
    val title: String,
    val artist: String?,
    val duration: Int?,
    val addedBy: String?,
)

data class MusicStatus(
    val playing: Boolean,
    val currentSongIndex: Int?,
    val songs: List<MusicStatusSong>,
)

sealed class MusicAddResult {
    data class Added(val song: String) : MusicAddResult()

    data object AlreadyAdded : MusicAddResult()

    data object SongNotFound : MusicAddResult()

    data object Failed : MusicAddResult()
}

fun Model.addMusic(
    title: String,
    user: String?,
    requestId: String? = null,
    onCompleted: (MusicAddResult) -> Unit,
) {
    actions.addLast(Action.Add(title, user, requestId, onCompleted))
    tryRunNextAction()
}

fun Model.makeMusicAddedMessage(song: String, user: String): String {
    return localized("$song added to the queue by $user.")
}

fun Model.findMusic(title: String, onCompleted: (Any?) -> Unit) {
    actions.addLast(Action.Find(title, onCompleted))
    tryRunNextAction()
}

fun Model.playMusic() {
    actions.addLast(Action.Play)
    tryRunNextAction()
}

fun Model.pauseMusic() {
    actions.addLast(Action.Pause)
    tryRunNextAction()
}

fun Model.nextMusic(count: Int) {
    actions.addLast(Action.Next(count))
    tryRunNextAction()
}

fun Model.previousMusic(count: Int) {
    actions.addLast(Action.Previous(count))
    tryRunNextAction()
}

fun Model.statusMusic(onCompleted: (MusicStatus) -> Unit) {
    actions.addLast(Action.Status(onCompleted))
    tryRunNextAction()
}

fun Model.clearMusic() {
    actions.addLast(Action.Clear)
    tryRunNextAction()
}

fun Model.removeMusic(entryId: String) {
    actions.addLast(Action.Remove(entryId))
    tryRunNextAction()
}

fun Model.selectMusic(entryId: String) {
    actions.addLast(Action.Select(entryId))
    tryRunNextAction()
}

fun Model.loadMusic() {
    actions.addLast(Action.Load)
    tryRunNextAction()
}

private fun Model.setMusicQueue(entries: List<Any?>, startingAt: Any? = null) {
    Unit
}

private fun Model.storeMusicQueue() {
    Unit
}

private fun Model.tryRunNextAction() {
    if (isActionRunning) {
        return
    }
    val action = actions.removeFirstOrNull() ?: return
    isActionRunning = true
    Log.d(TAG, "music: Running action: $action")
    val model = this
    mainScope.launch {
        when (action) {
            is Action.Find, Action.Clear -> Unit
            else -> {
                try {
                    model.loadQueueIfNeeded()
                } catch (e: Throwable) {
                    Log.i(TAG, "music: Load queue failed: $e")
                }
            }
        }
        try {
            when (action) {
                is Action.Add ->
                    model.addAction(
                        action.title,
                        action.user,
                        action.requestId,
                        action.onCompleted,
                    )
                is Action.Find -> model.findAction(action.title, action.onCompleted)
                Action.Play -> model.playAction()
                Action.Pause -> model.pauseAction()
                is Action.Next -> model.nextAction(action.count)
                is Action.Previous -> model.previousAction(action.count)
                is Action.Status -> model.statusAction(action.onCompleted)
                Action.Clear -> model.clearAction()
                is Action.Remove -> model.removeAction(action.entryId)
                is Action.Select -> model.selectAction(action.entryId)
                Action.Load -> Unit
            }
        } catch (e: Throwable) {
            model.makeErrorToast(
                title = localized("Music player error"),
                subTitle = e.message ?: e.toString(),
            )
        }
        isActionRunning = false
        model.tryRunNextAction()
    }
}

private suspend fun Model.loadQueueIfNeeded() {
    Unit
}

private suspend fun Model.addAction(
    title: String,
    user: String?,
    requestId: String?,
    onCompleted: (MusicAddResult) -> Unit,
) {
    Unit
}

private suspend fun Model.workaroundAppleMusicTransientQueueEntriesBug() {
    Unit
}

private suspend fun Model.findAction(title: String, onCompleted: (Any?) -> Unit) {
    Unit
}

private suspend fun Model.findSong(title: String): Any? =
    Unit
private suspend fun Model.playAction() {
    Unit
}

private fun Model.pauseAction() {
    Unit
}

private suspend fun Model.nextAction(count: Int) {
    Unit
}

private suspend fun Model.previousAction(count: Int) {
    Unit
}

private fun Model.clearAction() {
    Unit
}

private suspend fun Model.removeAction(entryId: String) {
    Unit
}

private suspend fun Model.selectAction(entryId: String) {
    Unit
}

private fun Model.statusAction(onCompleted: (MusicStatus) -> Unit) {
    Unit
}

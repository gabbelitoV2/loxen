package com.moblin.android.various.model

import android.util.Log
import com.moblin.android.localized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "ModelMusic"

private sealed class Action {
    data class Add(val title: String, val onCompleted: (MusicAddResult) -> Unit) : Action()

    data object Play : Action()

    data object Pause : Action()

    data class Next(val count: Int) : Action()

    data class Previous(val count: Int) : Action()

    data class Status(val onCompleted: (MusicStatus) -> Unit) : Action()
}

private val songs: MutableList<Any> = mutableListOf()

private val musicPlayer: Any? = null

private var isActionRunning = false

private val actions: ArrayDeque<Action> = ArrayDeque()

private var playRequested = true

private val mainScope = CoroutineScope(Dispatchers.Main)

data class MusicStatusSong(
    val title: String,
)

data class MusicStatus(
    val playing: Boolean,
    val currentSongIndex: Int?,
    val songs: List<MusicStatusSong>,
)

sealed class MusicAddResult {
    data class Added(val song: String) : MusicAddResult()

    data object SongNotFound : MusicAddResult()
}

fun Model.addMusic(title: String, onCompleted: (MusicAddResult) -> Unit) {
    actions.addLast(Action.Add(title, onCompleted))
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

private fun Model.tryRunNextAction() {
    if (isActionRunning) {
        return
    }
    val action = actions.removeFirstOrNull() ?: return
    isActionRunning = true
    Log.d(TAG, "music: Running action: $action")
    val model = this
    mainScope.launch {
        try {
            when (action) {
                is Action.Add -> model.addAction(action.title, action.onCompleted)
                Action.Play -> model.playAction()
                Action.Pause -> model.pauseAction()
                is Action.Next -> model.nextAction(action.count)
                is Action.Previous -> model.previousAction(action.count)
                is Action.Status -> model.statusAction(action.onCompleted)
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

private suspend fun Model.addAction(title: String, onCompleted: (MusicAddResult) -> Unit) {
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

private suspend fun Model.statusAction(onCompleted: (MusicStatus) -> Unit) {
    Unit
}

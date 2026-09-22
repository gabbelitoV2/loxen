package com.moblin.android.various.model

import com.moblin.android.various.ChatPost
import com.moblin.android.various.settings.SettingsWidgetGenericScoreboard
import com.moblin.android.various.settings.SettingsWidgetPadelScoreboard
import com.moblin.android.various.settings.SettingsZoomPreset
import com.moblin.android.various.storages.ThermalState
import java.util.UUID

fun Model.isWatchReachable(): Boolean {
    return false
}

private fun Model.sendMessageToWatch(
    type: Any,
    data: Any,
    replyHandler: ((Map<String, Any>) -> Unit)? = null,
    errorHandler: ((Throwable) -> Unit)? = null,
) {
    Unit
}

fun Model.sendInitToWatch() {
    Unit
}

fun Model.sendSpeedAndTotalToWatch(speedAndTotal: String) {
    Unit
}

fun Model.sendRecordingLengthToWatch(recordingLength: String) {
    Unit
}

fun Model.sendAudioLevelToWatch(audioLevel: Float) {
    Unit
}

fun Model.sendThermalStateToWatch(thermalState: ThermalState) {
    Unit
}

fun Model.sendViewerCountWatch() {
    Unit
}

fun Model.sendUpdatePadelScoreboardToWatch(id: UUID, padel: SettingsWidgetPadelScoreboard) {
    Unit
}

fun Model.sendUpdateGenericScoreboardToWatch(id: UUID, generic: SettingsWidgetGenericScoreboard) {
    Unit
}

fun Model.sendRemoveScoreboardToWatch(id: UUID) {
    Unit
}

fun Model.sendScoreboardPlayersToWatch() {
    Unit
}

private fun Model.resetWorkoutStats() {
    Unit
}

private fun Model.enqueueWatchChatPost(post: ChatPost) {
    Unit
}

fun Model.trySendNextChatPostToWatch() {
    Unit
}

fun Model.sendChatMessageToWatch(post: ChatPost) {
    Unit
}

fun Model.sendPreviewToWatch(image: ByteArray) {
    Unit
}

fun Model.sendZoomToWatch(x: Float) {
    Unit
}

fun Model.sendZoomPresetsToWatch(presets: List<SettingsZoomPreset>) {
    Unit
}

fun Model.sendZoomPresetToWatch() {
    Unit
}

private fun Model.sendScenesToWatch(scenes: List<Any>) {
    Unit
}

private fun Model.sendScenesToWatchLocal() {
    Unit
}

private fun Model.sendScenesToWatchRemoteControl() {
    Unit
}

fun Model.sendSceneToWatch(id: UUID) {
    Unit
}

fun Model.sendSettingsToWatch() {
    Unit
}

fun Model.sendIsLiveToWatch(isLive: Any) {
    Unit
}

fun Model.sendIsRecordingToWatch(isRecording: Any) {
    Unit
}

fun Model.sendIsMutedToWatch(isMuteOn: Any) {
    Unit
}

fun Model.sendRemoteControlAssistantStatusToWatch() {
    Unit
}

fun Model.isWatchRemoteControl(): Boolean {
    return false
}

fun Model.isWatchLocal(): Boolean {
    return false
}

fun Model.session(
    session: Any,
    activationState: Any,
    error: Throwable?,
) {
    Unit
}

fun Model.sessionDidBecomeInactive(session: Any) {
    Unit
}

fun Model.sessionDidDeactivate(session: Any) {
    Unit
}

fun Model.sessionReachabilityDidChange(session: Any) {
    Unit
}

private fun Model.makePng(uiImage: Any): ByteArray {
    TODO()
}

private fun Model.handleGetImage(data: Any, replyHandler: (Map<String, Any>) -> Unit) {
    Unit
}

private fun Model.handleSetIsLive(data: Any) {
    Unit
}

private fun Model.handleSetIsRecording(data: Any) {
    Unit
}

private fun Model.handleSetIsMuted(data: Any) {
    Unit
}

private fun Model.handleSkipCurrentChatTextToSpeechMessage() {
    Unit
}

private fun Model.handleSetZoomMessage(data: Any) {
    Unit
}

private fun Model.handleSetZoomPresetMessage(data: Any) {
    Unit
}

private fun Model.handleSetSceneMessage(data: Any) {
    Unit
}

private fun Model.handleUpdateWorkoutStats(data: Any) {
    Unit
}

private fun Model.handleUpdatePadelScoreboardFromWatch(data: Any) {
    Unit
}

private fun Model.handleUpdateGenericScoreboardFromWatch(data: Any) {
    Unit
}

private fun Model.handleCreateStreamMarker() {
    Unit
}

private fun Model.handleInstantReplay(data: Any) {
    Unit
}

private fun Model.handleSaveReplay() {
    Unit
}

fun Model.session(
    session: Any,
    didReceiveMessage: Map<String, Any>,
    replyHandler: (Map<String, Any>) -> Unit,
) {
    Unit
}

fun Model.session(session: Any, didReceiveMessage: Map<String, Any>) {
    Unit
}

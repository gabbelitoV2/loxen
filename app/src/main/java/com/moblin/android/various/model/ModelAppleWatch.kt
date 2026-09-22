package com.moblin.android.various.model

import com.moblin.android.various.ChatPost
import com.moblin.android.various.settings.SettingsWidgetGenericScoreboard
import com.moblin.android.various.settings.SettingsWidgetPadelScoreboard
import com.moblin.android.various.settings.SettingsZoomPreset
import com.moblin.android.various.storages.ThermalState
import java.util.UUID

fun Model.isWatchReachable(): Boolean {
    TODO("no Android counterpart for WatchConnectivity")
}

private fun Model.sendMessageToWatch(
    type: Any,
    data: Any,
    replyHandler: ((Map<String, Any>) -> Unit)? = null,
    errorHandler: ((Throwable) -> Unit)? = null,
) {
    TODO("no Android counterpart for WatchConnectivity")
}

fun Model.sendInitToWatch() {
    TODO("no Android counterpart for WatchConnectivity")
}

fun Model.sendSpeedAndTotalToWatch(speedAndTotal: String) {
    TODO("no Android counterpart for WatchConnectivity")
}

fun Model.sendRecordingLengthToWatch(recordingLength: String) {
    TODO("no Android counterpart for WatchConnectivity")
}

fun Model.sendAudioLevelToWatch(audioLevel: Float) {
    TODO("no Android counterpart for WatchConnectivity")
}

fun Model.sendThermalStateToWatch(thermalState: ThermalState) {
    TODO("no Android counterpart for WatchConnectivity")
}

fun Model.sendViewerCountWatch() {
    TODO("no Android counterpart for WatchConnectivity")
}

fun Model.sendUpdatePadelScoreboardToWatch(id: UUID, padel: SettingsWidgetPadelScoreboard) {
    TODO("no Android counterpart for WatchConnectivity")
}

fun Model.sendUpdateGenericScoreboardToWatch(id: UUID, generic: SettingsWidgetGenericScoreboard) {
    TODO("no Android counterpart for WatchConnectivity")
}

fun Model.sendRemoveScoreboardToWatch(id: UUID) {
    TODO("no Android counterpart for WatchConnectivity")
}

fun Model.sendScoreboardPlayersToWatch() {
    TODO("no Android counterpart for WatchConnectivity")
}

private fun Model.resetWorkoutStats() {
    TODO("no Android counterpart for WatchConnectivity")
}

private fun Model.enqueueWatchChatPost(post: ChatPost) {
    TODO("no Android counterpart for WatchConnectivity")
}

fun Model.trySendNextChatPostToWatch() {
    TODO("no Android counterpart for WatchConnectivity")
}

fun Model.sendChatMessageToWatch(post: ChatPost) {
    TODO("no Android counterpart for WatchConnectivity")
}

fun Model.sendPreviewToWatch(image: ByteArray) {
    TODO("no Android counterpart for WatchConnectivity")
}

fun Model.sendZoomToWatch(x: Float) {
    TODO("no Android counterpart for WatchConnectivity")
}

fun Model.sendZoomPresetsToWatch(presets: List<SettingsZoomPreset>) {
    TODO("no Android counterpart for WatchConnectivity")
}

fun Model.sendZoomPresetToWatch() {
    TODO("no Android counterpart for WatchConnectivity")
}

private fun Model.sendScenesToWatch(scenes: List<Any>) {
    TODO("no Android counterpart for WatchConnectivity")
}

private fun Model.sendScenesToWatchLocal() {
    TODO("no Android counterpart for WatchConnectivity")
}

private fun Model.sendScenesToWatchRemoteControl() {
    TODO("no Android counterpart for WatchConnectivity")
}

fun Model.sendSceneToWatch(id: UUID) {
    TODO("no Android counterpart for WatchConnectivity")
}

fun Model.sendSettingsToWatch() {
    TODO("no Android counterpart for WatchConnectivity")
}

fun Model.sendIsLiveToWatch(isLive: Any) {
    TODO("no Android counterpart for WatchConnectivity")
}

fun Model.sendIsRecordingToWatch(isRecording: Any) {
    TODO("no Android counterpart for WatchConnectivity")
}

fun Model.sendIsMutedToWatch(isMuteOn: Any) {
    TODO("no Android counterpart for WatchConnectivity")
}

fun Model.sendRemoteControlAssistantStatusToWatch() {
    TODO("no Android counterpart for WatchConnectivity")
}

fun Model.isWatchRemoteControl(): Boolean {
    TODO("no Android counterpart for WatchConnectivity")
}

fun Model.isWatchLocal(): Boolean {
    TODO("no Android counterpart for WatchConnectivity")
}

fun Model.session(
    session: Any,
    activationState: Any,
    error: Throwable?,
) {
    TODO("no Android counterpart for WatchConnectivity")
}

fun Model.sessionDidBecomeInactive(session: Any) {
    TODO("no Android counterpart for WatchConnectivity")
}

fun Model.sessionDidDeactivate(session: Any) {
    TODO("no Android counterpart for WatchConnectivity")
}

fun Model.sessionReachabilityDidChange(session: Any) {
    TODO("no Android counterpart for WatchConnectivity")
}

private fun Model.makePng(uiImage: Any): ByteArray {
    TODO("no Android counterpart for WatchConnectivity")
}

private fun Model.handleGetImage(data: Any, replyHandler: (Map<String, Any>) -> Unit) {
    TODO("no Android counterpart for WatchConnectivity")
}

private fun Model.handleSetIsLive(data: Any) {
    TODO("no Android counterpart for WatchConnectivity")
}

private fun Model.handleSetIsRecording(data: Any) {
    TODO("no Android counterpart for WatchConnectivity")
}

private fun Model.handleSetIsMuted(data: Any) {
    TODO("no Android counterpart for WatchConnectivity")
}

private fun Model.handleSkipCurrentChatTextToSpeechMessage() {
    TODO("no Android counterpart for WatchConnectivity")
}

private fun Model.handleSetZoomMessage(data: Any) {
    TODO("no Android counterpart for WatchConnectivity")
}

private fun Model.handleSetZoomPresetMessage(data: Any) {
    TODO("no Android counterpart for WatchConnectivity")
}

private fun Model.handleSetSceneMessage(data: Any) {
    TODO("no Android counterpart for WatchConnectivity")
}

private fun Model.handleUpdateWorkoutStats(data: Any) {
    TODO("no Android counterpart for WatchConnectivity")
}

private fun Model.handleUpdatePadelScoreboardFromWatch(data: Any) {
    TODO("no Android counterpart for WatchConnectivity")
}

private fun Model.handleUpdateGenericScoreboardFromWatch(data: Any) {
    TODO("no Android counterpart for WatchConnectivity")
}

private fun Model.handleCreateStreamMarker() {
    TODO("no Android counterpart for WatchConnectivity")
}

private fun Model.handleInstantReplay(data: Any) {
    TODO("no Android counterpart for WatchConnectivity")
}

private fun Model.handleSaveReplay() {
    TODO("no Android counterpart for WatchConnectivity")
}

fun Model.session(
    session: Any,
    didReceiveMessage: Map<String, Any>,
    replyHandler: (Map<String, Any>) -> Unit,
) {
    TODO("no Android counterpart for WatchConnectivity")
}

fun Model.session(session: Any, didReceiveMessage: Map<String, Any>) {
    TODO("no Android counterpart for WatchConnectivity")
}

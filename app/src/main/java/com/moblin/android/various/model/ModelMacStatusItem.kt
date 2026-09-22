package com.moblin.android.various.model

import android.util.Log
import com.moblin.android.localized
import java.util.WeakHashMap

private const val TAG = "Model"

interface MacStatusItemDelegate

interface MacStatusItem {
    fun start(iconData: ByteArray, delegate: MacStatusItemDelegate)
    fun update(
        isLive: Boolean,
        isRecording: Boolean,
        statusTitle: String,
        streamTitle: String,
        recordingTitle: String,
    )
    fun stop()
}

private val macStatusItems = WeakHashMap<Model, MacStatusItem?>()

var Model.macStatusItem: MacStatusItem?
    get() = macStatusItems[this]
    set(value) {
        macStatusItems[this] = value
    }

fun Model.setupMacStatusItem() {
    Log.i(TAG, "mac-status-item: Failed to load helper bundle")
    TODO("no Android counterpart for macCatalyst status item")
}

fun Model.updateMacStatusItem() {
    TODO("no Android counterpart for macCatalyst status item")
}

fun Model.stopMacStatusItem() {
    TODO("no Android counterpart for macCatalyst status item")
}

fun Model.macStatusItemToggleStream() {
    TODO("no Android counterpart for macCatalyst status item")
}

fun Model.macStatusItemToggleRecording() {
    TODO("no Android counterpart for macCatalyst status item")
}

private fun Model.makeMacStatusItemStatusTitle(): String {
    val titles = mutableListOf<String>()
    if (isLive) {
        titles.add(localized("Live"))
    }
    if (isRecording) {
        titles.add(localized("Recording"))
    }
    if (titles.isEmpty()) {
        titles.add(localized("Idle"))
    }
    return titles.joinToString(", ")
}

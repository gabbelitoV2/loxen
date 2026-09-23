package com.moblin.android.platform.swiftui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.toMutableStateMap
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class Published<T>(initial: T) : ReadWriteProperty<Any?, T> {
    private val state = mutableStateOf(initial)

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = state.value

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        state.value = value
    }
}

class PublishedList<T>(initial: List<T> = emptyList()) : ReadWriteProperty<Any?, MutableList<T>> {
    private val state = mutableStateOf(initial.toMutableStateList())

    override fun getValue(thisRef: Any?, property: KProperty<*>): MutableList<T> = state.value

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: MutableList<T>) {
        state.value = value as? SnapshotStateList<T> ?: value.toMutableStateList()
    }
}

class PublishedMap<K, V>(initial: Map<K, V> = emptyMap()) : ReadWriteProperty<Any?, MutableMap<K, V>> {
    private val state = mutableStateOf(initial.toList().toMutableStateMap())

    override fun getValue(thisRef: Any?, property: KProperty<*>): MutableMap<K, V> = state.value

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: MutableMap<K, V>) {
        state.value = value as? SnapshotStateMap<K, V> ?: value.toList().toMutableStateMap()
    }
}

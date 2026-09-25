package com.moblin.android.platform.core

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

enum class NSKeyValueObservingOptions {
    new,
    old,
    initial,
}

class NSKeyValueObservedChange<Value>(
    val newValue: Value?,
    val oldValue: Value?,
)

class NSKeyValueObservation internal constructor(private val onInvalidate: () -> Unit) {
    private val valid = AtomicBoolean(true)

    val isValid: Boolean
        get() = valid.get()

    fun invalidate() {
        if (valid.compareAndSet(true, false)) {
            onInvalidate()
        }
    }

    private class Holder : ReadWriteProperty<Any?, NSKeyValueObservation?> {
        @Volatile
        private var observation: NSKeyValueObservation? = null

        override fun getValue(thisRef: Any?, property: KProperty<*>): NSKeyValueObservation? = observation

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: NSKeyValueObservation?) {
            val previous = observation
            observation = value
            if (previous !== value) {
                previous?.invalidate()
            }
        }
    }

    companion object {
        fun holder(): ReadWriteProperty<Any?, NSKeyValueObservation?> = Holder()
    }
}

internal class KeyValueObservers<Object : Any>(private val target: Object) {
    private class Registration<Object, Value>(
        val key: String,
        val options: Set<NSKeyValueObservingOptions>,
        val changeHandler: (Object, NSKeyValueObservedChange<Value>) -> Unit,
    ) {
        lateinit var observation: NSKeyValueObservation
    }

    private val registrations = CopyOnWriteArrayList<Registration<Object, *>>()

    fun <Value> observe(
        keyPath: KProperty1<Object, Value>,
        options: Set<NSKeyValueObservingOptions>,
        changeHandler: (Object, NSKeyValueObservedChange<Value>) -> Unit,
    ): NSKeyValueObservation {
        val registration = Registration(keyPath.name, options, changeHandler)
        val observation = NSKeyValueObservation { registrations.remove(registration) }
        registration.observation = observation
        registrations.add(registration)
        if (NSKeyValueObservingOptions.initial in options) {
            changeHandler(target, change(options, oldValue = null, newValue = keyPath.get(target)))
        }
        return observation
    }

    val isObserved: Boolean
        get() = registrations.isNotEmpty()

    fun <Value> didChangeValue(keyPath: KProperty1<Object, Value>, oldValue: Value, newValue: Value) {
        if (oldValue == newValue || registrations.isEmpty()) {
            return
        }
        val key = keyPath.name
        for (registration in registrations) {
            if (registration.key != key || !registration.observation.isValid) {
                continue
            }
            @Suppress("UNCHECKED_CAST")
            val typed = registration as Registration<Object, Value>
            typed.changeHandler(target, change(typed.options, oldValue, newValue))
        }
    }

    private fun <Value> change(
        options: Set<NSKeyValueObservingOptions>,
        oldValue: Value?,
        newValue: Value?,
    ): NSKeyValueObservedChange<Value> {
        return NSKeyValueObservedChange(
            newValue = if (NSKeyValueObservingOptions.new in options) newValue else null,
            oldValue = if (NSKeyValueObservingOptions.old in options) oldValue else null,
        )
    }
}

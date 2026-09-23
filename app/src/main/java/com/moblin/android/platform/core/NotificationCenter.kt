package com.moblin.android.platform.core

import android.util.Log
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "MoblinNotifications"

class Notification(val name: String, val obj: Any?, val userInfo: Map<String, Any?> = emptyMap()) {
    val `object`: Any?
        get() = obj
}

class NotificationCenter {
    private class Registration(
        observer: Any,
        val name: String?,
        obj: Any?,
        val scope: CoroutineScope?,
        val block: (Notification) -> Unit,
        val token: Any? = null,
    ) {
        val observer = WeakReference(observer)
        val obj: WeakReference<Any>? = obj?.let { WeakReference(it) }
        val hasObject = obj != null
    }

    private val lock = Any()
    private var registrations: List<Registration> = emptyList()

    fun addObserver(observer: Any, name: String, obj: Any?, block: (Notification) -> Unit) {
        add(Registration(observer, name, obj, null, block))
    }

    fun addObserver(
        forName: String?,
        obj: Any?,
        queue: CoroutineDispatcher?,
        using: (Notification) -> Unit,
    ): Any {
        val token = Any()
        add(Registration(token, forName, obj, queue?.let { CoroutineScope(it) }, using, token))
        return token
    }

    fun removeObserver(observer: Any, name: String? = null, obj: Any? = null) {
        synchronized(lock) {
            registrations = registrations.filterNot { registration ->
                val registeredObserver = registration.observer.get()
                val observerMatches = registeredObserver == null || registeredObserver === observer
                observerMatches &&
                    (registeredObserver == null ||
                        ((name == null || registration.name == name) &&
                            (obj == null || registration.obj?.get() === obj)))
            }
        }
    }

    fun post(name: String, obj: Any? = null, userInfo: Map<String, Any?> = emptyMap()) {
        post(Notification(name, obj, userInfo))
    }

    fun post(notification: Notification) {
        val matching = synchronized(lock) {
            registrations.filter { registration ->
                registration.observer.get() != null &&
                    (registration.name == null || registration.name == notification.name) &&
                    (!registration.hasObject || registration.obj?.get()?.let { it === notification.obj } == true)
            }
        }
        for (registration in matching) {
            val scope = registration.scope
            if (scope == null) {
                deliver(registration, notification)
            } else {
                scope.launch {
                    deliver(registration, notification)
                }
            }
        }
    }

    private fun add(registration: Registration) {
        synchronized(lock) {
            registrations = registrations.filter { it.observer.get() != null } + registration
        }
    }

    private fun deliver(registration: Registration, notification: Notification) {
        try {
            registration.block(notification)
        } catch (error: Throwable) {
            Log.e(TAG, "Observer of ${notification.name} failed", error)
        }
    }

    companion object {
        val default = NotificationCenter()
    }
}

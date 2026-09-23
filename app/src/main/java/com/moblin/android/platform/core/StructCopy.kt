package com.moblin.android.platform.core

import android.util.Log
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "MoblinStructCopy"

private class StructCopier(val constructor: Constructor<*>?, val fields: List<Field>)

private val copiers = ConcurrentHashMap<Class<*>, StructCopier>()
private val failedClasses = ConcurrentHashMap.newKeySet<Class<*>>()

private val unsafeAllocator: ((Class<*>) -> Any)? by lazy {
    try {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val field = unsafeClass.getDeclaredField("theUnsafe")
        field.isAccessible = true
        val unsafe = field.get(null)
        val allocateInstance = unsafeClass.getMethod("allocateInstance", Class::class.java)
        val allocator: (Class<*>) -> Any = { clazz -> allocateInstance.invoke(unsafe, clazz)!! }
        allocator
    } catch (error: Throwable) {
        Log.w(TAG, "No instance allocator: $error")
        null
    }
}

@Suppress("UNCHECKED_CAST")
fun <T : Any> structCopy(value: T): T {
    when (value) {
        is String, is Number, is Boolean, is Char, is Enum<*>, is Unit -> return value
        is ByteArray -> return value.copyOf() as T
        is ShortArray -> return value.copyOf() as T
        is IntArray -> return value.copyOf() as T
        is LongArray -> return value.copyOf() as T
        is FloatArray -> return value.copyOf() as T
        is DoubleArray -> return value.copyOf() as T
        is BooleanArray -> return value.copyOf() as T
        is CharArray -> return value.copyOf() as T
        is Array<*> -> return (value as Array<Any?>).copyOf() as T
        is MutableList<*> -> return ArrayList(value) as T
        is MutableSet<*> -> return LinkedHashSet(value) as T
        is MutableMap<*, *> -> return LinkedHashMap(value) as T
        is List<*>, is Set<*>, is Map<*, *> -> return value
    }
    val clazz = value.javaClass
    if (failedClasses.contains(clazz)) {
        return value
    }
    return try {
        val copier = copiers.getOrPut(clazz) { makeCopier(clazz) }
        val copy = copier.constructor?.newInstance() ?: unsafeAllocator?.invoke(clazz)
        if (copy == null) {
            failedClasses.add(clazz)
            Log.w(TAG, "Cannot copy ${clazz.name}, sharing the instance")
            return value
        }
        for (field in copier.fields) {
            field.set(copy, field.get(value))
        }
        copy as T
    } catch (error: Throwable) {
        failedClasses.add(clazz)
        Log.w(TAG, "Cannot copy ${clazz.name}, sharing the instance: $error")
        value
    }
}

private fun makeCopier(clazz: Class<*>): StructCopier {
    val constructor = try {
        clazz.getDeclaredConstructor().also { it.isAccessible = true }
    } catch (error: NoSuchMethodException) {
        null
    }
    val fields = mutableListOf<Field>()
    var current: Class<*>? = clazz
    while (current != null && current != Any::class.java) {
        for (field in current.declaredFields) {
            if (Modifier.isStatic(field.modifiers)) {
                continue
            }
            field.isAccessible = true
            fields.add(field)
        }
        current = current.superclass
    }
    return StructCopier(constructor, fields)
}

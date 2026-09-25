package com.moblin.android.platform.uikit

open class UISlider {
    open var minimumValue: Float = 0f
    open var maximumValue: Float = 1f
    private var storedValue = 0f

    open var value: Float
        get() = storedValue
        set(newValue) {
            storedValue = clamp(newValue)
        }

    protected fun clamp(value: Float): Float {
        if (value.isNaN()) {
            return minimumValue
        }
        return maxOf(minimumValue, minOf(value, maximumValue))
    }
}

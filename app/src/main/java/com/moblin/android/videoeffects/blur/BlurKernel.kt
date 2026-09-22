package com.moblin.android.videoeffects.blur

class BlurKernel {
    companion object {
        fun process(inputs: List<Any>?, arguments: Map<String, Any>?, output: Any?) {
            val radius = arguments?.get("radius") as? Float ?: return
            val sigma = radius
            Unit
        }
    }
}

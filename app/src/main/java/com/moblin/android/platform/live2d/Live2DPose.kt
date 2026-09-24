package com.moblin.android.platform.live2d

internal class Live2DPoseDescriptor(
    val isParam: Boolean,
    val id: String,
    val min: Float,
    val max: Float,
    val default: Float,
)

internal class Live2DPoseMap(val descriptors: List<Live2DPoseDescriptor>) {
    private val paramIndexes = HashMap<String, Int>()
    private val partIndexes = HashMap<String, Int>()

    init {
        for ((index, descriptor) in descriptors.withIndex()) {
            val indexes = if (descriptor.isParam) paramIndexes else partIndexes
            if (indexes.put(descriptor.id, index) != null) {
                throw AyagamiError("Duplicate ID")
            }
        }
    }

    fun paramIndex(id: String): Int? {
        return paramIndexes[id]
    }

    companion object {
        fun fromModel(model: Live2DModelData): Live2DPoseMap {
            val descriptors = ArrayList<Live2DPoseDescriptor>()
            for (i in 0 until model.paramCount) {
                descriptors.add(
                    Live2DPoseDescriptor(
                        isParam = true,
                        id = model.paramIds[i],
                        min = model.paramMin[i],
                        max = model.paramMax[i],
                        default = model.paramDefault[i],
                    )
                )
            }
            for (i in 0 until model.partCount) {
                descriptors.add(
                    Live2DPoseDescriptor(isParam = false, id = model.partIds[i], min = 0f, max = 1f, default = 1f)
                )
            }
            return Live2DPoseMap(descriptors)
        }
    }
}

internal class Live2DValue(val value: Float, val opacity: Float) {
    fun flatten(default: Float): Float {
        return value * opacity + default * (1f - opacity)
    }
}

internal class Live2DPose(val map: Live2DPoseMap) {
    val values = HashMap<Int, Live2DValue>()

    fun clone(): Live2DPose {
        val pose = Live2DPose(map)
        pose.values.putAll(values)
        return pose
    }

    fun hasParam(id: String): Boolean {
        return map.paramIndex(id) != null
    }

    fun setParam(id: String, value: Float) {
        val index = map.paramIndex(id) ?: return
        values[index] = Live2DValue(value, 1f)
    }

    fun update(other: Live2DPose) {
        values.putAll(other.values)
    }

    fun getFlattened(index: Int): Float {
        val default = map.descriptors[index].default
        return values[index]?.flatten(default) ?: default
    }

    fun flattenedForUpdate(index: Int): Float {
        val default = map.descriptors[index].default
        val value = values.getOrPut(index) { Live2DValue(default, 1f) }
        if (value.opacity != 1f) {
            val flattened = Live2DValue(value.flatten(default), 1f)
            values[index] = flattened
            return flattened.value
        }
        return value.value
    }

    fun set(index: Int, value: Float) {
        values[index] = Live2DValue(value, 1f)
    }
}

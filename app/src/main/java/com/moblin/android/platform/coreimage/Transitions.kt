package com.moblin.android.platform.coreimage

import com.moblin.android.platform.coreimage.internal.CombineNode
import com.moblin.android.platform.coreimage.internal.MixCombine

class CITransitionFilter : CIFilter() {
    var inputImage: CIImage? = null
    var targetImage: CIImage? = null
    var time: Float = 0f

    override val name: String
        get() = "CIDissolveTransition"

    override val outputImage: CIImage?
        get() {
            val input = inputImage ?: return null
            val target = targetImage ?: return null
            val amount = time.coerceIn(0f, 1f).toDouble()
            val extent = input.extent.union(target.extent)
            return CIImage(CombineNode(listOf(input.node, target.node), MixCombine(amount), extent))
        }

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            kCIInputImageKey -> inputImage = value as? CIImage
            kCIInputTargetImageKey -> targetImage = value as? CIImage
            kCIInputTimeKey -> time = anyToFloat(value, time)
            else -> super.setValue(value, forKey)
        }
    }

    override fun value(forKey: String): Any? {
        return when (forKey) {
            kCIInputImageKey -> inputImage
            kCIInputTargetImageKey -> targetImage
            kCIInputTimeKey -> time
            else -> super.value(forKey)
        }
    }

    override fun setDefaults() {
        time = 0f
    }
}

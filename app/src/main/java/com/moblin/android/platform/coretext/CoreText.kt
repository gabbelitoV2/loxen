package com.moblin.android.platform.coretext

import com.moblin.android.platform.uikit.InstalledFonts

const val kCTFontNameAttribute = "NSFontNameAttribute"
const val kCTFontDisplayNameAttribute = "NSFontVisibleNameAttribute"
const val kCTFontFamilyNameAttribute = "NSFontFamilyAttribute"
const val kCTFontStyleNameAttribute = "NSFontFaceAttribute"

class CTFontDescriptor internal constructor(private val attributes: Map<String, Any>) {
    internal fun attribute(name: String): Any? = attributes[name]
}

class CTFontCollection internal constructor(internal val descriptors: List<CTFontDescriptor>)

fun CTFontCollectionCreateFromAvailableFonts(options: Any?): CTFontCollection {
    val fonts = InstalledFonts.shared
    val descriptors = fonts.familyNames.flatMap { family ->
        fonts.fontNames(family).mapNotNull { name ->
            val face = fonts.face(name) ?: return@mapNotNull null
            CTFontDescriptor(
                mapOf(
                    kCTFontNameAttribute to face.fontName,
                    kCTFontDisplayNameAttribute to face.fullName,
                    kCTFontFamilyNameAttribute to face.familyName,
                    kCTFontStyleNameAttribute to face.styleName,
                ),
            )
        }
    }
    return CTFontCollection(descriptors)
}

fun CTFontCollectionCreateMatchingFontDescriptors(collection: CTFontCollection): List<CTFontDescriptor>? =
    collection.descriptors.ifEmpty { null }

fun CTFontDescriptorCopyAttribute(descriptor: CTFontDescriptor, attribute: String): Any? =
    descriptor.attribute(attribute)

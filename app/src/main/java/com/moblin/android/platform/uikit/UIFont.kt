package com.moblin.android.platform.uikit

import android.graphics.Typeface

class UIFont private constructor(private val face: InstalledFontFace, val pointSize: Double) {
    val fontName: String
        get() = face.fontName

    val familyName: String
        get() = face.familyName

    val typeface: Typeface
        get() = face.typeface ?: Typeface.DEFAULT

    companion object {
        val familyNames: List<String>
            get() = InstalledFonts.shared.familyNames

        fun fontNames(forFamilyName: String): List<String> = InstalledFonts.shared.fontNames(forFamilyName)

        operator fun invoke(name: String, size: Number): UIFont? {
            val face = InstalledFonts.shared.face(name) ?: return null
            if (face.typeface == null) {
                return null
            }
            return UIFont(face, size.toDouble())
        }
    }
}

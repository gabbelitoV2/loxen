package com.moblin.android.platform.swiftui

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.moblin.android.AppDelegate
import com.moblin.android.platform.offscreen.logOverlayOnce
import com.moblin.android.view.utils.FontDesign

object SwiftUIFonts {
    private const val interPath = "fonts/InterVariable.ttf"
    private const val nunitoPath = "fonts/Nunito.ttf"

    private val weights = listOf(
        FontWeight.W100,
        FontWeight.W200,
        FontWeight.W300,
        FontWeight.W400,
        FontWeight.W500,
        FontWeight.W600,
        FontWeight.W700,
        FontWeight.W800,
        FontWeight.W900,
    )

    private val inter: FontFamily by lazy { assetFamily(interPath, FontFamily.SansSerif) }

    private val nunito: FontFamily by lazy { assetFamily(nunitoPath, FontFamily.SansSerif) }

    val rounded: FontFamily
        get() = nunito

    fun system(
        size: Float,
        weight: FontWeight = FontWeight.Normal,
        design: FontDesign = FontDesign.Default,
    ): TextStyle = TextStyle(fontSize = size.sp, fontWeight = weight, fontFamily = family(design))

    fun system(
        size: Number,
        weight: FontWeight = FontWeight.Normal,
        design: FontDesign = FontDesign.Default,
    ): TextStyle = system(size.toFloat(), weight, design)

    fun custom(name: String, size: Number): TextStyle = custom(name, size.toFloat())

    fun custom(name: String, size: Float): TextStyle {
        val font = resolveCustom(name)
        return TextStyle(
            fontSize = size.sp,
            fontWeight = font.weight,
            fontStyle = font.style,
            fontFamily = font.family,
        )
    }

    fun family(design: FontDesign): FontFamily = when (design) {
        FontDesign.Default -> inter
        FontDesign.Serif -> FontFamily.Serif
        FontDesign.Rounded -> nunito
        FontDesign.Monospaced -> FontFamily.Monospace
    }

    private fun assetFamily(path: String, fallback: FontFamily): FontFamily = try {
        val assets = AppDelegate.context.assets
        assets.open(path).close()
        FontFamily(weights.map { weight -> Font(path = path, assetManager = assets, weight = weight) })
    } catch (error: Throwable) {
        logOverlayOnce("SwiftUIFonts: $path unavailable: $error")
        fallback
    }

    private class CustomFont(val family: FontFamily, val weight: FontWeight?, val style: FontStyle?)

    private enum class Generic { sans, serif, monospace, rounded, cursive }

    private val families: List<Pair<String, Generic>> = listOf(
        "sfprorounded" to Generic.rounded,
        "sfrounded" to Generic.rounded,
        "arialroundedmt" to Generic.rounded,
        "nunito" to Generic.rounded,
        "sfmono" to Generic.monospace,
        "menlo" to Generic.monospace,
        "monaco" to Generic.monospace,
        "courier" to Generic.monospace,
        "americantypewriter" to Generic.monospace,
        "andalemono" to Generic.monospace,
        "monospace" to Generic.monospace,
        "timesnewroman" to Generic.serif,
        "times" to Generic.serif,
        "georgia" to Generic.serif,
        "baskerville" to Generic.serif,
        "didot" to Generic.serif,
        "palatino" to Generic.serif,
        "hoefler" to Generic.serif,
        "charter" to Generic.serif,
        "bodoni" to Generic.serif,
        "cochin" to Generic.serif,
        "newyork" to Generic.serif,
        "iowan" to Generic.serif,
        "superclarendon" to Generic.serif,
        "rockwell" to Generic.serif,
        "bigcaslon" to Generic.serif,
        "serif" to Generic.serif,
        "zapfino" to Generic.cursive,
        "snellroundhand" to Generic.cursive,
        "bradleyhand" to Generic.cursive,
        "savoyelet" to Generic.cursive,
        "noteworthy" to Generic.cursive,
        "markerfelt" to Generic.cursive,
        "chalkboard" to Generic.cursive,
        "chalkduster" to Generic.cursive,
        "partylet" to Generic.cursive,
        "cursive" to Generic.cursive,
    ).sortedByDescending { it.first.length }

    private val styleWords: List<Pair<String, FontWeight>> = listOf(
        "ultralight" to FontWeight.W200,
        "extralight" to FontWeight.W200,
        "semibold" to FontWeight.W600,
        "demibold" to FontWeight.W600,
        "extrabold" to FontWeight.W800,
        "ultrabold" to FontWeight.W800,
        "heavy" to FontWeight.W800,
        "black" to FontWeight.W900,
        "thin" to FontWeight.W100,
        "light" to FontWeight.W300,
        "medium" to FontWeight.W500,
        "bold" to FontWeight.W700,
        "regular" to FontWeight.W400,
        "roman" to FontWeight.W400,
        "book" to FontWeight.W400,
    )

    private fun resolveCustom(name: String): CustomFont {
        val dash = name.lastIndexOf('-')
        val familyPart = if (dash > 0) name.substring(0, dash) else name
        val stylePart = if (dash > 0) name.substring(dash + 1).lowercase() else ""
        val key = familyPart.lowercase().filter { it.isLetterOrDigit() }
        val generic = families.firstOrNull { key.startsWith(it.first) }?.second ?: Generic.sans
        val family = when (generic) {
            Generic.sans -> inter
            Generic.serif -> FontFamily.Serif
            Generic.monospace -> FontFamily.Monospace
            Generic.rounded -> nunito
            Generic.cursive -> FontFamily.Cursive
        }
        val weight = styleWords.firstOrNull { stylePart.contains(it.first) }?.second
        val style = if (stylePart.contains("italic") || stylePart.contains("oblique")) FontStyle.Italic else null
        return CustomFont(family, weight, style)
    }
}

fun TextStyle.monospacedDigit(): TextStyle {
    val existing = fontFeatureSettings
    val settings = when {
        existing.isNullOrEmpty() -> "tnum"
        existing.contains("tnum") -> existing
        else -> "$existing, tnum"
    }
    return copy(fontFeatureSettings = settings)
}

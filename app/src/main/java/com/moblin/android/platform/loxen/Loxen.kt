package com.moblin.android.platform.loxen

object Loxen {
    const val appName = "Loxen"
    const val upstreamName = "Moblin"
    const val repositoryUrl = "https://github.com/gabbelitoV2/loxen"
    const val privacyPolicyUrl = "https://gabbelitov2.github.io/loxen/privacy-policy/en.html"
    const val twitchClientId = "vv58ywvqesrle0zm9trsu3jg20utzr"
    const val attribution = "Loxen is based on Moblin by Erik Moqvist (MIT). Not affiliated with or endorsed by Moblin."
    const val noPurchases = "Loxen is free and has no in-app purchases."
    val hidesStore = true
    val hidesStreamDecks = true

    const val license = """MIT License

Copyright (c) 2023 Erik Moqvist
Copyright (c) 2026 gabbelitoV2

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
"""

    val keptPhrases = listOf(
        "Moblin website",
        "Moblin Discord",
        "Moblin Mobcam",
        "Moblin Watch",
        "Moblin Remote Control",
        "Add automatically to Moblin",
    )

    val androidTexts = mapOf(
        "⚠️ Allow Moblin to access your location in iOS Settings to use location." to
            "⚠️ Allow Moblin to access your location and turn on location in Android settings to use location.",
        "⚠️ Allow Moblin to access your location in iOS Settings to see the current WiFi network." to
            "⚠️ Allow Moblin to access your location and turn on location in Android settings to see the current " +
            "WiFi network.",
        "Copy your device name from iOS settings." to "Copy your device name from Android settings.",
        "Download languages in iOS Settings → Apps → Translate → Languages." to
            "Download on-device translation languages in Android settings.",
        "Download enhanced and premium voices in iOS Settings → Accessibility → Live Speech → Preferred Voices." to
            "Download more voices in Android settings → Text-to-speech output.",
        "⚠️ Hijacks volume buttons. You can only change volume in Control Center when enabled." to
            "⚠️ Hijacks volume buttons. You can only change volume in Android settings when enabled.",
    )

    private val upstreamWord = Regex("""(?<![\p{L}\p{N}_./\\@-])Moblin(?![\p{L}\p{N}_]|[.-]\p{L})""")

    fun rename(text: String): String {
        val android = androidTexts[text] ?: text
        if (!android.contains(upstreamName)) {
            return android
        }
        val kept = keptPhrases.flatMap { phrase -> ranges(android, phrase) }
        return upstreamWord.replace(android) { match ->
            if (kept.any { match.range.first in it }) match.value else appName
        }
    }

    private fun ranges(text: String, phrase: String): List<IntRange> {
        val found = mutableListOf<IntRange>()
        var index = text.indexOf(phrase)
        while (index >= 0) {
            found.add(index until index + phrase.length)
            index = text.indexOf(phrase, index + 1)
        }
        return found
    }

    fun imagePath(name: String): String = when {
        name == "MoblinInMouth" -> "Loxen/AppIconNoBackground"
        name.startsWith("AppIcon") && name.endsWith("NoBackground") -> "Loxen/AppIconNoBackground"
        name.startsWith("AppIcon") -> "Loxen/AppIcon"
        else -> "Assets/$name"
    }
}

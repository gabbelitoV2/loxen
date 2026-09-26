package com.moblin.android.platform.weatherkit

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import com.moblin.android.platform.swiftui.formPalette

private val markdownLink = Regex("""\[([^\]]+)]\(([^)\s]+)\)""")

@Composable
fun WeatherAttributionText(text: String) {
    val linkColor = formPalette().accent
    val annotated = remember(text, linkColor) { weatherAttribution(text = text, linkColor = linkColor) }
    Text(text = annotated)
}

internal fun weatherAttribution(text: String, linkColor: Color): AnnotatedString = buildAnnotatedString {
    var start = 0
    for (match in markdownLink.findAll(text)) {
        append(text.substring(start, match.range.first))
        val url = LinkAnnotation.Url(
            url = match.groupValues[2],
            styles = TextLinkStyles(style = SpanStyle(color = linkColor)),
        )
        withLink(url) {
            append(match.groupValues[1])
        }
        start = match.range.last + 1
    }
    append(text.substring(start))
}

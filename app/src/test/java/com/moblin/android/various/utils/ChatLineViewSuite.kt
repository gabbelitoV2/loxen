package com.moblin.android.various.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.Size
import com.moblin.android.view.utils.ChatLineContent
import com.moblin.android.view.utils.ChatLineItem
import com.moblin.android.view.utils.ChatLineTextStyle
import com.moblin.android.view.utils.ChatLineUiView
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.Test

private fun makeView(): ChatLineUiView {
    val view = ChatLineUiView()
    view.setContent(
        ChatLineContent(
            items = listOf(
                ChatLineItem.Text(
                    "user: this is a fairly long chat message that wraps over several lines",
                    ChatLineTextStyle(color = Color.WHITE),
                ),
            ),
            fontSize = 17,
        ),
    )
    return view
}

private fun render(view: ChatLineUiView): ByteArray? {
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    view.draw(canvas)
    val output = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
    return output.toByteArray()
}

private fun setSize(view: ChatLineUiView, availableWidth: Float) {
    val size = view.size(availableWidth = availableWidth)
    view.layout(0, 0, size.width, size.height)
}

class ChatLineViewSuite {
    @Test
    fun renderingIsTheSameAfterWidthChangedBackWithoutRemeasuring() {
        val reference = makeView()
        setSize(reference, availableWidth = 400f)
        val resized = makeView()
        setSize(resized, availableWidth = 400f)
        val wideSize = Size(resized.width, resized.height)
        setSize(resized, availableWidth = 250f)
        assertNotEquals(wideSize, Size(resized.width, resized.height))
        resized.layout(0, 0, wideSize.width, wideSize.height)
        assertEquals(render(reference), render(resized))
    }
}

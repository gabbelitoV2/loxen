package com.moblin.android.various.subtitles

import org.junit.Test
import kotlin.test.assertEquals

class TextAlignerSuite {
    @Test
    fun basic() {
        val textAligner = TextAligner("Cats have more fun than dogs")
        assertEquals(0, textAligner.position)
        textAligner.update("Cats have more fun than dogs")
        assertEquals(0, textAligner.position)
        textAligner.update("have more fun than dogs when")
        assertEquals(5, textAligner.position)
        textAligner.update("Cats have more fun than dogs")
        assertEquals(0, textAligner.position)
        textAligner.update("Cats are more fun than dogs")
        assertEquals(1, textAligner.position)
        textAligner.update("ore fun than dogs and have")
        assertEquals(11, textAligner.position)
        textAligner.update("ore fun than dogs but dogs are better listeners")
        assertEquals(11, textAligner.position)
        textAligner.update("ore fun but dogs are better listeners")
        assertEquals(21, textAligner.position)
    }
}

package com.michis.player.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class NaturalOrderTest {
    @Test fun `sorts numeric file names naturally`() {
        val names = listOf("10.mp3", "2.mp3", "1.mp3").sortedWith(NaturalOrder)
        assertEquals(listOf("1.mp3", "2.mp3", "10.mp3"), names)
    }
}

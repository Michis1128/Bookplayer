package com.michis.player.feature.player

import com.michis.player.domain.model.Chapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChapterRangeTest {
    @Test
    fun chapterUsesItsExplicitEnd() {
        val state = PlayerUiState(
            durationMs = 90_000,
            chapters = listOf(chapter(0, 10_000, 40_000), chapter(1, 40_000, 90_000)),
        )

        val range = state.chapterAt(25_000)

        assertEquals(10_000L, range?.startMs)
        assertEquals(40_000L, range?.endMs)
    }

    @Test
    fun chapterWithoutEndUsesNextChapterStart() {
        val state = PlayerUiState(
            durationMs = 90_000,
            chapters = listOf(chapter(0, 0, 0), chapter(1, 35_000, 0)),
        )

        assertEquals(35_000L, state.chapterAt(12_000)?.endMs)
        assertEquals(90_000L, state.chapterAt(50_000)?.endMs)
    }

    @Test
    fun audioWithoutChaptersHasNoChapterRange() {
        assertNull(PlayerUiState(durationMs = 90_000).chapterAt(20_000))
    }

    private fun chapter(order: Int, startMs: Long, endMs: Long) = Chapter(
        id = "chapter-$order",
        bookId = "book",
        audioFileId = "file",
        title = "Capítulo ${order + 1}",
        startMs = startMs,
        endMs = endMs,
        order = order,
    )
}

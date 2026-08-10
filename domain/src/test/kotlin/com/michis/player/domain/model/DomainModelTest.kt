package com.michis.player.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DomainModelTest {
    @Test fun `new audiobook retains explicit state`() {
        val book = Audiobook("id", "Title", null, null, 0, BookStatus.NEW, 1, 1)
        assertEquals(BookStatus.NEW, book.status)
        assertEquals(BookAvailability.AVAILABLE, book.availability)
    }
}

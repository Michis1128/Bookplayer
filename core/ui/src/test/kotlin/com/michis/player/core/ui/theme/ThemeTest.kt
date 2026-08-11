package com.michis.player.core.ui.theme

import com.michis.player.domain.repository.ThemePreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ThemeTest {
    @Test
    fun systemThemeUsesDayOrNightPalette() {
        assertEquals(paletteFor(ThemePreference.LIGHT), paletteFor(ThemePreference.SYSTEM, systemDark = false))
        assertEquals(paletteFor(ThemePreference.DARK), paletteFor(ThemePreference.SYSTEM, systemDark = true))
    }

    @Test
    fun namedThemesHaveDifferentBackgrounds() {
        val namedThemes = ThemePreference.entries.filterNot { it == ThemePreference.SYSTEM }
        val backgrounds = namedThemes.map { paletteFor(it).background }

        assertEquals(namedThemes.size, backgrounds.distinct().size)
        assertNotEquals(paletteFor(ThemePreference.LIGHT), paletteFor(ThemePreference.DARK))
    }
}

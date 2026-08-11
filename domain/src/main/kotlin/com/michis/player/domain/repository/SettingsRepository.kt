package com.michis.player.domain.repository

import kotlinx.coroutines.flow.Flow

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
    SEPIA,
    TWILIGHT,
    CONSOLE,
    PAPER,
    SAND,
    LAVENDER,
    FOREST,
    OCEAN,
    GRAPHITE,
    MIDNIGHT,
    SOFT_PINK,
    MINT,
}
enum class LibraryLayout { GRID, LIST }
enum class LibrarySort { TITLE, AUTHOR, DATE_ADDED, PROGRESS, LAST_PLAYED }

data class GlobalSettings(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val libraryLayout: LibraryLayout = LibraryLayout.GRID,
    val librarySort: LibrarySort = LibrarySort.TITLE,
    val skipBackwardSeconds: Int = 10,
    val skipForwardSeconds: Int = 30,
    val autoRewindEnabled: Boolean = true,
)

interface SettingsRepository {
    val settings: Flow<GlobalSettings>
    suspend fun setTheme(theme: ThemePreference)
    suspend fun setLibraryLayout(layout: LibraryLayout)
}

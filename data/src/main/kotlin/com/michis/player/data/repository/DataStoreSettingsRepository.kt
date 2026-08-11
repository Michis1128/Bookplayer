package com.michis.player.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.michis.player.domain.repository.GlobalSettings
import com.michis.player.domain.repository.LibraryLayout
import com.michis.player.domain.repository.LibrarySort
import com.michis.player.domain.repository.SettingsRepository
import com.michis.player.domain.repository.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    override val settings: Flow<GlobalSettings> = dataStore.data.map { preferences ->
        GlobalSettings(
            theme = preferences[THEME]?.let { value -> runCatching { ThemePreference.valueOf(value) }.getOrNull() }
                ?: ThemePreference.SYSTEM,
            libraryLayout = preferences[LAYOUT]?.let { value -> runCatching { LibraryLayout.valueOf(value) }.getOrNull() }
                ?: LibraryLayout.GRID,
            librarySort = preferences[SORT]?.let { value -> runCatching { LibrarySort.valueOf(value) }.getOrNull() }
                ?: LibrarySort.TITLE,
        )
    }

    override suspend fun setTheme(theme: ThemePreference) { dataStore.edit { it[THEME] = theme.name } }
    override suspend fun setLibraryLayout(layout: LibraryLayout) { dataStore.edit { it[LAYOUT] = layout.name } }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val LAYOUT = stringPreferencesKey("library_layout")
        val SORT = stringPreferencesKey("library_sort")
    }
}

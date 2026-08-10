package com.michis.player.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import com.michis.player.data.local.MichisPlayerDatabase
import com.michis.player.data.local.dao.AudiobookDao
import com.michis.player.data.local.dao.AudioFileDao
import com.michis.player.data.local.MIGRATION_1_2
import com.michis.player.data.local.dao.LibraryRootDao
import com.michis.player.data.repository.DataStoreSettingsRepository
import com.michis.player.data.repository.LocalAudiobookRepository
import com.michis.player.data.repository.LocalLibraryRootRepository
import com.michis.player.domain.repository.AudiobookRepository
import com.michis.player.domain.repository.LibraryRootRepository
import com.michis.player.domain.repository.SettingsRepository
import com.michis.player.domain.repository.LibraryScanner
import com.michis.player.data.scanner.SafLibraryScanner
import com.michis.player.domain.usecase.AddLibraryRootUseCase
import com.michis.player.domain.usecase.ScanLibraryUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindAudiobookRepository(repository: LocalAudiobookRepository): AudiobookRepository
    @Binds abstract fun bindLibraryRootRepository(repository: LocalLibraryRootRepository): LibraryRootRepository
    @Binds abstract fun bindSettingsRepository(repository: DataStoreSettingsRepository): SettingsRepository
    @Binds abstract fun bindLibraryScanner(scanner: SafLibraryScanner): LibraryScanner
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton fun provideDatabase(@ApplicationContext context: Context): MichisPlayerDatabase =
        Room.databaseBuilder(context, MichisPlayerDatabase::class.java, "michis-player.db").addMigrations(MIGRATION_1_2).build()

    @Provides fun provideAudiobookDao(database: MichisPlayerDatabase): AudiobookDao = database.audiobookDao()
    @Provides fun provideLibraryRootDao(database: MichisPlayerDatabase): LibraryRootDao = database.libraryRootDao()
    @Provides fun provideAudioFileDao(database: MichisPlayerDatabase): AudioFileDao = database.audioFileDao()

    @Provides @Singleton fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("global-settings.preferences_pb") }

    @Provides fun provideAddLibraryRootUseCase(roots: LibraryRootRepository, scanner: LibraryScanner) =
        AddLibraryRootUseCase(roots, scanner)

    @Provides fun provideScanLibraryUseCase(scanner: LibraryScanner) = ScanLibraryUseCase(scanner)
}

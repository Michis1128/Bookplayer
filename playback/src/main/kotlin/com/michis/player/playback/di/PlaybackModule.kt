package com.michis.player.playback.di

import com.michis.player.domain.repository.PlaybackController
import com.michis.player.playback.player.Media3PlaybackController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlaybackModule {
    @Binds @Singleton abstract fun bindPlaybackController(controller: Media3PlaybackController): PlaybackController
}

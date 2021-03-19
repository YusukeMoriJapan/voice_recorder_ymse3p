package ymse3p.app.audiorecorder.di

import android.content.Context
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class PlaybackModule {

    @Singleton
    @Provides
    fun provideExoPlayer(
        @ApplicationContext context: Context,
    ): SimpleExoPlayer {

        val trackSelector = DefaultTrackSelector(context)
        val loadControl = DefaultLoadControl()

        return SimpleExoPlayer.Builder(context).run {
            setTrackSelector(trackSelector)
            setLoadControl(loadControl)
            build()
        }
    }
}
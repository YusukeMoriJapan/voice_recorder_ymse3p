package ymse3p.app.audiorecorder.di.playbackmodule.playbackVmModule

import android.content.ComponentName
import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import ymse3p.app.audiorecorder.services.AudioService
import javax.inject.Qualifier

@Module
@InstallIn(ViewModelComponent::class)
object PlaybackVmProvidesModule {
    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class PlaybackVmCoroutineScope

    @Provides
    @ViewModelScoped
    fun provideMediaBrowser(
        @ApplicationContext context: Context,
        connectionCallback: MediaBrowserCompat.ConnectionCallback
    ): MediaBrowserCompat =
        MediaBrowserCompat(
            context, ComponentName(context, AudioService::class.java),
            connectionCallback, null
        ).apply { connect() }


    @PlaybackVmCoroutineScope
    @Provides
    @ViewModelScoped
    fun provideViewModelScope() = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

}

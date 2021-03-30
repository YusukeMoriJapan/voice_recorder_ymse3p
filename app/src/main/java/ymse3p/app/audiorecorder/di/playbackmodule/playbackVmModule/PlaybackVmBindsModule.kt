package ymse3p.app.audiorecorder.di.playbackmodule.playbackVmModule

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import ymse3p.app.audiorecorder.viewmodels.playbackViewModel.playbackComponent.PlaybackComponent
import ymse3p.app.audiorecorder.viewmodels.playbackViewModel.playbackComponent.PlaybackComponentImpl

@Module
@InstallIn(ViewModelComponent::class)
abstract class PlaybackVmBindsModule {

    @ViewModelScoped
    @Binds
    abstract fun bindPlaybackVmComponent(
        playbackComponent: PlaybackComponentImpl
    ): PlaybackComponent

}
package ymse3p.app.voicelogger.di.playbackmodule.playbackVmModule

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import ymse3p.app.voicelogger.viewmodels.playbackViewModel.playbackComponent.VmPlaybackComponent
import ymse3p.app.voicelogger.viewmodels.playbackViewModel.playbackComponent.VmPlaybackComponentImpl

@Module
@InstallIn(ViewModelComponent::class)
abstract class PlaybackVmBindsModule {

    @ViewModelScoped
    @Binds
    abstract fun bindPlaybackVmComponent(
        playbackComponent: VmPlaybackComponentImpl
    ): VmPlaybackComponent

}
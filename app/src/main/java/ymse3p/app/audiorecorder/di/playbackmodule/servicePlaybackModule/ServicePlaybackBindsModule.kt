package ymse3p.app.audiorecorder.di.playbackmodule.servicePlaybackModule

import android.media.AudioManager
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.scopes.ServiceScoped
import ymse3p.app.audiorecorder.services.playbackComponent.ServicePlaybackComponent
import ymse3p.app.audiorecorder.services.playbackComponent.ServicePlaybackComponentImpl
import ymse3p.app.audiorecorder.services.playbackComponent.components.AudioMediaControllerCallback
import ymse3p.app.audiorecorder.services.playbackComponent.components.AudioMediaSessionCallback
import ymse3p.app.audiorecorder.services.playbackComponent.components.OnAudioFocusChangeListenerImpl

@Module
@InstallIn(ServiceComponent::class)
abstract class ServicePlaybackBindsModule {

    @ServiceScoped
    @Binds
    abstract fun bindServicePlaybackComponent(
        servicePlaybackComponent: ServicePlaybackComponentImpl
    ): ServicePlaybackComponent

    @ServiceScoped
    @Binds
    abstract fun bindOnAudioFocusChangeListener(
        onAudioFocusChangeListenerImpl: OnAudioFocusChangeListenerImpl
    ): AudioManager.OnAudioFocusChangeListener


    @ServiceScoped
    @Binds
    abstract fun bindMediaControllerCallback(
        audioMediaControllerCallback: AudioMediaControllerCallback
    ): MediaControllerCompat.Callback

    @ServiceScoped
    @Binds
    abstract fun provideMediaSessionCallback(
        audioMediaSessionCallback: AudioMediaSessionCallback
    ): MediaSessionCompat.Callback
}
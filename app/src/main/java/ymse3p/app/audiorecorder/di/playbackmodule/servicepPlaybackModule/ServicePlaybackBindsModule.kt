package ymse3p.app.audiorecorder.di.playbackmodule.servicepPlaybackModule

import android.media.AudioManager
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.scopes.ServiceScoped
import ymse3p.app.audiorecorder.services.implementation.AudioMediaControllerCallback
import ymse3p.app.audiorecorder.services.implementation.AudioMediaSessionCallback
import ymse3p.app.audiorecorder.services.implementation.OnAudioFocusChangeListenerImpl

@Module
@InstallIn(ServiceComponent::class)
abstract class ServicePlaybackBindsModule {

    @ServiceScoped
    @Binds
    abstract fun provideOnAudioFocusChangeListener(
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
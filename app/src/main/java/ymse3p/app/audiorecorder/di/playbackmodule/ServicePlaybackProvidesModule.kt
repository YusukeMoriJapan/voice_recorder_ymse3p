package ymse3p.app.audiorecorder.di.playbackmodule

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import kotlinx.coroutines.Job
import ymse3p.app.audiorecorder.services.AudioNotificationBuilder
import ymse3p.app.audiorecorder.services.AudioService
import javax.inject.Qualifier
import javax.inject.Singleton


@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ServiceContext

@Module
@InstallIn(ServiceComponent::class)
object ServicePlaybackProvidesModule {

    @ServiceScoped
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

    @ServiceScoped
    @Provides
    fun provideMediaSession(
        @ApplicationContext context: Context,
    ): MediaSessionCompat {
        return MediaSessionCompat(context, AudioService::class.simpleName.orEmpty()).apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS)
        }
    }

    @ServiceScoped
    @Provides
    fun provideNotificationManager(@ApplicationContext context: Context): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @ServiceScoped
    @Provides
    fun provideAudioNotificationBuilder(
        @ApplicationContext context: Context,
        mediaSession: MediaSessionCompat,
    ): AudioNotificationBuilder = AudioNotificationBuilder(context, mediaSession)


    @ServiceScoped
    @Provides
    fun provideAudioManager(@ApplicationContext context: Context) =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @ServiceScoped
    @Provides
    fun provideAudioAttributes(): AudioAttributesCompat =
        AudioAttributesCompat.Builder().run {
            setUsage(AudioAttributesCompat.USAGE_MEDIA)
            setContentType(AudioAttributesCompat.CONTENT_TYPE_SPEECH)
            build()
        }

    @ServiceScoped
    @Provides
    fun provideAudioFocusRequest(
        audioAttributes: AudioAttributesCompat,
        onAudioFocusChangeListener: AudioManager.OnAudioFocusChangeListener
    ): AudioFocusRequestCompat {
        return AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener(onAudioFocusChangeListener).build()
    }

    @ServiceScoped
    @Provides
    fun provideAudioMediaMetaData()
            : MutableMap<Int, MediaMetadataCompat> = mutableMapOf()


    @ServiceScoped
    @Provides
    fun provideQueueItems()
            : MutableList<MediaSessionCompat.QueueItem> = mutableListOf()

    class PlaybackQueueIndex() {
        var i = 0
    }

    @ServiceScoped
    @Provides
    fun provideIndex(): PlaybackQueueIndex = PlaybackQueueIndex()


    @ServiceContext
    @ServiceScoped
    @Provides
    fun provideServiceContext(): Job = Job()

}
package ymse3p.app.audiorecorder.services.playbackComponent

import android.content.Context
import android.media.AudioManager
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ymse3p.app.audiorecorder.MyApplication
import ymse3p.app.audiorecorder.di.playbackmodule.servicePlaybackModule.ServiceCoroutineScope
import ymse3p.app.audiorecorder.services.playbackComponent.components.ServicePlaybackComponentState
import javax.inject.Inject

@ServiceScoped
class ServicePlaybackComponentImpl
@Inject constructor(
    @ApplicationContext private val myApplication: Context,
    private val mediaSession: MediaSessionCompat,
    private val exoPlayer: SimpleExoPlayer,
    private val componentState: ServicePlaybackComponentState,
    private val controllerCallback: MediaControllerCompat.Callback,
    private val sessionCallback: MediaSessionCompat.Callback,
    @ServiceCoroutineScope private val serviceScope: CoroutineScope
) : ServicePlaybackComponent {


    init {
        mediaSession.setCallback(sessionCallback)
        mediaSession.controller.registerCallback(controllerCallback)

        exoPlayer.addListener(object : Player.EventListener {
            /** ExoPlayerの再生状態が変化したときに呼ばれる */
            override fun onPlaybackStateChanged(state: Int) {
                updateSessionPlaybackState()
                componentState.setExoPlaybackState(state)
                if (state == Player.STATE_ENDED) {
                    sessionCallback.onSkipToNext()
                }

            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateSessionPlaybackState()
                componentState.setExoPlayingState(isPlaying)
            }

        })

        /** 500msごとに再生情報を更新 */
        serviceScope.launch(Dispatchers.Main) {
            while (true) {
                if (exoPlayer.playbackState == Player.STATE_READY && exoPlayer.playWhenReady) {
                    updateSessionPlaybackState()
                }
                delay(500)
            }
        }

        /** 最新の再生リストをMediaSessionに反映 */
        serviceScope.launch(Dispatchers.Default) {
            componentState.queueItems.collect { queueList ->
                queueList?.let { mediaSession.setQueue(it) }
            }
        }

        /** オーディオフォーカスの変更にに紐づいて、再生状態を切り替える */
        serviceScope.launch {
            componentState.audioFocusChange.collect { focusChange ->
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS)
                    mediaSession.controller.transportControls.pause()
                else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
                    mediaSession.controller.transportControls.pause()
                else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
                //音量を下げる
                else if (focusChange == AudioManager.AUDIOFOCUS_GAIN)
                    mediaSession.controller.transportControls.play()
            }
        }

        /** 倍速再生の値を受け取りEXOに反映 */
        serviceScope.launch(Dispatchers.Main) {
            (myApplication as MyApplication).playbackSpeedFlow.collect { playbackSpeed ->
                exoPlayer.setPlaybackParameters(PlaybackParameters(playbackSpeed))
            }
        }


    }

    /** 「ExoPlayerが保持している現在の再生状態」を、MediaSessionの再生状態に反映する。
    　　再生位置情報も含むため定期的に更新する　*/
    private fun updateSessionPlaybackState() {

        /** ExoPlayerの再生状態を、MediaSession向けの情報に変換 */
        var state = PlaybackStateCompat.STATE_NONE
        when (exoPlayer.playbackState) {
            Player.STATE_IDLE -> {
                state = PlaybackStateCompat.STATE_NONE
            }
            Player.STATE_BUFFERING -> {
                state = PlaybackStateCompat.STATE_BUFFERING
            }
            Player.STATE_READY -> {
                state = if (exoPlayer.playWhenReady)
                    PlaybackStateCompat.STATE_PLAYING
                else
                    PlaybackStateCompat.STATE_PAUSED
            }
            Player.STATE_ENDED -> {
                state = PlaybackStateCompat.STATE_STOPPED
            }
        }

        /** 変換したデータを元にMediaSessionの再生状態を更新 */
        val playbackState =
            PlaybackStateCompat.Builder()
                /** MediaButtonIntentで可能な操作を設定 */
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                            PlaybackStateCompat.ACTION_STOP or
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                )
                .setState(
                    state,
                    exoPlayer.currentPosition,
                    exoPlayer.playbackParameters.speed
                )
                .build()
        mediaSession.setPlaybackStateAndEmit(playbackState)
    }

    override fun getCurrentMetadata(): MediaMetadataCompat? =
        mediaSession.controller.metadata

    override fun getCurrentPlaybackState(): PlaybackStateCompat? =
        mediaSession.controller.playbackState

    override fun playingStateFlow(): StateFlow<Boolean> = componentState.exoPlayingState

    override fun getSessionToken(): MediaSessionCompat.Token =
        mediaSession.sessionToken

    override fun playbackStateFlow(): StateFlow<PlaybackStateCompat?> =
        componentState.sessionPlaybackState


    override fun metadataFlow(): StateFlow<MediaMetadataCompat?> =
        componentState.sessionMetadata

    override fun mediaItemListFlow(): StateFlow<MutableList<MediaBrowserCompat.MediaItem>?> =
        componentState.mediaItemList


    override fun releaseComponent() {
        mediaSession.apply {
            isActive = false
            release()
        }
        exoPlayer.apply {
            stop()
            release()
        }
    }

    private fun MediaSessionCompat.setPlaybackStateAndEmit(state: PlaybackStateCompat) {
        setPlaybackState(state)
        componentState.setSessionPlaybackState(state)
    }

}
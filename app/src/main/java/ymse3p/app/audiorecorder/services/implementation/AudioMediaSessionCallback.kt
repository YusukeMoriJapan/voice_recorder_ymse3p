package ymse3p.app.audiorecorder.services

import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import ymse3p.app.audiorecorder.data.Repository
import ymse3p.app.audiorecorder.data.database.entities.AudioEntity
import ymse3p.app.audiorecorder.di.playbackmodule.ServiceContext
import ymse3p.app.audiorecorder.di.playbackmodule.ServicePlaybackProvidesModule
import javax.inject.Inject

class AudioMediaSessionCallback @Inject constructor(
    private val repository: Repository,
    private val simpleExoPlayer: SimpleExoPlayer,
    private val mediaSession: MediaSessionCompat,
    private val audioManager: AudioManager,
    private val audioFocusRequest: AudioFocusRequestCompat,
    private val audioMediaMetaData: MutableMap<String, MediaMetadataCompat>,
    private val queueItems: MutableList<MediaSessionCompat.QueueItem>,
    private val playbackQueueIndex: ServicePlaybackProvidesModule.PlaybackQueueIndex,
    @ServiceContext private val serviceContext: Job
) : MediaSessionCompat.Callback() {

    /** TransportController#playFromMediaId　に対して呼び出される */
    override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
        if (mediaId == null) {
            return
        } else {
            val readAudioEntity: Flow<AudioEntity> =
                repository.localDataSource.readAudioFromId(mediaId.toInt())
            CoroutineScope(serviceContext).launch {
                readAudioEntity.first { audioEntity ->
                    if (audioEntity != null) {
                        withContext(Dispatchers.Main) {
                            simpleExoPlayer.setMediaItem(MediaItem.fromUri(audioEntity.audioUri))
                            simpleExoPlayer.prepare()
                            mediaSession.isActive = true
                            onPlay()
                        }
                        /** 再生中の曲情報(MediaSessionが配信している曲)を設定 */
                        mediaSession.setMetadata(audioMediaMetaData[mediaId])
                    }
                    return@first true
                }
            }
        }
    }

    /** MediaController#play　に対して呼び出される */
    override fun onPlay() {
        val audioFocusRequestResult =
            AudioManagerCompat
                .requestAudioFocus(audioManager, audioFocusRequest)

        /** Audio focusを要求 */
        if (audioFocusRequestResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            /** focus要求が許可されたら再生を開始 */
            mediaSession.isActive = true
            simpleExoPlayer.playWhenReady = true
        }
    }

    override fun onPause() {
        simpleExoPlayer.playWhenReady = false
        /** オーディオフォーカスを開放する　*/
        AudioManagerCompat.abandonAudioFocusRequest(audioManager, audioFocusRequest)
    }

    override fun onStop() {
        onPause()
        mediaSession.isActive = false
        /** オーディオフォーカスを開放する　*/
        AudioManagerCompat.abandonAudioFocusRequest(audioManager, audioFocusRequest)
    }

    override fun onSeekTo(pos: Long) {
        simpleExoPlayer.seekTo(pos)
    }

    override fun onSkipToNext() {
        playbackQueueIndex.i++
        /** ライブラリの最後まで再生されてる場合は0に戻す */
        if (playbackQueueIndex.i >= queueItems.size) playbackQueueIndex.i = 0

        onPlayFromMediaId(queueItems[playbackQueueIndex.i].description.mediaId, null)
    }

    override fun onSkipToPrevious() {
        playbackQueueIndex.i--
        /** インデックスがマイナスの場合は、最後の曲を再生する*/
        if (playbackQueueIndex.i < 0) playbackQueueIndex.i = queueItems.size - 1

        onPlayFromMediaId(queueItems[playbackQueueIndex.i].description.mediaId, null)
    }

    /** WearやAutoでキュー内のアイテムを選択された際にも呼び出される */
    override fun onSkipToQueueItem(id: Long) {
        onPlayFromMediaId(queueItems[id.toInt()].description.mediaId, null)
    }

    override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
        return super.onMediaButtonEvent(mediaButtonEvent)
    }

}
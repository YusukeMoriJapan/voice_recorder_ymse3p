package ymse3p.app.audiorecorder.services.playbackComponent.components

import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import dagger.hilt.android.scopes.ServiceScoped
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import ymse3p.app.audiorecorder.data.Repository
import ymse3p.app.audiorecorder.data.database.entities.AudioEntity
import ymse3p.app.audiorecorder.di.playbackmodule.servicePlaybackModule.ServiceCoroutineScope
import javax.inject.Inject

@ServiceScoped
class AudioMediaSessionCallback
@Inject constructor(
    private val repository: Repository,
    private val simpleExoPlayer: SimpleExoPlayer,
    private val mediaSession: MediaSessionCompat,
    private val audioManager: AudioManager,
    private val audioFocusRequest: AudioFocusRequestCompat,
    private val componentState: ServicePlaybackComponentState,
    @ServiceCoroutineScope private val serviceScope: CoroutineScope
) : MediaSessionCompat.Callback() {

    /** TransportController#playFromMediaId　に対して呼び出される */
    override fun onPlayFromMediaId(id: String?, extras: Bundle?) {
        if (id == null) {
            return
        } else {
            val readAudioEntity: Flow<AudioEntity> =
                repository.localDataSource.readAudioFromId(id.toInt())
            serviceScope.launch {
                readAudioEntity.first { audioEntity ->
                    if (audioEntity != null) {
                        withContext(Dispatchers.Main) {
                            simpleExoPlayer.setMediaItem(MediaItem.fromUri(audioEntity.audioUri))
                            simpleExoPlayer.prepare()
                            mediaSession.isActive = true
                            onPlay()
                        }
                        /** 再生中の曲情報(MediaSessionが配信している曲)を設定 */
                        componentState.audioMediaMetaData.value?.get(id.toInt())
                            ?.let {
                                mediaSession.setMetadata(it)
                                // QueueIndexを更新する処理の追加が必要
                            }

                    }
                    return@first true
                }
            }
        }
    }

    /** WearやAutoでキュー内のアイテムを選択された際にも呼び出される */
    override fun onSkipToQueueItem(queue: Long) {
        if (queue == null) {
            return
        } else {
            val mediaId: String =
                componentState.queueItems.value?.getOrNull(queue.toInt())?.description?.mediaId
                    ?: return
            val readAudioEntity: Flow<AudioEntity> =
                repository.localDataSource.readAudioFromId(mediaId.toInt())
            serviceScope.launch {
                readAudioEntity.first { audioEntity ->
                    if (audioEntity != null) {
                        withContext(Dispatchers.Main) {
                            simpleExoPlayer.setMediaItem(MediaItem.fromUri(audioEntity.audioUri))
                            simpleExoPlayer.prepare()
                            mediaSession.isActive = true
                            onPlay()
                        }
                        componentState.playbackQueueIndex = queue.toInt()
                        /** 再生中の曲情報(MediaSessionが配信している曲)を設定 */
                        componentState.audioMediaMetaData.value?.get(mediaId.toInt())?.let {
                            mediaSession.setMetadata(it)
                        }
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
        var index = ++componentState.playbackQueueIndex
        val queueItemSize = componentState.queueItems.value?.size
        /** ライブラリの最後まで再生されてる場合は0に戻す */
        queueItemSize?.let {
            if (index >= it) index = 0
            onSkipToQueueItem(index.toLong())
        }

    }

    override fun onSkipToPrevious() {
        var index = --componentState.playbackQueueIndex
        val queueItemSize = componentState.queueItems.value?.size
        /** インデックスがマイナスの場合は、最後の曲を再生する*/
        queueItemSize?.let {
            if (index < 0) index = it - 1
            onSkipToQueueItem(index.toLong())
        }

    }


    override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
        return super.onMediaButtonEvent(mediaButtonEvent)
    }

}
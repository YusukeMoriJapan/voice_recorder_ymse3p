package ymse3p.app.audiorecorder.services

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.Player
import dagger.hilt.android.scopes.ServiceScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ymse3p.app.audiorecorder.data.Repository
import ymse3p.app.audiorecorder.data.database.entities.AudioEntity
import ymse3p.app.audiorecorder.di.playbackmodule.servicePlaybackModule.ServiceCoroutineScope
import ymse3p.app.audiorecorder.util.Constants
import javax.inject.Inject

@ServiceScoped
class ServicePlaybackComponentState @Inject constructor(
    repository: Repository,
    @ServiceCoroutineScope private val serviceScope: CoroutineScope
) {
    private val _exoPlaybackState = MutableStateFlow(Player.STATE_IDLE)
    val exoPlaybackState: StateFlow<Int> = _exoPlaybackState

    private val _exoPlayingState = MutableStateFlow(false)
    val exoPlayingState: StateFlow<Boolean> = _exoPlayingState

    private val _sessionPlaybackState = MutableStateFlow<PlaybackStateCompat?>(null)
    val sessionPlaybackState: StateFlow<PlaybackStateCompat?> = _sessionPlaybackState

    private val _sessionMetadata = MutableStateFlow<MediaMetadataCompat?>(null)
    val sessionMetadata: StateFlow<MediaMetadataCompat?> = _sessionMetadata

    private val _audioMediaMetaData: MutableStateFlow<Map<Int, MediaMetadataCompat>?> =
        MutableStateFlow(null)
    val audioMediaMetaData: StateFlow<Map<Int, MediaMetadataCompat>?> = _audioMediaMetaData

    private val _queueItems: MutableStateFlow<List<MediaSessionCompat.QueueItem>?> =
        MutableStateFlow(null)
    val queueItems: StateFlow<List<MediaSessionCompat.QueueItem>?> = _queueItems

    private val _mediaItemList: MutableStateFlow<MutableList<MediaBrowserCompat.MediaItem>?> =
        MutableStateFlow(null)
    val mediaItemList: StateFlow<MutableList<MediaBrowserCompat.MediaItem>?> = _mediaItemList

    private val _isLoadingDatabase = MutableStateFlow<Boolean?>(null)
    val isLoadingDatabase: StateFlow<Boolean?> = _isLoadingDatabase

    private val _audioFocusChange = MutableSharedFlow<Int>()
    val audioFocusChange = _audioFocusChange

    val readAudio = repository.localDataSource.readAudio()

    var playbackQueueIndex: Int = 0

    init {
        /** データベースの情報(AudioEntity)をMediaSession向けの情報(MediaMetadata,QueueItem,MediaItem)に変換　*/
        serviceScope.launch(Dispatchers.IO) {
            readAudio.collect { audioEntityList ->
                _isLoadingDatabase.value = true
                val jobConvertToMetadata =
                    launch(Dispatchers.IO) { convertToMetadataAndEmit(audioEntityList) }

                val jobConvertToQueue = launch(Dispatchers.IO) {
                    jobConvertToMetadata.join()
                    convertToQueueAndEmit()
                }


                val jobConvertToMediaItem = launch(Dispatchers.IO) {
                    jobConvertToMetadata.join()
                    convertToMediaItemAndEmit()
                }

                jobConvertToMediaItem.join()
                jobConvertToMetadata.join()
                jobConvertToQueue.join()
                _isLoadingDatabase.value = false
            }
        }
    }

    private suspend fun convertToMetadataAndEmit(audioEntityList: List<AudioEntity>) {
        val metadataMap: Map<Int, MediaMetadataCompat> = buildMap {
            audioEntityList.forEachIndexed { index, audioEntity ->
                val mediaMetaData =
                    MediaMetadataCompat.Builder()
                        .putLong(
                            Constants.MEDIA_METADATA_QUEUE,
                            index.toLong()
                        )
                        .putString(
                            MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                            audioEntity.id.toString()
                        )
                        .putString(
                            MediaMetadataCompat.METADATA_KEY_TITLE,
                            audioEntity.audioTitle
                        )
                        .putLong(
                            MediaMetadataCompat.METADATA_KEY_DURATION,
                            audioEntity.audioDuration.toLong()
                        )
                        .build()
                this[audioEntity.id] = mediaMetaData
            }
        }
        _audioMediaMetaData.emit(metadataMap)
    }

    private suspend fun convertToQueueAndEmit() {
        val queueList: List<MediaSessionCompat.QueueItem> = buildList {
            audioMediaMetaData.value?.forEach { mediaMetaData ->
                this.add(
                    MediaSessionCompat.QueueItem(
                        mediaMetaData.value.description,
                        mediaMetaData.value.getLong(Constants.MEDIA_METADATA_QUEUE)
                    )
                )
            }
        }
        _queueItems.emit(queueList)
    }

    private suspend fun convertToMediaItemAndEmit() {
        audioMediaMetaData.value?.let {
            val metadata: List<MediaMetadataCompat> = it.values.toList()
            val mediaItemList = MutableList(metadata.size) { i ->
                MediaBrowserCompat.MediaItem(
                    metadata[i].description,
                    MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                )
            }
            _mediaItemList.emit(mediaItemList)
        }
    }

    fun setExoPlaybackState(state: Int) {
        serviceScope.launch { _exoPlaybackState.emit(state) }
    }

    fun setExoPlayingState(isPlaying: Boolean) {
        serviceScope.launch { _exoPlayingState.emit(isPlaying) }
    }

    fun setSessionPlaybackState(playbackState: PlaybackStateCompat) {
        serviceScope.launch { _sessionPlaybackState.emit(playbackState) }
    }

    fun setSessionMetadata(metadata: MediaMetadataCompat) {
        serviceScope.launch { _sessionMetadata.emit(metadata) }
    }

    fun setAudioFocusChange(focusChange: Int) {
        serviceScope.launch { _audioFocusChange.emit(focusChange) }
    }
}

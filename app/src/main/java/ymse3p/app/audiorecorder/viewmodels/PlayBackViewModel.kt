package ymse3p.app.audiorecorder.viewmodels

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ymse3p.app.audiorecorder.services.AudioService
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltViewModel
class PlayBackViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = viewModelScope.coroutineContext

    private val context get() = getApplication<Application>().applicationContext
    val requestPlayQueue = MutableSharedFlow<Int>()


    /** 再生状態の保持 */
    private val isConnectedController = MutableStateFlow(false)
    private val isInitializedController: Deferred<Boolean> =
        async {
            isConnectedController.first {
                if (it) {
                    mediaController =
                        MediaControllerCompat(context, mediaBrowser.sessionToken)
                            .apply { registerCallback(controllerCallback) }
                    true
                } else {
                    false
                }
            }
            return@async true
        }

    private val _metadata = MutableSharedFlow<MediaMetadataCompat?>(1)
    val metadata: SharedFlow<MediaMetadataCompat?> = _metadata

    private val _state = MutableSharedFlow<PlaybackStateCompat?>(1)
    val state: SharedFlow<PlaybackStateCompat?> = _state


    /** Callbacks */
    private val connectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            isConnectedController.value = true
            mediaBrowser.subscribe(mediaBrowser.root, subscriptionCallback)
        }
    }

    private val subscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(
            parentId: String,
            children: MutableList<MediaBrowserCompat.MediaItem>
        ) {
            launch {
                if (getController().playbackState == null)
                    children.firstOrNull()?.mediaId?.let { playFromMediaId(it) }
            }
        }
    }

    private val controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            launch { _metadata.emit(metadata) }
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            launch { _state.emit(state) }
        }
    }


    /** 再生に必要なコンポーネント */
    private val mediaBrowser: MediaBrowserCompat = MediaBrowserCompat(
        context, ComponentName(context, AudioService::class.java),
        connectionCallback, null
    ).apply { connect() }

    private lateinit var mediaController: MediaControllerCompat


    init {
        /** Foreground Service の実行　*/
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
            context.startForegroundService(Intent(context, AudioService::class.java))
        else
            context.startService(Intent(context, AudioService::class.java))

        /** ユーザーからの音源再生指示を受け取る */
        val collectRequestQueue = launch(Dispatchers.Default) {
            requestPlayQueue.collect {
                skipToQueueItem(it.toLong())
            }
        }

    }


    /** ViewModelの状態遷移に対応した処理 */
    override fun onCleared() {
        super.onCleared()
        mediaBrowser.disconnect()
        try {
            if (mediaController.playbackState.state == PlaybackStateCompat.STATE_PLAYING) return
            else context.stopService(Intent(context, AudioService::class.java))
        } catch (e: UninitializedPropertyAccessException) {
            context.stopService(Intent(context, AudioService::class.java))
        }

    }


    /** オーディオ操作(MediaControllerに対する操作) */
    private suspend fun getController(): MediaControllerCompat {
        isInitializedController.await()
        return mediaController
    }

    fun playFromMediaId(id: String) {
        launch { getController().transportControls.playFromMediaId(id, null) }
    }

    fun skipToQueueItem(queue: Long) {
        launch { getController().transportControls.skipToQueueItem(queue) }
    }

    fun play() {
        launch { getController().transportControls.play() }
    }

    fun pause() {
        launch { getController().transportControls.pause() }
    }

    fun stop() {
        launch { getController().transportControls.stop() }
    }

    fun skipToPrev() {
        launch { getController().transportControls.skipToPrevious() }
    }

    fun skipToNext() {
        launch { getController().transportControls.skipToNext() }
    }

    fun seekTo(position: Long) {
        launch { getController().transportControls.seekTo(position) }
    }

    suspend fun getMetadata(): MediaMetadataCompat? = getController().metadata
    suspend fun getPlaybackState(): PlaybackStateCompat? = getController().playbackState




}
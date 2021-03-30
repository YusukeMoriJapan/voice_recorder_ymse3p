package ymse3p.app.audiorecorder.viewmodels.playbackViewModel

import android.app.Application
import android.content.Intent
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import ymse3p.app.audiorecorder.di.playbackmodule.playbackVmModule.PlaybackVmProvidesModule
import ymse3p.app.audiorecorder.services.AudioService
import ymse3p.app.audiorecorder.viewmodels.playbackViewModel.playbackComponent.PlaybackComponent
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltViewModel
class PlayBackViewModel @Inject constructor(
    application: Application,
    private val playbackComponent: PlaybackComponent,
    @PlaybackVmProvidesModule.PlaybackVmCoroutineScope
    private val playbackVmScope: CoroutineScope
) : AndroidViewModel(application), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = playbackVmScope.coroutineContext

    private val context get() = getApplication<Application>().applicationContext

    val playbackState = playbackComponent.playbackStateFlow()
    val metadata = playbackComponent.metadataFlow()


    init {
        /** Foreground Service の実行　*/
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
            context.startForegroundService(Intent(context, AudioService::class.java))
        else
            context.startService(Intent(context, AudioService::class.java))
    }

    fun playFromMediaId(id: String) {
        playbackComponent.playFromMediaId(id)
    }

    fun skipToQueueItem(queue: Long) {
        playbackComponent.skipToQueueItem(queue)
    }

    fun play() {
        playbackComponent.play()
    }

    fun pause() {
        playbackComponent.pause()
    }

    fun stop() {
        playbackComponent.stop()
    }

    fun skipToPrev() {
        playbackComponent.skipToPrev()
    }

    fun skipToNext() {
        playbackComponent.skipToNext()
    }

    fun seekTo(position: Long) {
        playbackComponent.seekTo(position)
    }

    suspend fun getCurrentPlaybackState() = playbackComponent.getCurrentPlaybackState()
    suspend fun getCurrentMetadata() = playbackComponent.getCurrentMetadata()

    /** ViewModelの状態遷移に対応した処理 */
    override fun onCleared() {
        super.onCleared()
        if (playbackState.replayCache.firstOrNull()?.state == PlaybackStateCompat.STATE_PLAYING) {
            playbackComponent.releaseResources()
        } else {
            playbackComponent.releaseResources()
            context.stopService(Intent(context, AudioService::class.java))
        }
        cancel()
    }
}
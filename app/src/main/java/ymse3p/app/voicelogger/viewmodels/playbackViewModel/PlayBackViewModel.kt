package ymse3p.app.voicelogger.viewmodels.playbackViewModel

import android.app.Application
import android.content.Intent
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import ymse3p.app.voicelogger.di.playbackmodule.playbackVmModule.PlaybackVmProvidesModule
import ymse3p.app.voicelogger.services.AudioService
import ymse3p.app.voicelogger.viewmodels.playbackViewModel.playbackComponent.VmPlaybackComponent
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltViewModel
class PlayBackViewModel @Inject constructor(
    application: Application,
    private val vmPlaybackComponent: VmPlaybackComponent,
    @PlaybackVmProvidesModule.PlaybackVmCoroutineScope
    private val playbackVmScope: CoroutineScope
) : AndroidViewModel(application), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = playbackVmScope.coroutineContext

    private val context get() = getApplication<Application>().applicationContext

    val playbackState = vmPlaybackComponent.playbackStateFlow()
    val metadata = vmPlaybackComponent.metadataFlow()


    init {
        /** Foreground Service の実行　*/
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
            context.startForegroundService(Intent(context, AudioService::class.java))
        else
            context.startService(Intent(context, AudioService::class.java))
    }

    fun playFromMediaId(id: String) {
        vmPlaybackComponent.playFromMediaId(id)
    }

    fun skipToQueueItem(queue: Long) {
        vmPlaybackComponent.skipToQueueItem(queue)
    }

    fun play() {
        vmPlaybackComponent.play()
    }

    fun pause() {
        vmPlaybackComponent.pause()
    }

    fun stop() {
        vmPlaybackComponent.stop()
    }

    fun skipToPrev() {
        vmPlaybackComponent.skipToPrev()
    }

    fun skipToNext() {
        vmPlaybackComponent.skipToNext()
    }

    fun seekTo(position: Long) {
        vmPlaybackComponent.seekTo(position)
    }

    suspend fun getCurrentPlaybackState() = vmPlaybackComponent.getCurrentPlaybackState()
    suspend fun getCurrentMetadata() = vmPlaybackComponent.getCurrentMetadata()

    /** ViewModelの状態遷移に対応した処理 */
    override fun onCleared() {
        super.onCleared()
        if (playbackState.replayCache.firstOrNull()?.state == PlaybackStateCompat.STATE_PLAYING) {
            vmPlaybackComponent.releaseResources()
        } else {
            vmPlaybackComponent.releaseResources()
            context.stopService(Intent(context, AudioService::class.java))
        }
        cancel()
    }
}
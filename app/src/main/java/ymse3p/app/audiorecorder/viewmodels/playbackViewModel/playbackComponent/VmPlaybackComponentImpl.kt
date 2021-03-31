package ymse3p.app.audiorecorder.viewmodels.playbackViewModel.playbackComponent

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import ymse3p.app.audiorecorder.di.playbackmodule.playbackVmModule.PlaybackVmProvidesModule
import ymse3p.app.audiorecorder.viewmodels.playbackViewModel.playbackComponent.components.PlaybackComponentState
import ymse3p.app.audiorecorder.viewmodels.playbackViewModel.playbackComponent.components.PlaybackVmBrowser
import ymse3p.app.audiorecorder.viewmodels.playbackViewModel.playbackComponent.components.PlaybackVmController
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@ViewModelScoped
class VmPlaybackComponentImpl @Inject constructor(
    @PlaybackVmProvidesModule.PlaybackVmCoroutineScope private val viewModelScope: CoroutineScope,
    private val playbackVmController: PlaybackVmController,
    private val playbackComponentState: PlaybackComponentState,
    private val playbackVmBrowser: PlaybackVmBrowser
) : VmPlaybackComponent, CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = viewModelScope.coroutineContext

    override fun playFromMediaId(id: String) {
        launch { playbackVmController.getController().transportControls.playFromMediaId(id, null) }
    }

    override fun skipToQueueItem(queue: Long) {
        launch { playbackVmController.getController().transportControls.skipToQueueItem(queue) }
    }

    override fun play() {
        launch { playbackVmController.getController().transportControls.play() }
    }

    override fun pause() {
        launch { playbackVmController.getController().transportControls.pause() }
    }

    override fun stop() {
        launch { playbackVmController.getController().transportControls.stop() }
    }

    override fun skipToPrev() {
        launch { playbackVmController.getController().transportControls.skipToPrevious() }
    }

    override fun skipToNext() {
        launch { playbackVmController.getController().transportControls.skipToNext() }
    }

    override fun seekTo(position: Long) {
        launch { playbackVmController.getController().transportControls.seekTo(position) }
    }

    override suspend fun getCurrentPlaybackState(): PlaybackStateCompat? =
        playbackVmController.getController().playbackState


    override suspend fun getCurrentMetadata(): MediaMetadataCompat? =
        playbackVmController.getController().metadata

    override fun playbackStateFlow(): SharedFlow<PlaybackStateCompat?> =
        playbackComponentState.state

    override fun metadataFlow(): SharedFlow<MediaMetadataCompat?> =
        playbackComponentState.metadata

    override fun releaseResources() {
        playbackVmBrowser.mediaBrowser.disconnect()
    }

}
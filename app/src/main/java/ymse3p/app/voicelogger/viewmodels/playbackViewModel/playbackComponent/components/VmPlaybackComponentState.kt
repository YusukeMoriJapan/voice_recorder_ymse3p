package ymse3p.app.voicelogger.viewmodels.playbackViewModel.playbackComponent.components

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ymse3p.app.voicelogger.di.playbackmodule.playbackVmModule.PlaybackVmProvidesModule
import javax.inject.Inject

@ViewModelScoped
class VmPlaybackComponentState @Inject constructor(
    @PlaybackVmProvidesModule.PlaybackVmCoroutineScope private val viewModelScope: CoroutineScope
) {
    /** state */
    private val _metadata = MutableSharedFlow<MediaMetadataCompat?>(1)
    val metadata: SharedFlow<MediaMetadataCompat?> = _metadata

    private val _state = MutableSharedFlow<PlaybackStateCompat?>(1)
    val state: SharedFlow<PlaybackStateCompat?> = _state

    private val _isConnectedController = MutableStateFlow(false)
    val isConnectedController: StateFlow<Boolean> = _isConnectedController

    private val _isInitializedController = MutableStateFlow(false)
    val isInitializedController: StateFlow<Boolean> = _isInitializedController


    fun setControllerConnectedState(connectState: Boolean) {
        viewModelScope.launch { _isConnectedController.emit(connectState) }
    }

    fun setControllerInitializedState(connectState: Boolean) {
        viewModelScope.launch { _isInitializedController.emit(connectState) }
    }

    fun setMetadata(metadata: MediaMetadataCompat) {
        viewModelScope.launch { _metadata.emit(metadata) }
    }

    fun setPlaybackState(playbackState: PlaybackStateCompat) {
        viewModelScope.launch { _state.emit(playbackState) }
    }

}
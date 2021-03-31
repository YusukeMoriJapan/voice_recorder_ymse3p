package ymse3p.app.audiorecorder.viewmodels.playbackViewModel.playbackComponent.components

import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ymse3p.app.audiorecorder.di.playbackmodule.playbackVmModule.PlaybackVmProvidesModule
import javax.inject.Inject

@ViewModelScoped
class PlaybackVmController @Inject constructor(
    @ApplicationContext private val context: Context,
    @PlaybackVmProvidesModule.PlaybackVmCoroutineScope private val viewModelScope: CoroutineScope,
    private val vmPlaybackComponentState: VmPlaybackComponentState,
    private val playbackVmBrowser: PlaybackVmBrowser,
) {
    private val controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            metadata?.let { viewModelScope.launch { (vmPlaybackComponentState.setMetadata(it)) } }
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            state?.let { viewModelScope.launch { vmPlaybackComponentState.setPlaybackState(it) } }
        }
    }

    private lateinit var mediaController: MediaControllerCompat


    init {
        viewModelScope.launch {
            vmPlaybackComponentState.isConnectedController.first {
                if (it) {
                    mediaController =
                        MediaControllerCompat(context, playbackVmBrowser.mediaBrowser.sessionToken)
                            .apply { registerCallback(controllerCallback) }
                    vmPlaybackComponentState.setControllerInitializedState(true)
                    true
                } else {
                    false
                }
            }
        }
    }


    suspend fun getController(): MediaControllerCompat {
        vmPlaybackComponentState.isInitializedController.first {
            return@first it
        }
        return mediaController
    }
}
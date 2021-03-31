package ymse3p.app.audiorecorder.services.playbackComponent.components

import android.media.AudioManager
import dagger.hilt.android.scopes.ServiceScoped
import javax.inject.Inject

@ServiceScoped
class OnAudioFocusChangeListenerImpl @Inject constructor(
    private val servicePlaybackComponentState: ServicePlaybackComponentState
) : AudioManager.OnAudioFocusChangeListener {
    override fun onAudioFocusChange(focusChange: Int) {
        servicePlaybackComponentState.setAudioFocusChange(focusChange)
    }
}
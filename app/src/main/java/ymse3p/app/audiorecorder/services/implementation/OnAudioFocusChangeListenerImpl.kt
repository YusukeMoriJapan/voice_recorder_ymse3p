package ymse3p.app.audiorecorder.services.implementation

import android.media.AudioManager
import android.support.v4.media.session.MediaSessionCompat
import ymse3p.app.audiorecorder.services.ServicePlaybackComponentState
import javax.inject.Inject

class OnAudioFocusChangeListenerImpl @Inject constructor(
    private val servicePlaybackComponentState: ServicePlaybackComponentState
) : AudioManager.OnAudioFocusChangeListener {
    override fun onAudioFocusChange(focusChange: Int) {
        servicePlaybackComponentState.setAudioFocusChange(focusChange)
    }
}
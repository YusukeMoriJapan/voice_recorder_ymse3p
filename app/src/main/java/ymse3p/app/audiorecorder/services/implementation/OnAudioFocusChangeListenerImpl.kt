package ymse3p.app.audiorecorder.services

import android.media.AudioManager
import android.support.v4.media.session.MediaSessionCompat
import javax.inject.Inject

class OnAudioFocusChangeListenerImpl @Inject constructor(
    private val mediaSession: MediaSessionCompat
) : AudioManager.OnAudioFocusChangeListener {
    override fun onAudioFocusChange(focusChange: Int) {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS)
            mediaSession.controller.transportControls.pause()
        else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
            mediaSession.controller.transportControls.pause()
        else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
        //音量を下げる
        else if (focusChange == AudioManager.AUDIOFOCUS_GAIN)
            mediaSession.controller.transportControls.play()
    }
}
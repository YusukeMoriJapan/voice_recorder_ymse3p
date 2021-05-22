package ymse3p.app.voicelogger.services.playbackComponent.components

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import dagger.hilt.android.scopes.ServiceScoped
import javax.inject.Inject

@ServiceScoped
class AudioMediaControllerCallback @Inject constructor(
) : MediaControllerCompat.Callback() {
    override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
//              createNotification()
    }

    override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
//        notificationManager.notify(
//            Constants.FOREGROUND_NOTIFICATION_ID_PLAYBACK,
//            notificationBuilder.build()
//        )
    }
}
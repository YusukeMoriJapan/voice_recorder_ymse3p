package ymse3p.app.audiorecorder.services.implementation

import android.app.NotificationManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import ymse3p.app.audiorecorder.services.AudioNotificationBuilder
import ymse3p.app.audiorecorder.util.Constants
import javax.inject.Inject

class AudioMediaControllerCallback @Inject constructor(
    private val notificationManager: NotificationManager,
    private val notificationBuilder: AudioNotificationBuilder
) : MediaControllerCompat.Callback() {
    override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
//              createNotification()
    }

    override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
//                notifyNotification()
        notificationManager.notify(
            Constants.FOREGROUND_NOTIFICATION_ID_PLAYBACK,
            notificationBuilder.build()
        )
    }
}
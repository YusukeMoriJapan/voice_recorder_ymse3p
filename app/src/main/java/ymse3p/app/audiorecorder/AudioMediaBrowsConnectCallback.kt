package ymse3p.app.audiorecorder

import android.content.Context
import android.os.RemoteException
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AudioMediaBrowsConnectCallback @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaController: MediaControllerCompat,
    private val subscriptionCallback: MediaBrowserCompat.SubscriptionCallback,
    private val mediaBrowser: MediaBrowserCompat,
    private val controllerCallback: MediaControllerCompat.Callback
) : MediaBrowserCompat.ConnectionCallback() {

    override fun onConnected() {
        try {
            if (mediaController.playbackState != null &&
                mediaController.playbackState.state == PlaybackStateCompat.STATE_PLAYING
            ) {
                controllerCallback.onMetadataChanged(mediaController.metadata)
                controllerCallback.onPlaybackStateChanged(mediaController.playbackState)
            }

        } catch (e: RemoteException) {
            e.printStackTrace()
            Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
        }

        mediaBrowser.subscribe(mediaBrowser.root, subscriptionCallback)
    }

}
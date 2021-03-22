package ymse3p.app.audiorecorder

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import javax.inject.Inject

class AudioMediaBrowsSubscriptionCallback @Inject constructor(
    private val mediaController: MediaControllerCompat
) : MediaBrowserCompat.SubscriptionCallback(
) {
//    override fun onChildrenLoaded(
//        parentId: String,
//        children: MutableList<MediaBrowserCompat.MediaItem>
//    ) {
//        if (mediaController.playbackState == null)
//            children[0].mediaId?.let { play(it) }
//    }

}

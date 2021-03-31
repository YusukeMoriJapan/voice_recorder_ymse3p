package ymse3p.app.audiorecorder.services

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface ServicePlaybackComponent {
    fun getCurrentMetadata(): MediaMetadataCompat?
    fun getCurrentPlaybackState(): PlaybackStateCompat?


    fun getSessionToken(): MediaSessionCompat.Token

    fun playbackStateFlow(): StateFlow<PlaybackStateCompat?>
    fun metadataFlow(): StateFlow<MediaMetadataCompat?>
    fun playingStateFlow(): StateFlow<Boolean>

    fun mediaItemListFlow(): StateFlow<MutableList<MediaBrowserCompat.MediaItem>?>

    fun releaseComponent()

}
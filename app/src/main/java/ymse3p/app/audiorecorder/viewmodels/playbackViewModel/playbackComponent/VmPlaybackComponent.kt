package ymse3p.app.audiorecorder.viewmodels.playbackViewModel.playbackComponent

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import kotlinx.coroutines.flow.SharedFlow

interface VmPlaybackComponent {

    fun playFromMediaId(id: String)

    fun skipToQueueItem(queue: Long)

    fun play()

    fun pause()

    fun stop()

    fun skipToPrev()

    fun skipToNext()

    fun seekTo(position: Long)

    suspend fun getCurrentPlaybackState(): PlaybackStateCompat?
    suspend fun getCurrentMetadata(): MediaMetadataCompat?

    fun playbackStateFlow(): SharedFlow<PlaybackStateCompat?>
    fun metadataFlow(): SharedFlow<MediaMetadataCompat?>

    fun releaseResources()
}
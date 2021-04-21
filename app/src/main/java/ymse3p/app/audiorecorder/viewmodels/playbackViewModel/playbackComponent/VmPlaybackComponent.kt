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

    /** Flowの場合は、PlaybackService内のMediaSessionの再生状態が変更されたときのみ値が
        emitされるため、ViewModel破棄後の再復帰時は、この関数で再生状態を取得すること*/
    suspend fun getCurrentPlaybackState(): PlaybackStateCompat?
    suspend fun getCurrentMetadata(): MediaMetadataCompat?

    fun playbackStateFlow(): SharedFlow<PlaybackStateCompat?>
    fun metadataFlow(): SharedFlow<MediaMetadataCompat?>

    fun releaseResources()
}
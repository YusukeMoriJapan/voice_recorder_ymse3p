package ymse3p.app.audiorecorder.services

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ymse3p.app.audiorecorder.data.Repository
import ymse3p.app.audiorecorder.di.playbackmodule.ServiceContext
import ymse3p.app.audiorecorder.util.Constants.Companion.FOREGROUND_NOTIFICATION_ID_PLAYBACK
import ymse3p.app.audiorecorder.util.Constants.Companion.MEDIA_METADATA_QUEUE
import ymse3p.app.audiorecorder.util.Constants.Companion.NOTIFICATION_CHANNEL_ID_PLAYBACK
import javax.inject.Inject

@AndroidEntryPoint
class AudioService : MediaBrowserServiceCompat() {

    private val rootId = "root"
    private val tag = AudioService::class.simpleName.orEmpty()
    private val readAudio by lazy { repository.localDataSource.readAudio() }
    private var isLoadingDatabase = MutableStateFlow<Boolean?>(null)


    @Inject
    lateinit var repository: Repository

    @Inject
    lateinit var simpleExoPlayer: SimpleExoPlayer

    @Inject
    lateinit var mediaSession: MediaSessionCompat

    @Inject
    lateinit var queueItems: MutableList<MediaSessionCompat.QueueItem>

    @Inject
    lateinit var audioMediaMetaData: MutableMap<Int, MediaMetadataCompat>

    @ServiceContext
    @Inject
    lateinit var serviceContext: Job

    /** System Manager */
    @Inject
    lateinit var notificationManager: NotificationManager

    /** callbacks */
    @Inject
    lateinit var mediaSessionCallback: MediaSessionCompat.Callback

    @Inject
    lateinit var mediaControllerCallback: MediaControllerCompat.Callback

    /** Notification */
    @Inject
    lateinit var notificationBuilder: AudioNotificationBuilder
    private lateinit var _notificationChannel: NotificationChannel
    private val notificationChannel get() = _notificationChannel


    override fun onCreate() {
        super.onCreate()
        sessionToken = mediaSession.sessionToken

        mediaSession.setCallback(mediaSessionCallback)
        mediaSession.controller.registerCallback(mediaControllerCallback)

        simpleExoPlayer.addListener(object : Player.EventListener {
            /** ExoPlayerの再生状態が変化したときに呼ばれる */
            override fun onPlaybackStateChanged(state: Int) {
                updatePlaybackState()
                notifyNotification()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlaybackState()
                notifyNotification()
            }
        })

        /** Media sessionに再生リスト(queue)を登録 */
        CoroutineScope(serviceContext).launch {
            readAudio.collect { audioEntityList ->
                isLoadingDatabase.value = true
                audioMediaMetaData.clear()
                audioEntityList.forEachIndexed { index, audioEntity ->
                    val mediaMetaData =
                        MediaMetadataCompat.Builder()
                            .putLong(
                                MEDIA_METADATA_QUEUE,
                                index.toLong()
                            )
                            .putString(
                                MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                                audioEntity.id.toString()
                            )
                            .putString(
                                MediaMetadataCompat.METADATA_KEY_TITLE,
                                audioEntity.audioTitle
                            )
                            .putLong(
                                MediaMetadataCompat.METADATA_KEY_DURATION,
                                audioEntity.audioDuration.toLong()
                            )
                            .build()
                    audioMediaMetaData[audioEntity.id] = mediaMetaData
                }

                queueItems.clear()
                audioMediaMetaData.forEach { mediaMetaData ->
                    queueItems.add(
                        MediaSessionCompat.QueueItem(
                            mediaMetaData.value.description,
                            mediaMetaData.value.getLong(MEDIA_METADATA_QUEUE)
                        )
                    )
                }
                mediaSession.setQueue(queueItems)
                isLoadingDatabase.value = false
            }
        }


        /** 500msごとに再生情報を更新 */
        CoroutineScope(serviceContext).launch(Dispatchers.Main) {
            while (true) {
                if (simpleExoPlayer.playbackState == Player.STATE_READY && simpleExoPlayer.playWhenReady) {
                    updatePlaybackState()
                }
                delay(500)
            }
        }
    }


    /** MediaBrowserがconnectを要求してきた時に呼び出され、MediaBrowserにroot名を返す*/
    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        Log.d(tag, "Connected from pkg:" + clientPackageName + "uid:" + clientUid)
        return BrowserRoot(rootId, null)
    }

    /** MediaBrowserがsubscribeを呼び出すと呼び出される */
    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {

        fun convertMetadataToMediaItem() {
            val metadata: List<MediaMetadataCompat> = audioMediaMetaData.values.toList()
            val mediaItems = MutableList(metadata.size) { i ->
                MediaBrowserCompat.MediaItem(
                    metadata[i].description,
                    MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                )
            }
            result.sendResult(mediaItems)
        }

        if (parentId == rootId) {
            if (isLoadingDatabase.value == false) {
                convertMetadataToMediaItem()
            } else {
                CoroutineScope(serviceContext).launch {
                    isLoadingDatabase.first {
                        if (it == false) {
                            convertMetadataToMediaItem()
                            true
                        } else {
                            false
                        }
                    }
                }
            }

        } else {
//            result.sendError(null)
            result.sendResult(mutableListOf())
        }

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        /** 通知用のチェンネルを登録 */
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            _notificationChannel =
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID_PLAYBACK,
                    "再生中のオーディオ",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    enableLights(false)
                    lightColor = Color.BLUE
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
            notificationManager.createNotificationChannel(notificationChannel)
        }

        startForeground(FOREGROUND_NOTIFICATION_ID_PLAYBACK, notificationBuilder.build())

//        /** 再生中以外はスワイプによる通知削除を許可 */
//        if (controller.playbackState?.state != PlaybackStateCompat.STATE_PLAYING)
//            stopForeground(false)
        return START_STICKY
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.d(tag, "onDestroy")
        serviceContext.cancel()
        mediaSession.apply {
            isActive = false
            release()
        }
        simpleExoPlayer.apply {
            stop()
            release()
        }
        notificationManager.cancelAll()
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
    }


    /** 「ExoPlayerが保持している現在の再生状態」を、MediaSessionの再生状態に反映する。
    　　再生位置情報も含むため定期的に更新する　*/
    private fun updatePlaybackState() {

        /** ExoPlayerの再生状態を、MediaSession向けの情報に変換 */
        var state = PlaybackStateCompat.STATE_NONE
        when (simpleExoPlayer.playbackState) {
            Player.STATE_IDLE -> {
                state = PlaybackStateCompat.STATE_NONE
            }
            Player.STATE_BUFFERING -> {
                state = PlaybackStateCompat.STATE_BUFFERING
            }
            Player.STATE_READY -> {
                state = if (simpleExoPlayer.playWhenReady)
                    PlaybackStateCompat.STATE_PLAYING
                else
                    PlaybackStateCompat.STATE_PAUSED
            }
            Player.STATE_ENDED -> {
                state = PlaybackStateCompat.STATE_STOPPED
            }
        }

        /** 変換したデータを元にMediaSessionの再生状態を更新 */
        val playbackState =
            PlaybackStateCompat.Builder()
                /** MediaButtonIntentで可能な操作を設定 */
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                            PlaybackStateCompat.ACTION_STOP or
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                )
                .setState(
                    state,
                    simpleExoPlayer.currentPosition,
                    simpleExoPlayer.playbackParameters.speed
                )
                .build()
        mediaSession.setPlaybackState(playbackState)
    }


    private fun notifyNotification() {
        notificationManager.notify(FOREGROUND_NOTIFICATION_ID_PLAYBACK, notificationBuilder.build())
    }

}
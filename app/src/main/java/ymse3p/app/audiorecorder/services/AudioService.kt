package ymse3p.app.audiorecorder.services

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import androidx.media.MediaBrowserServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import ymse3p.app.audiorecorder.di.playbackmodule.servicePlaybackModule.ServiceCoroutineScope
import ymse3p.app.audiorecorder.services.playbackComponent.ServicePlaybackComponent
import ymse3p.app.audiorecorder.util.Constants.Companion.FOREGROUND_NOTIFICATION_ID_PLAYBACK
import ymse3p.app.audiorecorder.util.Constants.Companion.NOTIFICATION_CHANNEL_ID_PLAYBACK
import javax.inject.Inject

@AndroidEntryPoint
class AudioService : MediaBrowserServiceCompat() {

    private val rootId = "root"
    private val tag = AudioService::class.simpleName.orEmpty()

    @ServiceCoroutineScope
    @Inject
    lateinit var serviceScope: CoroutineScope

    @Inject
    lateinit var playbackComponent: ServicePlaybackComponent

    /** System Manager */
    @Inject
    lateinit var notificationManager: NotificationManager

    /** Notification */
    @Inject
    lateinit var notificationBuilder: AudioNotificationBuilder
    private lateinit var _notificationChannel: NotificationChannel
    private val notificationChannel get() = _notificationChannel


    override fun onCreate() {
        super.onCreate()
        sessionToken = playbackComponent.getSessionToken()

        serviceScope.launch {
            playbackComponent.playingStateFlow().collect {
                notifyNotification()
            }
            playbackComponent.metadataFlow().collect {
                notifyNotification()
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
        val mediaItemList = playbackComponent.mediaItemListFlow().value

        if (parentId == rootId && mediaItemList != null) result.sendResult(mediaItemList)
        else result.sendResult(mutableListOf())
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
        serviceScope.cancel()
        playbackComponent.releaseComponent()
        notificationManager.cancelAll()
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
    }


    private fun notifyNotification() {
        notificationManager.notify(FOREGROUND_NOTIFICATION_ID_PLAYBACK, notificationBuilder.build())
    }


}
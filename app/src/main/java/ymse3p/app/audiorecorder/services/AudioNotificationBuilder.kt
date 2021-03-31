package ymse3p.app.audiorecorder.services

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import ymse3p.app.audiorecorder.ui.MainActivity
import ymse3p.app.audiorecorder.R
import ymse3p.app.audiorecorder.util.Constants
import javax.inject.Inject

@ServiceScoped
class AudioNotificationBuilder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playbackComponent: ServicePlaybackComponent
) {

    /** 現在再生している曲のMediaMetadataを取得　*/
//    private val controller get() = mediaSession.controller
//    private val mediaMetadata get() = controller.metadata
//    private val description get() = mediaMetadata?.description

    /** 通知をクリックしてActivityを開くIntentを作成 */
    private val pendingIntents: PendingIntent by lazy {
        val openUi = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
        }

        PendingIntent.getActivity(
            context,
            1,
            openUi,
            PendingIntent.FLAG_CANCEL_CURRENT
        )
    }

    /** 通知バーのアクションで使用するPendingIntentを定義 */
    private val pendingIntentPrev = MediaButtonReceiver.buildMediaButtonPendingIntent(
        context, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
    )
    private val pendingIntentNext = MediaButtonReceiver.buildMediaButtonPendingIntent(
        context, PlaybackStateCompat.ACTION_SKIP_TO_NEXT
    )
    private val pendingIntentPause = MediaButtonReceiver.buildMediaButtonPendingIntent(
        context, PlaybackStateCompat.ACTION_PAUSE
    )
    private val pendingIntentPlay = MediaButtonReceiver.buildMediaButtonPendingIntent(
        context, PlaybackStateCompat.ACTION_PLAY
    )

    /** 通知に使用するスタイルを定義 */
    private val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle().run {
        setMediaSession(playbackComponent.getSessionToken())
        /** 通知を小さくたたんだ時に表示されるコントロールのインデックスを定義 */
        setShowActionsInCompactView(0)
        return@run this
    }

    fun build(): Notification =
        NotificationCompat.Builder(
            context,
            Constants.NOTIFICATION_CHANNEL_ID_PLAYBACK
        )
            .run {
//                  setContentText(description.subtitle)
//                  setSubText(description.description)
//                  setLargeIcon(description.iconBitmap)
                /** 通知領域色の設定　*/
//                  setColor(ContextCompat.getColor(this@AudioService, R.color.colorAccent))


                /** 通知クリック時のインテントを設定 */
                setContentIntent(pendingIntents)
                /** 通知がスワイプして消された際のインテントを設定　*/
                setDeleteIntent(
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        context,
                        PlaybackStateCompat.ACTION_STOP
                    )
                )
                /** ロック画面でも通知を表示する */
                setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                setStyle(mediaStyle)
                setNotificationSilent()

                setSmallIcon(R.drawable.ic_logo)

                playbackComponent.getCurrentMetadata()?.description?.title?.let {
                    setContentTitle(it)
                }


                /** 通知バーにアクションを設定*/
                if (playbackComponent.getCurrentPlaybackState()?.state
                    == PlaybackStateCompat.STATE_PLAYING
                ) {
                    addAction(
                        NotificationCompat.Action(
                            R.drawable.exo_controls_pause, "pause",
                            pendingIntentPause
                        )
                    )
                } else {
                    addAction(
                        NotificationCompat.Action(
                            R.drawable.exo_controls_play, "play",
                            pendingIntentPlay
                        )
                    )
                }.build()
                addAction(
                    NotificationCompat.Action(
                        R.drawable.exo_controls_previous,
                        "prev", pendingIntentPrev
                    )
                )
                addAction(
                    NotificationCompat.Action(
                        R.drawable.exo_controls_next, "next",
                        pendingIntentNext
                    )
                ).build()
            }
}
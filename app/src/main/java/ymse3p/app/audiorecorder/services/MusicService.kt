package ymse3p.app.audiorecorder.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import ymse3p.app.audiorecorder.MainActivity
import ymse3p.app.audiorecorder.R
import ymse3p.app.audiorecorder.data.Repository
import ymse3p.app.audiorecorder.data.database.entities.AudioEntity
import ymse3p.app.audiorecorder.util.Constants.Companion.FOREGROUND_NOTIFICATION_ID_PLAYBACK
import ymse3p.app.audiorecorder.util.Constants.Companion.NOTIFICATION_CHANNEL_ID_PLAYBACK
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : MediaBrowserServiceCompat() {

    private val audioManager by lazy {
        getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    @Inject
    lateinit var repository: Repository

    @Inject
    lateinit var simpleExoPlayer: SimpleExoPlayer

    private val tag = MusicService::class.simpleName.orEmpty()
    private var index = 0
    private val rootId = "root"

    private val mediaSession: MediaSessionCompat by lazy {
        MediaSessionCompat(applicationContext, tag).apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS or
                        MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS
            )
            setCallback(MusicSessionCallBack())
            controller.registerCallback(object : MediaControllerCompat.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                    createNotification()
                }

                override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
                    createNotification()
                }
            })
        }
    }

    private val queueItems = mutableListOf<MediaSessionCompat.QueueItem>()

    private val afChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS)
            mediaSession.controller.transportControls.pause()
        else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
            mediaSession.controller.transportControls.pause()
        else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
        //音量を下げる
        else if (focusChange == AudioManager.AUDIOFOCUS_GAIN)
            mediaSession.controller.transportControls.play()
    }
    private val serviceContext = Job()

    /** Audio focusのリクエストに必要なインスタンスの生成　*/
    private val audioAttributes by lazy {
        AudioAttributesCompat.Builder().run {
            setUsage(AudioAttributesCompat.USAGE_MEDIA)
            setContentType(AudioAttributesCompat.CONTENT_TYPE_SPEECH)
            build()
        }
    }
    private val audioFocusRequest by lazy {
        AudioFocusRequestCompat
            .Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener(afChangeListener).build()
    }

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onCreate() {
        super.onCreate()
        sessionToken = mediaSession.sessionToken

        simpleExoPlayer.addListener(object : Player.EventListener {
            /** ExoPlayerの再生状態が変化したときに呼ばれる */
            override fun onPlaybackStateChanged(state: Int) {
                updatePlaybackState()
            }
        })

        /** Media sessionに再生リスト(queue)を登録 */
        for ((i, media) in musicMediaItem.withIndex()) {
            queueItems.add(MediaSessionCompat.QueueItem(media.description, i.toLong()))
        }
        mediaSession.setQueue(queueItems)

        /** 500msごとに再生情報を更新 */
        CoroutineScope(serviceContext).launch(Dispatchers.Main) {
            while (true) {
                if (simpleExoPlayer.playbackState == Player.STATE_READY && simpleExoPlayer.playWhenReady) {
                    updatePlaybackState()
                }
                delay(500)
            }
        }

        /** 通知用のチェンネルを登録 */
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel =
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID_PLAYBACK,
                    "再生中のオーディオ",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    enableLights(true)
                    lightColor = Color.BLUE
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
            notificationManager.createNotificationChannel(channel)
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
        if (parentId == rootId) {
            result.sendResult(musicMediaItem)
        } else {
//            result.sendError(null)
            result.sendResult(mutableListOf())
        }
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
    }


    /** 「ExoPlayer保持している現在の再生状態」を、MediaSessionの再生状態に反映する。
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
                            PlaybackStateCompat.ACTION_STOP
                )
                .setState(
                    state,
                    simpleExoPlayer.currentPosition,
                    simpleExoPlayer.playbackParameters.speed
                )
                .build()
        mediaSession.setPlaybackState(playbackState)
    }


    /** 通知を作成。サービスをForegroundに設定 */
    private fun createNotification() {
        /** 現在再生している曲のMediaMetadataを取得　*/
        val controller = mediaSession.controller
        val mediaMetadata = controller.metadata

        if (mediaMetadata == null && mediaSession.isActive) return

        val description = mediaMetadata?.description

        /** 通知をクリックしてActivityを開くIntentを作成 */
        fun createContentIntent(): PendingIntent {
            val openUi = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
            }

            return PendingIntent.getActivity(this, 1, openUi, PendingIntent.FLAG_CANCEL_CURRENT)
        }

        /** 通知バーのアクションで使用するPendingIntentを定義 */
        val pendingIntentPrev = MediaButtonReceiver.buildMediaButtonPendingIntent(
            this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        )
        val pendingIntentNext = MediaButtonReceiver.buildMediaButtonPendingIntent(
            this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        )
        val pendingIntentPause = MediaButtonReceiver.buildMediaButtonPendingIntent(
            this, PlaybackStateCompat.ACTION_PAUSE
        )
        val pendingIntentPlay = MediaButtonReceiver.buildMediaButtonPendingIntent(
            this, PlaybackStateCompat.ACTION_PLAY
        )

        /** 通知に使用するスタイルを定義 */
        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle().run {
            setMediaSession(mediaSession.sessionToken)

            /** 通知を小さくたたんだ時に表示されるコントロールのインデックスを定義 */
            setShowActionsInCompactView(1)
            return@run this
        }

        val notificationBuilder =
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID_PLAYBACK)
                .apply {
//                  setContentText(description.subtitle)
//                  setSubText(description.description)
//                  setLargeIcon(description.iconBitmap)
                    /** 通知領域色の設定　*/
//                  setColor(ContextCompat.getColor(this@MusicService, R.color.colorAccent))

                    setContentTitle(description?.title)
                    /** 通知クリック時のインテントを設定 */
                    setContentIntent(createContentIntent())
                    /** 通知がスワイプして消された際のインテントを設定　*/
                    setDeleteIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            this@MusicService,
                            PlaybackStateCompat.ACTION_STOP
                        )
                    )
                    /** ロック画面でも通知を表示する */
                    setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    setStyle(mediaStyle)

                    setSmallIcon(R.drawable.ic_launcher_foreground)

                    /** 通知バーにアクションを設定*/
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
                    )

                    /** 再生状態で表示するアクションを分岐 */
                    if (controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING)
                        addAction(
                            NotificationCompat.Action(
                                R.drawable.exo_controls_pause, "pause",
                                pendingIntentPause
                            )
                        )
                    else
                        addAction(
                            NotificationCompat.Action(
                                R.drawable.exo_controls_play, "play",
                                pendingIntentPlay
                            )
                        )
                }

        notificationManager.notify(FOREGROUND_NOTIFICATION_ID_PLAYBACK,notificationBuilder.build())

        /** 再生中以外はスワイプによる通知削除を許可 */
        if (controller.playbackState.state != PlaybackStateCompat.STATE_PLAYING)
            stopForeground(false)

    }


    inner class MusicSessionCallBack : MediaSessionCompat.Callback() {

        /** TransportController#playFromMediaId　に対して呼び出される */
        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            if (mediaId == null) {
                return
            } else {
                val readAudioEntity: Flow<AudioEntity> =
                    repository.localDataSource.readAudioFromId(mediaId.toInt())
                CoroutineScope(serviceContext).launch {
                    readAudioEntity.first { audioEntity ->
                        if (audioEntity != null) {
                            withContext(Dispatchers.Main) {
                                simpleExoPlayer.setMediaItem(MediaItem.fromUri(audioEntity.audioUri))
                                simpleExoPlayer.prepare()
                                mediaSession.isActive = true
                                onPlay()
                            }
                            /** 再生中の曲情報(MediaSessionが配信している曲)を設定 */
                            mediaSession.setMetadata(musicMediaMetadata[mediaId])
                        }
                        return@first true
                    }
                }
//                var audioEntity: AudioEntity? = null
//                runBlocking {
//                    readAudioEntity.first {
//                        audioEntity = it
//                        true
//                    }
//                }
//                audioEntity?.run {
//                    simpleExoPlayer.setMediaItem(MediaItem.fromUri(audioUri))
//                    simpleExoPlayer.prepare()
//                    mediaSession.isActive = true
//                    onPlay()
//                    /** 再生中の曲情報(MediaSessionが配信している曲)を設定 */
//                    mediaSession.setMetadata(musicMediaMetadata[mediaId])
//                }
//                return
            }
        }

        /** MediaController#play　に対して呼び出される */
        override fun onPlay() {
            val audioFocusRequestResult =
                AudioManagerCompat
                    .requestAudioFocus(audioManager, audioFocusRequest)

            /** Audio focusを要求 */
            if (audioFocusRequestResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                /** focus要求が許可されたら再生を開始 */
                mediaSession.isActive = true
                simpleExoPlayer.playWhenReady = true
            }
        }

        override fun onPause() {
            simpleExoPlayer.playWhenReady = false
            /** オーディオフォーカスを開放する　*/
            AudioManagerCompat.abandonAudioFocusRequest(audioManager, audioFocusRequest)
        }

        override fun onStop() {
            onPause()
            mediaSession.isActive = false
            /** オーディオフォーカスを開放する　*/
            AudioManagerCompat.abandonAudioFocusRequest(audioManager, audioFocusRequest)
        }

        override fun onSeekTo(pos: Long) {
            simpleExoPlayer.seekTo(pos)
        }

        override fun onSkipToNext() {
            index++
            /** ライブラリの最後まで再生されてる場合は0に戻す */
            if (index >= queueItems.size) index = 0

            onPlayFromMediaId(queueItems[index].description.mediaId, null)
        }

        override fun onSkipToPrevious() {
            index--
            /** インデックスがマイナスの場合は、最後の曲を再生する*/
            if (index < 0) index = queueItems.size - 1

            onPlayFromMediaId(queueItems[index].description.mediaId, null)
        }

        /** WearやAutoでキュー内のアイテムを選択された際にも呼び出される */
        override fun onSkipToQueueItem(id: Long) {
            onPlayFromMediaId(queueItems[id.toInt()].description.mediaId, null)
        }

        override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
            return super.onMediaButtonEvent(mediaButtonEvent)
        }
    }

    /** media library*/
    private val musicMediaMetadata = mutableMapOf<String, MediaMetadataCompat>(
        "1" to MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "1")
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "JAzz in Paris")
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 2000)
            .build(),
        "2" to MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "2")
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "JAzz in Tokyo")
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 2000)
            .build()
    )

    private val musicMediaItem by lazy {
        val metadata: List<MediaMetadataCompat> = musicMediaMetadata.values.toList()
        MutableList(2) { i ->
            MediaBrowserCompat.MediaItem(
                metadata[i].description,
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            )
        }
    }
}
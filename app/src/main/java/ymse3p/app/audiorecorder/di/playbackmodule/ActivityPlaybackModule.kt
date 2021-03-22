package ymse3p.app.audiorecorder.di.playbackmodule

import android.content.ComponentName
import android.content.Context
import android.media.browse.MediaBrowser
import android.os.RemoteException
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.Toast
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import ymse3p.app.audiorecorder.AudioMediaBrowsConnectCallback
import ymse3p.app.audiorecorder.AudioMediaBrowsSubscriptionCallback
import ymse3p.app.audiorecorder.services.AudioService


@Module
@InstallIn(ActivityRetainedComponent::class)
object ActivityPlaybackModule {
//
//    @ActivityRetainedScoped
//    @Provides
//    fun provideMediaBrowser(
//        @ApplicationContext context: Context,
////        mediaController:MediaControllerCompat,
////        controllerCallback:MediaControllerCompat.Callback
//    ): MediaBrowserCompat {
//
//        val connectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
//            override fun onConnected() {
////                try {
////                    if (mediaController.playbackState != null &&
////                        mediaController.playbackState.state == PlaybackStateCompat.STATE_PLAYING
////                    ) {
//////                        controllerCallback.onMetadataChanged(mediaController.metadata)
//////                        controllerCallback.onPlaybackStateChanged(mediaController.playbackState)
////                    }
////
////                } catch (e: RemoteException) {
////                    e.printStackTrace()
////                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
////                }
//
////                mediaBrowser.subscribe(mediaBrowser.root, subscriptionCallback)
//            }
//        }
//
//        return MediaBrowserCompat(
//            context,
//            ComponentName(context, AudioService::class.java),
//            connectionCallback,
//            null
//        )
//    }
//
//    @ActivityRetainedScoped
//    @Provides
//    fun provideMediaController(
//        @ApplicationContext context: Context,
//        mediaBrowser: MediaBrowserCompat
//    ): MediaControllerCompat {
//        return MediaControllerCompat(context, mediaBrowser.sessionToken)
////            .apply { registerCallback(controllerCallback) }
//
//    }
//}
//
//@Module
//@InstallIn(ActivityRetainedComponent::class)
//abstract class ActivityPlaybackBindsModule {
//
//    @ActivityRetainedScoped
//    @Binds
//    abstract fun bindMediaBrowserSubscriptionCallback(
//        subscriptionCallback: AudioMediaBrowsSubscriptionCallback
//    ): MediaBrowserCompat.SubscriptionCallback
//
////    @ActivityRetainedScoped
////    @Binds
////    abstract fun bindMediaBrowserConnectCallback(
////        connectionCallback: AudioMediaBrowsConnectCallback
////    ): MediaBrowserCompat.ConnectionCallback

}
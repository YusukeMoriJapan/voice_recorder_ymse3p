package ymse3p.app.audiorecorder

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.RemoteException
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.google.android.exoplayer2.SimpleExoPlayer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import ymse3p.app.audiorecorder.databinding.ActivityMainBinding
import ymse3p.app.audiorecorder.services.AudioService
import ymse3p.app.audiorecorder.util.CannotSaveAudioException
import ymse3p.app.audiorecorder.util.CannotStartRecordingException
import ymse3p.app.audiorecorder.util.Constants.Companion.REQUEST_RECORD_AUDIO_PERMISSION
import ymse3p.app.audiorecorder.viewmodels.MainViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var simpleExoPlayer: SimpleExoPlayer

    private lateinit var _binding: ActivityMainBinding
    private val binding get() = _binding
    private val mainViewModel by lazy { ViewModelProvider(this).get(MainViewModel::class.java) }

    private val mediaBrowser: MediaBrowserCompat by lazy {

        val subscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
            override fun onChildrenLoaded(
                parentId: String,
                children: MutableList<MediaBrowserCompat.MediaItem>
            ) {
                if (mediaController.playbackState == null)
                    children[0].mediaId?.let { play(it) }
            }
        }

        val connectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
            override fun onConnected() {
                try {
                    if (mediaController.playbackState != null && mediaController.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                        controllerCallback.onMetadataChanged(mediaController.metadata)
                        controllerCallback.onPlaybackStateChanged(mediaController.playbackState)
                    }

                } catch (e: RemoteException) {
                    e.printStackTrace()
                    Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                }

                mediaBrowser.subscribe(mediaBrowser.root, subscriptionCallback)
            }
        }
        MediaBrowserCompat(
            this, ComponentName(this, AudioService::class.java),
            connectionCallback, null
        )
    }

    private val mediaController: MediaControllerCompat by lazy {
        MediaControllerCompat(this, mediaBrowser.sessionToken)
            .apply { registerCallback(controllerCallback) }
    }

    private val controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, AudioService::class.java))
        } else {
            startService(Intent(this, AudioService::class.java))
        }

        mediaBrowser.connect()

        mainViewModel
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        lifecycleScope.launchWhenCreated {
            mainViewModel.isRecording.collect { isRecording ->
                if (isRecording) {
                    binding.mic.setImageResource(R.drawable.ic_baseline_stop_24)
                } else {
                    binding.mic.setImageResource(R.drawable.ic_baseline_mic_24)
                }
            }
        }

        binding.mic.setOnClickListener {
            if (mainViewModel.isRecording.value) {
                try {
                    mainViewModel.stopRecording()
                    Toast.makeText(this, "録音を終了しました", Toast.LENGTH_SHORT).show()
                    findNavController(R.id.nav_host_fragment).navigate(R.id.action_global_audioSaveBottomSheet)
                } catch (e: CannotSaveAudioException) {
                    Toast.makeText(this, "エラー発生のため、録音データは保存されませんでした。", Toast.LENGTH_SHORT).show()
                }
            } else {
                requestPermissions(
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    REQUEST_RECORD_AUDIO_PERMISSION
                )
            }
        }
        binding.playerControlView.player = simpleExoPlayer
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            lifecycleScope.launchWhenCreated {
                grantResults.forEach {
                    if (it == PackageManager.PERMISSION_GRANTED) {
                        try {
                            mainViewModel.startRecording()
                            Toast.makeText(this@MainActivity, "録音を開始しました", Toast.LENGTH_SHORT)
                                .show()
                            return@launchWhenCreated
                        } catch (e: CannotStartRecordingException) {
                            Toast.makeText(this@MainActivity, "エラーが発生しました", Toast.LENGTH_SHORT)
                                .show()
                            return@launchWhenCreated
                        }
                    }
                }
                Toast.makeText(
                    this@MainActivity,
                    "録音機能の使用を許可して下さい",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaBrowser.disconnect()
        if (mediaController.playbackState.state != PlaybackStateCompat.STATE_PLAYING)
            stopService(Intent(this, AudioService::class.java))
    }

    private fun play(id: String) {
        mediaController.transportControls.playFromMediaId(id, null)
    }
}
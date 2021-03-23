package ymse3p.app.audiorecorder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import ymse3p.app.audiorecorder.databinding.ActivityMainBinding
import ymse3p.app.audiorecorder.util.CannotSaveAudioException
import ymse3p.app.audiorecorder.util.CannotStartRecordingException
import ymse3p.app.audiorecorder.util.Constants.Companion.REQUEST_RECORD_AUDIO_PERMISSION
import ymse3p.app.audiorecorder.viewmodels.MainViewModel
import ymse3p.app.audiorecorder.viewmodels.PlayBackViewModel
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var _binding: ActivityMainBinding
    private val binding get() = _binding
    private val mainViewModel by viewModels<MainViewModel>()
    private val playbackViewModel by viewModels<PlayBackViewModel>()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launchWhenCreated {
            playbackViewModel.metadata.collect { metadata ->
                changeMetadata(metadata)
            }
        }

        lifecycleScope.launchWhenCreated {
            playbackViewModel.state.collect { state ->
                changePlaybackState(state)
            }
        }

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

        /** 再生用UIの設定 */
        binding.buttonPrev.setOnClickListener {
            playbackViewModel.skipToPrev()
        }
        binding.buttonNext.setOnClickListener {
            playbackViewModel.skipToNext()
        }
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    playbackViewModel.seekTo(it.progress.toLong())
                }
            }
        })

    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launchWhenStarted {
            changeMetadata(playbackViewModel.getMetadata())
            changePlaybackState(playbackViewModel.getPlaybackState())
        }
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

    private fun changeMetadata(metadata: MediaMetadataCompat?) {
        metadata?.let {
            binding.textViewTitle.text = metadata.description.title
            binding.textViewDuration.text = milliSecToTimeString(
                it.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
            )
            binding.seekBar.max =
                it.getLong(MediaMetadataCompat.METADATA_KEY_DURATION).toInt()
        }
    }

    private fun changePlaybackState(state: PlaybackStateCompat?) {
        if (state !== null) {
            if (state.state == PlaybackStateCompat.STATE_PLAYING) {
                binding.buttonPlay.apply {
                    setOnClickListener {
                        playbackViewModel.pause()
                    }
                    setImageResource(R.drawable.ic_baseline_pause_24)
                }
            } else {
                binding.buttonPlay.apply {
                    setOnClickListener {
                        playbackViewModel.play()
                    }
                    setImageResource(R.drawable.ic_baseline_play_arrow_24)
                }
            }

            binding.textViewPosition.text = milliSecToTimeString(state.position)
            binding.seekBar.progress = state.position.toInt()
        }

    }


    private fun milliSecToTimeString(duration: Long): String {
        val minutes =
            TimeUnit.MILLISECONDS.toMinutes(duration)
        val seconds =
            TimeUnit.MILLISECONDS.toSeconds(duration)
        return "${minutes}:${seconds}"
    }


}
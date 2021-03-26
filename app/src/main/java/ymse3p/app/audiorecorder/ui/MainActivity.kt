package ymse3p.app.audiorecorder.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import ymse3p.app.audiorecorder.R
import ymse3p.app.audiorecorder.databinding.ActivityMainBinding
import ymse3p.app.audiorecorder.util.CannotSaveAudioException
import ymse3p.app.audiorecorder.util.CannotStartRecordingException
import ymse3p.app.audiorecorder.util.Constants.Companion.REQUEST_RECORD_AUDIO_PERMISSION
import ymse3p.app.audiorecorder.viewmodels.MainViewModel
import ymse3p.app.audiorecorder.viewmodels.PlayBackViewModel
import java.util.*
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    /** Data Bindings */
    private lateinit var _binding: ActivityMainBinding
    private val binding get() = _binding

    /** ViewModels */
    private val mainViewModel by viewModels<MainViewModel>()
    private val playbackViewModel by viewModels<PlayBackViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /** UIの初期化 */
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = "Audio List"

        /** コルーチン起動 */
        // metadataの変更を受け取る
        lifecycleScope.launchWhenCreated {
            playbackViewModel.metadata.collect { metadata ->
                changeMetadata(metadata)
            }
        }
        // playbackStateの変更を受け取る
        lifecycleScope.launchWhenCreated {
            playbackViewModel.state.collect { state ->
                if (binding.linearLayoutBottom.visibility == View.GONE)
                    binding.linearLayoutBottom.visibility = View.VISIBLE
                changePlaybackState(state)
            }
        }
        // 録音状態の変更を受け取る
        lifecycleScope.launchWhenCreated {
            mainViewModel.isRecording.collect { isRecording ->
                if (isRecording) {
                    binding.mic.setImageResource(R.drawable.ic_baseline_stop_24)
                } else {
                    binding.mic.setImageResource(R.drawable.ic_baseline_mic_24)
                }
            }
        }

        /** 録音用UIの設定　*/
        binding.mic.setOnClickListener {
            if (mainViewModel.isRecording.value) {
                try {
                    mainViewModel.stopRecording()
                    Snackbar.make(
                        binding.mainActivitySnackBar, "録音を終了しました", Snackbar.LENGTH_SHORT
                    ).show()
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
        // onStart()時の音声再生状態を受け取る
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
            R.id.sample_data_generate -> {
                mainViewModel.insertSampleAudio()
                true
            }
            R.id.all_sample_data_delete -> {
                mainViewModel.deleteAllSampleAudio()
                true
            }
            R.id.delete_all_data -> {
                mainViewModel.deleteAllAudio()
                true
            }
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
                            Snackbar.make(
                                binding.mainActivitySnackBar, "録音を開始しました", Snackbar.LENGTH_SHORT
                            ).show()
                            return@launchWhenCreated
                        } catch (e: CannotStartRecordingException) {
                            Snackbar.make(
                                binding.mainActivitySnackBar, "エラーが発生しました", Snackbar.LENGTH_SHORT
                            ).show()
                            return@launchWhenCreated
                        }
                    }
                }
                Snackbar.make(
                    binding.mainActivitySnackBar, "録音機能の使用を許可して下さい", Snackbar.LENGTH_SHORT
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
            TimeUnit.MILLISECONDS.toMinutes(duration).toString()
        var seconds = (
                TimeUnit.MILLISECONDS.toSeconds(duration) % 60).toString()
        if (seconds.length == 1) seconds = "0$seconds"
        return "${minutes}:${seconds}"
    }


}
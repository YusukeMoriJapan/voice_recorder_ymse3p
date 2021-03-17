package ymse3p.app.audiorecorder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import ymse3p.app.audiorecorder.data.database.dataStore
import ymse3p.app.audiorecorder.databinding.ActivityMainBinding
import ymse3p.app.audiorecorder.util.CannotSaveAudioException
import ymse3p.app.audiorecorder.util.CannotStartRecordingException
import ymse3p.app.audiorecorder.util.Constants.Companion.REQUEST_RECORD_AUDIO_PERMISSION
import ymse3p.app.audiorecorder.viewmodels.MainViewModel

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var _binding: ActivityMainBinding
    private val binding get() = _binding
    private val mainViewModel by lazy { ViewModelProvider(this).get(MainViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    override fun onBackPressed() {
        super.onBackPressed()
    }
}
package ymse3p.app.audiorecorder.viewmodels

import android.app.Application
import android.media.MediaRecorder
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ymse3p.app.audiorecorder.data.Repository
import ymse3p.app.audiorecorder.util.CannotSaveAudioException
import ymse3p.app.audiorecorder.util.CannotStartRecordingException
import java.io.File
import java.io.IOException
import java.lang.IllegalStateException
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: Repository,
    application: Application,
    private val audioRecorder: MediaRecorder
) : AndroidViewModel(application) {

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    fun startRecording() {
        try {
            audioRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(
                    File(getApplication<Application>().filesDir, "default_name.mp4").toString()
                )
                prepare()
                start()
            }
            _isRecording.value = true
        } catch (e: IllegalStateException) {
            Log.e("MediaRecorder", e.message.orEmpty())
            throw CannotStartRecordingException("IllegalStateException occurred")

        } catch (e: IOException) {
            Log.e("MediaRecorder", e.message.orEmpty())
            throw CannotStartRecordingException("IllegalStateException occurred")
        }
    }

    fun stopRecording() {
        try {
            audioRecorder.stop()
            _isRecording.value = false
        } catch (e: IllegalStateException) {
            Log.e("MediaRecorder", e.message.orEmpty())
            audioRecorder.reset()
            _isRecording.value = false
            throw CannotSaveAudioException("IllegalStateException occurred")
        } catch (e: RuntimeException) {
            Log.e("MediaRecorder", e.message.orEmpty())
            File(getApplication<Application>().filesDir, "default_name.mp4").delete()
            audioRecorder.reset()
            _isRecording.value = false
            throw CannotSaveAudioException("RuntimeException occurred")
        }
    }


    override fun onCleared() {
        super.onCleared()
        stopRecording()
        audioRecorder.release()

    }
}
package ymse3p.app.audiorecorder.viewmodels

import android.app.Application
import android.media.MediaRecorder
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ymse3p.app.audiorecorder.MyApplication
import ymse3p.app.audiorecorder.data.Repository
import ymse3p.app.audiorecorder.util.MyAudioRecorder
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: Repository,
    application: Application,
    val myAudioRecorder: MyAudioRecorder
) : AndroidViewModel(application) {

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    fun changeRecordingState(recordingState: Boolean) {
        _isRecording.value = recordingState
    }


    override fun onCleared() {
        super.onCleared()
        myAudioRecorder.apply {
            stopRecording(isRecording.value)
            release()
        }
    }
}
package ymse3p.app.voicelogger

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.MutableStateFlow

@HiltAndroidApp
class MyApplication : Application() {
    val playbackSpeedFlow = MutableStateFlow(1F)
    val skipSilenceFlow = MutableStateFlow(false)
}
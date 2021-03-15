package ymse3p.app.audiorecorder.util

import java.lang.RuntimeException

class CannotSaveAudioException(message: String) : RuntimeException(message) {
}

class CannotStartRecordingException(message: String) : RuntimeException(message) {
}
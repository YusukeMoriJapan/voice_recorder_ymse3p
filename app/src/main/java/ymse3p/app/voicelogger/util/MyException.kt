package ymse3p.app.voicelogger.util

import java.lang.RuntimeException

class CannotSaveAudioException(message: String) : RuntimeException(message) {
}

class CannotStartRecordingException(message: String) : RuntimeException(message) {
}

class CannotCollectGpsLocationException(message: String) : RuntimeException(message) {
}
package ymse3p.app.audiorecorder.util

import android.app.Application
import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import android.widget.Toast
import ymse3p.app.audiorecorder.MyApplication
import java.io.File
import java.io.IOException
import java.lang.IllegalStateException
import java.lang.RuntimeException

class MyAudioRecorder(val context: Context) : MediaRecorder() {

    fun startRecording(recordingState: Boolean): Boolean {
        try {
            setAudioSource(AudioSource.MIC)
            setOutputFormat(OutputFormat.MPEG_4)
            setAudioEncoder(AudioEncoder.AAC)
            setOutputFile(
                File(context.filesDir, "default_name.mp4").toString()
            )
            prepare()
            start()
            Toast.makeText(context, "録音を開始しました", Toast.LENGTH_SHORT).show()
            return true
        } catch (e: IllegalStateException) {
            Log.e("MediaRecorder", e.message.orEmpty())
            Toast.makeText(context, "エラーが発生しました", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Log.e("MediaRecorder", e.message.orEmpty())
            Toast.makeText(context, "エラーが発生しました", Toast.LENGTH_SHORT)
                .show()
        }
        return recordingState
    }

    fun stopRecording(recordingState: Boolean): Boolean {
        try {
            stop()
            Toast.makeText(context, "録音を終了しました", Toast.LENGTH_SHORT).show()
            return false
        } catch (e: IllegalStateException) {
            Log.e("MediaRecorder", e.message.orEmpty())
            Toast.makeText(context, "エラー発生のため、録音データは保存されませんでした。", Toast.LENGTH_SHORT).show()
            reset()
            return false
        } catch (e: RuntimeException) {
            Log.e("MediaRecorder", e.message.orEmpty())
            File(context.filesDir, "default_name.mp4").delete()
            reset()
            Toast.makeText(context, "エラー発生のため、録音データは保存されませんでした。", Toast.LENGTH_SHORT).show()
            return false
        }
    }
}

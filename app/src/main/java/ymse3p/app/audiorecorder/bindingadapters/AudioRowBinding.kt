package ymse3p.app.audiorecorder.bindingadapters

import android.widget.TextView
import androidx.databinding.BindingAdapter
import ymse3p.app.audiorecorder.data.database.entities.AudioEntity
import ymse3p.app.audiorecorder.util.Constants
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

class AudioRowBinding {

    companion object {
        @BindingAdapter("setAudioDuration")
        @JvmStatic
        fun setAudioDuration(textView: TextView, audioEntity: AudioEntity) {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(audioEntity.audioDuration.toLong())
            val seconds = TimeUnit.MILLISECONDS.toSeconds(audioEntity.audioDuration.toLong())
            val durationText = "${minutes.toString()}分${seconds.toString()}秒"
            textView.text = durationText
        }

        @BindingAdapter("setAudioCreatedDate")
        @JvmStatic
        fun setAudioCreatedDate(textView: TextView, audioEntity: AudioEntity) {
            val dataFormat = SimpleDateFormat("yyyy年MM月dd日 HH時mm分")
            val createdDate = audioEntity.audioCreateDate.time
            textView.text = dataFormat.format(createdDate)
        }
    }
}

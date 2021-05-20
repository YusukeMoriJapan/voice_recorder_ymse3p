package ymse3p.app.audiorecorder.bindingadapters

import android.widget.TextView
import androidx.databinding.BindingAdapter
import ymse3p.app.audiorecorder.data.database.entities.AudioEntity
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

class AudioRowBinding {

    companion object {

        @BindingAdapter("setAudioTitle")
        @JvmStatic
        fun setAudioTitle(textView: TextView, audioEntity: AudioEntity) {
            val audioTitle = audioEntity.audioTitle
            if (audioTitle == "") textView.text = "タイトルなし"
            else textView.text = audioTitle
        }

        @BindingAdapter("setAudioDuration")
        @JvmStatic
        fun setAudioDuration(textView: TextView, audioEntity: AudioEntity) {
            val minutes =
                TimeUnit.MILLISECONDS.toMinutes(audioEntity.audioDuration.toLong()).toString()
            var seconds =
                (TimeUnit.MILLISECONDS.toSeconds(audioEntity.audioDuration.toLong()) % 60).toString()

            if (seconds.length == 1) seconds = "0$seconds"
            val durationText = "${minutes}分${seconds}秒"
            textView.text = durationText
        }

        @BindingAdapter("setAudioCreatedDate")
        @JvmStatic
        fun setAudioCreatedDate(textView: TextView, audioEntity: AudioEntity) {
            val dataFormat = SimpleDateFormat("yyyy年MM月dd日 HH時mm分")
            val createdDate = audioEntity.audioCreateDate.time
            textView.text = dataFormat.format(createdDate)
        }

        @BindingAdapter("setAddress")
        @JvmStatic
        fun setAddress(textView: TextView, address: String?) {
            /**住所例：日本、〒150-0043 東京都渋谷区道玄坂１丁目６−１０
             * 国名、郵便番号は表示する必要なし
             * */
            val splintedAddress = address?.split(" ") ?: run {
                textView.text = "位置情報は保存されていません"
                return
            }
            try {
                val stringBuilder = StringBuilder()

                splintedAddress.forEachIndexed { index, s ->
                    if (index == 0) return@forEachIndexed
                    stringBuilder.append(s)
                }

                textView.text = stringBuilder.toString()
            } catch (e: IndexOutOfBoundsException) {
                textView.text = "位置情報は保存されていません"
                return
            }

        }
    }
}

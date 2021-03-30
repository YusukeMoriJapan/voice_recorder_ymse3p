package ymse3p.app.audiorecorder.data.database

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.room.TypeConverter
import ymse3p.app.audiorecorder.util.Constants.Companion.DATABASE_DATE_FORMAT
import java.text.SimpleDateFormat
import java.util.*

class AudioTypeConverter {

    @TypeConverter
    fun calendarToString(calendar: Calendar): String {
        // SimpleDataFormatクラスはスレッドセーフではないためクラス変数としての定義は不可
        val dataFormat = SimpleDateFormat(DATABASE_DATE_FORMAT)
        return dataFormat.format(calendar.time)
    }

    @TypeConverter
    fun stringToCalendar(calendarString: String): Calendar {
        // SimpleDataFormatクラスはスレッドセーフではないためクラス変数としての定義は不可
        val dataFormat = SimpleDateFormat(DATABASE_DATE_FORMAT)
        val calendar = Calendar.getInstance()
        val date = dataFormat.parse(calendarString)
        if (date == null) {
            calendar.time = Date(0)
            Log.d("AudioTypeConverter", "can't convert String to Date.")
        } else {
            calendar.time = date
        }
        return calendar
    }

    @TypeConverter
    fun uriToString(uri: Uri): String {
        return uri.toString()
    }

    @TypeConverter
    fun stringToUri(uriString: String): Uri {
        return uriString.toUri()
    }
}
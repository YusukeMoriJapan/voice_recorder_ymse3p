package ymse3p.app.audiorecorder.data.database

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.room.TypeConverter
import ymse3p.app.audiorecorder.util.Constants.Companion.DATE_FORMAT
import java.text.SimpleDateFormat
import java.util.*

class AudioTypeConverter {

    private val dataFormat = SimpleDateFormat(DATE_FORMAT)

    @TypeConverter
    fun calendarToString(calendar: Calendar): String {
        return dataFormat.format(calendar)
    }

    @TypeConverter
    fun stringToCalendar(calendarString: String): Calendar {
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
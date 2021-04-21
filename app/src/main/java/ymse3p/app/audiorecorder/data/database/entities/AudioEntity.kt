package ymse3p.app.audiorecorder.data.database.entities

import android.net.Uri
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize
import ymse3p.app.audiorecorder.models.GpsData
import ymse3p.app.audiorecorder.util.Constants.Companion.AUDIO_TABLE
import java.util.*

@Parcelize
@Entity(tableName = AUDIO_TABLE)
class AudioEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val audioUri: Uri,
    val audioCreateDate: Calendar,
    val audioTitle: String,
    val audioDuration: Int,
    val gpsDataList: List<GpsData>?
):Parcelable {
    companion object {
        fun createAudioEntity(
            audioUri: Uri,
            audioCreateDate: Calendar,
            audioTitle: String,
            audioDuration: Int,
            gpsDataList: List<GpsData>?
        ): AudioEntity {
            return AudioEntity(0, audioUri, audioCreateDate, audioTitle, audioDuration, gpsDataList)
        }
    }
}
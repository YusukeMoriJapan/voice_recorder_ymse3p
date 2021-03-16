package ymse3p.app.audiorecorder.data.database.entities

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import ymse3p.app.audiorecorder.util.Constants.Companion.AUDIO_TABLE
import java.util.*

@Entity(tableName = AUDIO_TABLE)
class AudioEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val audioUri: Uri,
    val audioCreateDate: Calendar,
    val audioTitle: String,
    val audioDuration: Int
) {
    companion object {
        fun createAudioEntity(
            audioUri: Uri,
            audioCreateDate: Calendar,
            audioTitle: String,
            audioDuration: Int
        ): AudioEntity {
            return AudioEntity(0, audioUri, audioCreateDate, audioTitle, audioDuration)
        }
    }
}
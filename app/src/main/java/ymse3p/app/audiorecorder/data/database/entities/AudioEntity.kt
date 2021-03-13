package ymse3p.app.audiorecorder.data.database.entities

import android.net.Uri
import android.text.InputFilter
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
    val audioLength: Int
) {


}
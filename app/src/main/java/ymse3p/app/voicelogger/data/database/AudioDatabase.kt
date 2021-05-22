package ymse3p.app.voicelogger.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ymse3p.app.voicelogger.data.database.entities.AudioEntity


@Database(
    entities = [AudioEntity::class],
    version = 1,
)

@TypeConverters(AudioTypeConverter::class)
abstract class AudioDatabase: RoomDatabase() {

    abstract fun audioDao(): AudioDao

}
package ymse3p.app.audiorecorder.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ymse3p.app.audiorecorder.data.database.entities.AudioEntity


@Database(
    entities = [AudioEntity::class],
    version = 1,
    exportSchema = false
)

@TypeConverters(AudioTypeConverter::class)
abstract class AudioDatabase: RoomDatabase() {

    abstract fun audioDao(): AudioDao

}
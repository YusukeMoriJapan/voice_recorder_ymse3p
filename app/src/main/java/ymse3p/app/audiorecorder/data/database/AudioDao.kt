package ymse3p.app.audiorecorder.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ymse3p.app.audiorecorder.data.database.entities.AudioEntity

@Dao
interface AudioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudio(audioEntity: AudioEntity)

    @Query("SELECT * FROM audio_table ORDER BY id ASC")
    fun readAudio(): Flow<List<AudioEntity>>

    @Query("SELECT * FROM audio_table WHERE id = :id")
    fun readAudioFromId(id: Int): Flow<AudioEntity>

    @Delete
    suspend fun deleteAudio(audioEntity: AudioEntity)

    @Query("DELETE FROM audio_table")
    fun deleteAllAudio()
}
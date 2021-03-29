package ymse3p.app.audiorecorder.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ymse3p.app.audiorecorder.data.database.entities.AudioEntity

@Dao
interface AudioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudio(audioEntity: AudioEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudioList(audioEntityList: List<AudioEntity>)

    @Query("SELECT * FROM audio_table  ORDER BY id ASC")
    fun readAudio(): Flow<List<AudioEntity>>

    @Query("SELECT * FROM audio_table WHERE id = :id")
    fun readAudioFromId(id: Int): Flow<AudioEntity>

    @Delete
    suspend fun deleteAudio(audioEntity: AudioEntity)

    @Query("DELETE FROM audio_table WHERE audioUri = 'android.resource://ymse3p.app.audiorecorder/2131886080' ")
    suspend fun deleteAllSampleAudio()

    @Query("DELETE FROM audio_table")
    suspend fun deleteAllAudio()
}
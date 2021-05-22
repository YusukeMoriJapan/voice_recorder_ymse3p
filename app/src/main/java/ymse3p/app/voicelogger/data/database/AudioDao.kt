package ymse3p.app.voicelogger.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ymse3p.app.voicelogger.data.database.entities.AudioEntity

@Dao
interface AudioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudio(audioEntity: AudioEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudioList(audioEntityList: List<AudioEntity>)

//    @Query("SELECT * FROM audio_table ORDER BY id DESC")
//    fun readAudio(): Flow<List<AudioEntity>>

    @Query("SELECT * FROM audio_table WHERE startAddress LIKE :address OR endAddress LIKE :address OR audioTitle LIKE :title ORDER BY id DESC")
    fun searchAudio(address: String? = null, title: String? = null): Flow<List<AudioEntity>>

    @Query("SELECT * FROM audio_table WHERE id = :id")
    fun readAudioFromId(id: Int): Flow<AudioEntity>

    @Delete
    suspend fun deleteAudio(audioEntity: AudioEntity)

    @Query("DELETE FROM audio_table WHERE audioUri = 'android.resource://ymse3p.app.voicelogger/2131886080' ")
    suspend fun deleteAllSampleAudio()

    @Query("DELETE FROM audio_table")
    suspend fun deleteAllAudio()
}
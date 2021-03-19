package ymse3p.app.audiorecorder.data

import kotlinx.coroutines.flow.Flow
import ymse3p.app.audiorecorder.data.database.AudioDao
import ymse3p.app.audiorecorder.data.database.entities.AudioEntity
import javax.inject.Inject

class LocalDataSource @Inject constructor(
    private val audioDao: AudioDao
) {
    fun readAudio(): Flow<List<AudioEntity>> {
        return audioDao.readAudio()
    }

    fun readAudioFromId(id: Int): Flow<AudioEntity> {
        return audioDao.readAudioFromId(id)
    }

    suspend fun insertAudio(audioEntity: AudioEntity) {
        return audioDao.insertAudio(audioEntity)
    }

    suspend fun deleteAudio(audioEntity: AudioEntity) {
        return audioDao.deleteAudio(audioEntity)
    }

    fun deleteAllAudio() {
        return audioDao.deleteAllAudio()
    }

}
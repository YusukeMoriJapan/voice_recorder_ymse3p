package ymse3p.app.audiorecorder.data
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Repository @Inject constructor(
    val localDataSource: LocalDataSource
) {
}
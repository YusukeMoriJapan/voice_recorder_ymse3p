package ymse3p.app.voicelogger.data
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Repository @Inject constructor(
    val remoteDataSource: RemoteDataSource,
    val localDataSource: LocalDataSource
) {
}
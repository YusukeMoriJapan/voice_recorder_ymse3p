package ymse3p.app.audiorecorder.data

import dagger.hilt.android.scopes.ServiceScoped
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Repository @Inject constructor(
    val localDataSource: LocalDataSource
) {
}
package ymse3p.app.audiorecorder.data

import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class Repository @Inject constructor(
    val localDataSource: LocalDataSource
) {
}
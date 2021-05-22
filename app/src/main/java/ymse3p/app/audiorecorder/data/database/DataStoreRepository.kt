package ymse3p.app.audiorecorder.data.database

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.*
import ymse3p.app.audiorecorder.util.Constants.Companion.IS_FIRST_LAUNCH
import ymse3p.app.audiorecorder.util.Constants.Companion.PREFERENCES_NAME
import ymse3p.app.audiorecorder.util.Constants.Companion.RECORDED_AUDIO_ID
import java.io.IOException
import javax.inject.Inject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCES_NAME)

@ActivityRetainedScoped
class DataStoreRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private object PreferenceKeys {
        val recordedAudioId: Preferences.Key<Int> = intPreferencesKey(RECORDED_AUDIO_ID)
        val isFirstLaunch: Preferences.Key<Boolean> = booleanPreferencesKey(IS_FIRST_LAUNCH)
    }

    suspend fun incrementRecordedAudioId() {
        context.dataStore.edit { preferences ->
            val currentNumber = preferences[PreferenceKeys.recordedAudioId]?.plus(1) ?: 0
            preferences[PreferenceKeys.recordedAudioId] = currentNumber + 1
        }
    }

    val readRecordedAudioId: Flow<Int> = context.dataStore.data
//        .catch { exception ->
//            if (exception is IOException) {
//                emit(emptyPreferences())
//            } else {
//                throw exception
//            }
//        }
        .map { preferences ->
            preferences[PreferenceKeys.recordedAudioId] ?: 0
        }

    suspend fun setIsFirstLaunch(isFirstLaunch: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.isFirstLaunch] = isFirstLaunch
        }
    }

    val isFirstLaunch: Flow<Boolean?> = context.dataStore.data
//        .catch { exception ->
//            if (exception is IOException) {
//                emit(emptyPreferences())
//            } else {
//                throw exception
//            }
//        }
        .map { preferences ->
            preferences[PreferenceKeys.isFirstLaunch]
        }
}
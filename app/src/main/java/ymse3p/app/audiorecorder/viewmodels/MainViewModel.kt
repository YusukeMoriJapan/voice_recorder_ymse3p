package ymse3p.app.audiorecorder.viewmodels

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.*
import com.google.android.gms.location.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ymse3p.app.audiorecorder.R
import ymse3p.app.audiorecorder.data.Repository
import ymse3p.app.audiorecorder.data.database.DataStoreRepository
import ymse3p.app.audiorecorder.data.database.entities.AudioEntity
import ymse3p.app.audiorecorder.models.GpsData
import ymse3p.app.audiorecorder.util.CannotCollectGpsLocationException
import ymse3p.app.audiorecorder.util.CannotSaveAudioException
import ymse3p.app.audiorecorder.util.CannotStartRecordingException
import java.io.File
import java.io.IOException
import java.lang.IllegalStateException
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: Repository,
    application: Application,
    private val audioRecorder: MediaRecorder,
    private val dataStoreRepository: DataStoreRepository
) : AndroidViewModel(application) {

    /** ROOM DATABASE */
    val readAudio: LiveData<List<AudioEntity>> =
        repository.localDataSource.readAudio().asLiveData()
    private val readRecordedAudioId = dataStoreRepository.readRecordedAudioId


    /** 実行状態の保持 */
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording
    private var currentOutputFileName: File? = null
    private var currentAudioCreatedDate: Calendar? = null


    /** 録音処理(Media Recorderに対する処理) */
    suspend fun startRecording() {
        try {
            audioRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                this@MainViewModel.readRecordedAudioId.first { audioId ->
                    currentOutputFileName = File(
                        getApplication<Application>().filesDir,
                        audioId.toString(16) + "_recorded_audio.mp4"
                    )
                    setOutputFile(currentOutputFileName.toString())
                    Log.d("AudioNumber", audioId.toString(16))
                    true
                }
                withContext(Dispatchers.IO) {
                    prepare()
                }
                start()
            }
            _isRecording.value = true
            currentAudioCreatedDate = Calendar.getInstance()
            dataStoreRepository.incrementRecordedAudioId()
        } catch (e: IllegalStateException) {
            Log.e("MediaRecorder", e.message.orEmpty() + "/n" + e.stackTraceToString())
            throw CannotStartRecordingException("IllegalStateException occurred")

        } catch (e: IOException) {
            Log.e("MediaRecorder", e.message.orEmpty() + "/n" + e.stackTraceToString())
            throw CannotStartRecordingException("IOExceptionException occurred")
        }
    }

    fun stopRecording() {
        audioRecorder.apply {
            try {
                stop()
                _isRecording.value = false
            } catch (e: IllegalStateException) {
                Log.e("MediaRecorder", e.message.orEmpty())
                reset()
                _isRecording.value = false
                throw CannotSaveAudioException("IllegalStateException occurred")
            } catch (e: RuntimeException) {
                Log.e("MediaRecorder", e.message.orEmpty() + "/n" + e.stackTraceToString())
                reset()
                _isRecording.value = false
                throw CannotSaveAudioException("RuntimeException occurred")
            } finally {
                stopLocationUpdates()
            }
        }
    }


    /** ROOM DATABASEに対する処理 */
    fun insertAudio(
        audioTitle: String,
    ) = viewModelScope.launch(Dispatchers.IO) {
        val audioDuration = try {
            MediaMetadataRetriever().run {
                setDataSource(currentOutputFileName?.path)
                extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            }?.toInt() ?: 0
        } catch (e: NumberFormatException) {
            Log.e("MediaMetadataRetriever", e.message.orEmpty() + "/n" + e.stackTraceToString())
        }

        val audioEntity =
            AudioEntity.createAudioEntity(
                Uri.fromFile(currentOutputFileName),
                currentAudioCreatedDate ?: Calendar.getInstance(),
                audioTitle,
                audioDuration,
                savedGpsDataList
            )
        repository.localDataSource.insertAudio(audioEntity)
        currentOutputFileName = null
    }

    fun deleteAudio(audioEntity: AudioEntity) =
        viewModelScope.launch(Dispatchers.IO) {
            repository.localDataSource.deleteAudio(audioEntity)
        }

    fun deleteAllAudio() =
        viewModelScope.launch(Dispatchers.IO) {
            repository.localDataSource.deleteAllAudio()
        }


    /** サンプルデータに対する操作　*/
    fun insertSampleAudio() {
        val sampleUri =
            Uri.parse("android.resource://${this.getApplication<Application>().packageName}/" + R.raw.famipop3)
        val audioCreateDate = Calendar.getInstance()
        val audioDuration = try {
            MediaMetadataRetriever().run {
                setDataSource(getApplication(), sampleUri)
                extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            }?.toInt() ?: 0
        } catch (e: NumberFormatException) {
            Log.e(
                "MediaMetadataRetriever",
                e.message.orEmpty() + "/n" + e.stackTraceToString()
            )
        }
        val audioList = List(10) {
            AudioEntity.createAudioEntity(
                sampleUri,
                audioCreateDate,
                "録音データ${it}",
                audioDuration,
                null
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.localDataSource.insertAudioList(audioList)
        }
    }

    fun deleteAllSampleAudio() {
        viewModelScope.launch(Dispatchers.IO) { repository.localDataSource.deleteAllSampleAudio() }
    }


    /** ViewModelの状態遷移に紐づく処理 */
    override fun onCleared() {
        super.onCleared()
        audioRecorder.release()
    }


    /** Location */
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val savedLocationList = mutableListOf<Location>()
    private val savedGpsDataList = mutableListOf<GpsData>()

    private val locationCallback = object : LocationCallback() {
        val lastLocationFlow =
            MutableSharedFlow<Location>(20, onBufferOverflow = BufferOverflow.SUSPEND)

        val jobAddCollection by lazy {
            lastLocationFlow
                .onEach {
                    withContext(Dispatchers.IO) { savedLocationList.add(it) }
                }
                .launchIn(viewModelScope)
        }

        override fun onLocationResult(locationResult: LocationResult) {
            /** LocationをsavedLocationListに格納するコルーチンを起動 */
            jobAddCollection
            lastLocationFlow.tryEmit(locationResult.lastLocation)
        }
    }

    fun startLocationUpdates() {
        savedLocationList.clear()
        savedGpsDataList.clear()
        val locationRequest = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 1000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val isFineLocationGranted = ActivityCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val isCoarseLocationGranted = ActivityCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!isFineLocationGranted && !isCoarseLocationGranted) {
            throw CannotCollectGpsLocationException("FineLocation and CoarseLocation are not granted.")
        }
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(getApplication() as Context)
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun stopLocationUpdates() {
        savedLocationList.forEach { location ->
            val gpsData = location.run {
                GpsData(latitude, longitude, altitude, bearing, speed, time)
            }
            savedGpsDataList.add(gpsData)
        }
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

}
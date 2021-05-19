package ymse3p.app.audiorecorder.viewmodels

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.*
import com.google.android.gms.location.*
import com.google.gson.GsonBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import retrofit2.Response
import ymse3p.app.audiorecorder.R
import ymse3p.app.audiorecorder.data.Repository
import ymse3p.app.audiorecorder.data.database.DataStoreRepository
import ymse3p.app.audiorecorder.data.database.entities.AudioEntity
import ymse3p.app.audiorecorder.models.GpsData
import ymse3p.app.audiorecorder.models.RoadsApiResponse
import ymse3p.app.audiorecorder.util.*
import ymse3p.app.audiorecorder.util.Constants.Companion.PAGE_SIZE_LIMIT
import ymse3p.app.audiorecorder.util.Constants.Companion.PAGINATION_OVERLAP
import ymse3p.app.audiorecorder.util.Constants.Companion.REQUEST_MIN_INTERVAL_MS
import java.io.*
import java.lang.IllegalStateException
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val repository: Repository,
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
    val filteringMode = MutableStateFlow(FilteringMode.ADDRESS)

    private val _isInserting = MutableSharedFlow<Boolean>()
    val isInserting: SharedFlow<Boolean> = _isInserting


    /** 録音処理(Media Recorderに対する処理) */
    suspend fun startRecording() {
        try {
            audioRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC)
                setAudioEncodingBitRate(1152000)
                setAudioSamplingRate(48000)
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
        _isInserting.emit(true)
        val audioDuration = try {
            MediaMetadataRetriever().run {
                setDataSource(currentOutputFileName?.path)
                extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            }?.toInt() ?: 0
        } catch (e: NumberFormatException) {
            Log.e("MediaMetadataRetriever", e.message.orEmpty() + "/n" + e.stackTraceToString())
        }

        val startAddress: String = getStartAddress(savedLocationList) ?: ""
        val endAddress: String = getEndAddress(savedLocationList) ?: ""

        isSavingGpsList.first { isLoading ->
            if (isLoading) return@first false

            val audioEntity =
                AudioEntity.createAudioEntity(
                    Uri.fromFile(currentOutputFileName),
                    currentAudioCreatedDate ?: Calendar.getInstance(),
                    audioTitle,
                    audioDuration,
                    savedGpsDataList,
                    startAddress,
                    endAddress
                )
            repository.localDataSource.insertAudio(audioEntity)
            currentOutputFileName = null
            return@first true
        }
        _isInserting.emit(false)
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
        viewModelScope.launch(Dispatchers.IO) {
            _isInserting.emit(true)
            val sampleUri =
                Uri.parse("android.resource://${this@MainViewModel.getApplication<Application>().packageName}/" + R.raw.sample_audio)
            val audioCreateDate = Calendar.getInstance().apply { time = Date(0) }
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

            val sampleJson = sampleJsonToGpsList()

            val audioList = List(10) {
                AudioEntity.createAudioEntity(
                    sampleUri,
                    audioCreateDate,
                    "録音データ${it}",
                    audioDuration,
                    sampleJson,
                    "日本、〒036-1343 青森県弘前市百沢東岩木山 津軽岩木スカイライン",
                    "日本、〒036-1343 青森県弘前市百沢東岩木山 津軽岩木スカイライン"
                )
            }
            repository.localDataSource.insertAudioList(audioList)
            _isInserting.emit(false)
        }


    }

    fun deleteAllSampleAudio() {
        viewModelScope.launch(Dispatchers.IO) { repository.localDataSource.deleteAllSampleAudio() }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.release()
    }

    /** Location */
    private fun getStartAddress(locationList: List<Location>): String? {
        val strAddress = StringBuffer()
        val gCoder = Geocoder(getApplication(), Locale.getDefault())

        val startLoc: Location = locationList.firstOrNull() ?: return null

        return try {
            val startAddressList =
                gCoder.getFromLocation(startLoc.latitude, startLoc.longitude, 1)

            addressToString(startAddressList, strAddress)
        } catch (e: IOException) {
            Log.e("Geocoder", e.stackTrace.toString())
            null
        }
    }

    private fun getEndAddress(locationList: List<Location>): String? {
        val strAddress = StringBuffer()
        val gCoder = Geocoder(getApplication(), Locale.getDefault())

        val endLoc: Location = locationList.lastOrNull() ?: return null

        return try {
            val endAddressList =
                gCoder.getFromLocation(endLoc.latitude, endLoc.longitude, 1)

            addressToString(endAddressList, strAddress)
        } catch (e: IOException) {
            Log.e("Geocoder", e.stackTrace.toString())
            null
        }
    }

    private fun addressToString(
        addressList: List<Address>,
        strAddress: StringBuffer
    ): String? {
        val address: Address = addressList.firstOrNull() ?: return null

        val maxLineIndex = address.maxAddressLineIndex
        for (i in 0..maxLineIndex) {
            strAddress.append(address.getAddressLine(i))
        }
        return strAddress.toString()
    }

    /** Pathway */
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val savedLocationList = mutableListOf<Location>()
    private var savedGpsDataList = mutableListOf<GpsData>()
    private val isSavingGpsList = MutableStateFlow(false)

    private val locationCallback = object : LocationCallback() {
        val lastLocationFlow =
            MutableSharedFlow<Location>(20, onBufferOverflow = BufferOverflow.SUSPEND)

        val jobAddCollection by lazy {
            var counter = 0
            lastLocationFlow
                .onEach {
                    withContext(Dispatchers.IO) {
                        savedLocationList.add(it)
                        counter += 1
                        Log.d("counter", counter.toString())
                    }
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
            interval = 1000
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
        viewModelScope.launch(Dispatchers.IO) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            isSavingGpsList.value = true
            if (hasInternetConnection(getApplication()))
                saveSnappedGpsDataList(getSnappedPoints())
            else
                saveOriginalGpsDataList()

            isSavingGpsList.value = false
        }
    }

    private fun saveSnappedGpsDataList(snappedGpsDataList: MutableList<GpsData>) {
        savedGpsDataList = snappedGpsDataList
    }

    private fun saveOriginalGpsDataList() {
        savedLocationList.forEachIndexed { index, location ->
            val gpsData =
                location.run {
                    GpsData(
                        latitude,
                        longitude,
                        altitude,
                        bearing,
                        speed,
                        time,
                        index
                    )
                }
            savedGpsDataList.add(gpsData)
        }
    }


    private suspend fun getSnappedPoints(): MutableList<GpsData> {
        val dividedOriginalLocList: List<List<Location>> =
            savedLocationList.divideList(PAGINATION_OVERLAP, PAGE_SIZE_LIMIT)

        val dividedSnappedGpsList: List<List<GpsData>?> =
            dividedOriginalLocList
                .map { locationList ->
                    val deferred = getSnappedPointsAsync(locationList)
                    delay(REQUEST_MIN_INTERVAL_MS * 2)
                    return@map deferred
                }
                .map { deferred -> deferred.await() }
                .map { retrofitResponse -> responseToNetworkResult(retrofitResponse) }
                .mapIndexed { index, networkResult ->
                    netWorkResultToGpsList(
                        networkResult,
                        dividedOriginalLocList[index]
                    )
                }

        val combinedGpsList: List<List<GpsData>> =
            dividedSnappedGpsList.reduceOverlappedPoints(dividedOriginalLocList)

        return combinedGpsList.reAllocateOneByOne().toMutableList()
    }

    private suspend fun getSnappedPointsAsync(locationList: List<Location>): Deferred<Response<RoadsApiResponse>> =
        coroutineScope {
            async(Dispatchers.IO) {
                val pathQuery: String = generatePathQuery(locationList)
                repository.remoteDataSource.getSnappedPoints(pathQuery)
            }
        }

    private fun generatePathQuery(page: List<Location>): String {
        val query = StringBuilder()

        val listSize = page.size
        page.forEachIndexed { index, location ->
            val latitude = location.latitude
            val longitude = location.longitude

            if (index == listSize - 1)
                query.append("$latitude,$longitude")
            else
                query.append("$latitude,$longitude|")
        }

        return query.toString()
    }

    private fun responseToNetworkResult(response: Response<RoadsApiResponse>): NetworkResult<RoadsApiResponse> {
        when {
            response.message().toString().contains("timeout") -> {
                return NetworkResult.Error("Timeout")
            }
            response.code() == 402 -> {
                return NetworkResult.Error("API Key Limited.")
            }
            response.body() == null || response.body()?.snappedPoints.isNullOrEmpty() -> {
                Log.e("Roads API Call", response.errorBody()?.string().orEmpty())
                return NetworkResult.Error("Points not found.")
            }
            response.isSuccessful -> {
                val snappedPoints = response.body()
                    ?: return NetworkResult.Error("Points not found.")

                return NetworkResult.Success(snappedPoints)
            }
            else -> {
                return NetworkResult.Error(response.message())
            }

        }
    }

    private fun netWorkResultToGpsList(
        networkResult: NetworkResult<RoadsApiResponse>, originalLocList: List<Location>
    ): MutableList<GpsData>? {
        when (networkResult) {
            is NetworkResult.Success -> {
                return if (networkResult.data == null) null
                else responseToGpsDataList(networkResult.data, originalLocList)
            }

            is NetworkResult.Error -> {
                Log.e("Roads API Call", networkResult.message.orEmpty())
                return null

            }

            // 修正要　読み込み中状態を返すロジックは現状なし
            is NetworkResult.Loading -> {
                Log.d("Roads API Call", "network loading...")
                return null
            }
        }
    }

    private fun responseToGpsDataList(
        responseBody: RoadsApiResponse, originalLocList: List<Location>
    ): MutableList<GpsData> {
        val gpsDataList = mutableListOf<GpsData>()

        responseBody.snappedPoints.forEach { snappedPoint ->
            val snappedLat = snappedPoint.location.latitude
            val snappedLng = snappedPoint.location.longitude
            val originalIndex: Int? = snappedPoint.originalIndex

            if (originalIndex !== null) {
                val originalAlt = originalLocList[originalIndex].altitude
                val originalBear = originalLocList[originalIndex].bearing
                val originalSpeed = originalLocList[originalIndex].speed
                val originalTime = originalLocList[originalIndex].time
                gpsDataList.add(
                    GpsData(
                        latitude = snappedLat,
                        longitude = snappedLng,
                        altitude = originalAlt,
                        bearing = originalBear,
                        speed = originalSpeed,
                        time = originalTime,
                        originalIndex = originalIndex
                    )
                )
            } else
                gpsDataList.add(GpsData(latitude = snappedLat, longitude = snappedLng))
        }

        return gpsDataList
    }


    private fun List<List<GpsData>?>.reduceOverlappedPoints(originalList: List<List<Location>>): List<List<GpsData>> =
        this.mapIndexed { index, gpsList ->
            gpsList
                ?: run { return@mapIndexed originalList[index].locationListToGpsList() }

            if (index == 0) return@mapIndexed gpsList

            val reducedGpsList: MutableList<GpsData> = mutableListOf()
            var passedOverlap = false
            gpsList.forEach { gpsData ->
                val safeOriginalIndex = gpsData.originalIndex
                    ?: run {
                        if (passedOverlap) reducedGpsList.add(gpsData)
                        return@forEach
                    }

                if (safeOriginalIndex >= PAGINATION_OVERLAP - 1) passedOverlap = true
                if (passedOverlap) reducedGpsList.add(gpsData)
            }
            return@mapIndexed reducedGpsList
        }

    private fun sampleJsonToGpsList(): List<GpsData> {
        val sampleJson = getSampleJson(getApplication())

        val gSon = GsonBuilder().serializeNulls().create()
        val sampleResponse: RoadsApiResponse =
            gSon.fromJson(sampleJson, RoadsApiResponse::class.java)

        val gpsDataList = mutableListOf<GpsData>()

        var sampleTimeMs: Long = 0
        sampleResponse.snappedPoints.forEach { snappedPoint ->
            val snappedLat = snappedPoint.location.latitude
            val snappedLng = snappedPoint.location.longitude
            val originalIndex: Int? = snappedPoint.originalIndex

            if (originalIndex !== null) {
                gpsDataList.add(
                    GpsData(
                        latitude = snappedLat,
                        longitude = snappedLng,
                        time = sampleTimeMs,
                        originalIndex = originalIndex
                    )
                )
                sampleTimeMs += 1000
            } else
                gpsDataList.add(GpsData(latitude = snappedLat, longitude = snappedLng))
        }

        return gpsDataList
    }
}

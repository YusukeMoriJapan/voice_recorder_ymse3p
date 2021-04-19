package ymse3p.app.audiorecorder

import androidx.fragment.app.Fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import retrofit2.Response
import ymse3p.app.audiorecorder.data.network.RoadsApi
import ymse3p.app.audiorecorder.databinding.FragmentMapsBinding
import ymse3p.app.audiorecorder.models.GpsData
import ymse3p.app.audiorecorder.models.RoadsApiResponse
import ymse3p.app.audiorecorder.util.NetworkResult
import javax.inject.Inject

@AndroidEntryPoint
class MapsFragment : Fragment() {

    private lateinit var _binding: FragmentMapsBinding
    private val binding get() = _binding

    @Inject
    lateinit var roadsApi: RoadsApi

    private lateinit var mGoogleMap: GoogleMap

    private val isMapReady = MutableStateFlow(false)

    private val callback = OnMapReadyCallback { googleMap ->
        val sydney = LatLng(-34.0, 151.0)
        googleMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 15f))
        mGoogleMap = googleMap
        isMapReady.value = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(callback)

        /** Roads APIから補正データを取得 */
        lifecycleScope.launchWhenCreated {
            val retrofitResponse = roadsApi.getSnappedPoints(generatePathQuery())
            val networkResult: NetworkResult<RoadsApiResponse> =
                handleRetrofitResponse(retrofitResponse)

            when (networkResult) {
                is NetworkResult.Success -> {
                    val responseBodyNullSafe: RoadsApiResponse =
                        networkResult.data ?: return@launchWhenCreated

                    val gpsDataList = responseToGpsDataList(responseBodyNullSafe)
                    drawPolyLine(gpsDataList)
                }

                is NetworkResult.Error -> {
                    /** 後日実装 */
                }
                is NetworkResult.Loading -> {
                    /** 後日実装 */
                }
            }
        }
    }

    private fun drawPolyLine(gpsDataList: List<GpsData>) {
        lifecycleScope.launchWhenCreated {
            val latLngList = gpsDataToLatLng(gpsDataList)
            isMapReady.first { isReady ->
                if (isReady) {
                    mGoogleMap.addPolyline(PolylineOptions().addAll(latLngList))
                    mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngList[0], 15f))
                    true
                } else false
            }
        }
    }

    private fun gpsDataToLatLng(gpsDataList: List<GpsData>): List<LatLng> {
        val latLngList = mutableListOf<LatLng>()
        gpsDataList.forEach { gpsData ->
            latLngList.add(LatLng(gpsData.latitude, gpsData.longitude))
        }
        return latLngList
    }

    private fun responseToGpsDataList(responseBody: RoadsApiResponse): List<GpsData> {
        val gpsDataList = mutableListOf<GpsData>()

        responseBody.snappedPoints.forEach { snappedPoint ->
            val lat = snappedPoint.location.latitude
            val lng = snappedPoint.location.longitude
            gpsDataList.add(GpsData(latitude = lat, longitude = lng))
        }

        return gpsDataList
    }


    private fun handleRetrofitResponse(response: Response<RoadsApiResponse>): NetworkResult<RoadsApiResponse> {
        when {
            response.message().toString().contains("timeout") -> {
                return NetworkResult.Error("Timeout")
            }
            response.code() == 402 -> {
                return NetworkResult.Error("API Key Limited.")
            }
            response.body() == null || response.body()?.snappedPoints.isNullOrEmpty() -> {
                Log.e("Retrofit", response.errorBody()!!.string())
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

    private fun generatePathQuery(): String {
        val query = StringBuilder()
        val latitude = -35.2784167
        val longitude = 149.12958
        query.append("$latitude,$longitude|")
//        return query
        return "-35.27801,149.12958|-35.28032,149.12907|-35.28099,149.12929|-35.28144,149.12984|-35.28194,149.13003|-35.28282,149.12956|-35.28302,149.12881|-35.28473,149.12836"
    }
}
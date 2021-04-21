package ymse3p.app.audiorecorder.ui.fragments

import androidx.fragment.app.Fragment
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import ymse3p.app.audiorecorder.R
import ymse3p.app.audiorecorder.databinding.FragmentMapsBinding
import ymse3p.app.audiorecorder.models.GpsData
import ymse3p.app.audiorecorder.viewmodels.playbackViewModel.PlayBackViewModel
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs

@AndroidEntryPoint
class MapsFragment : Fragment() {

    private lateinit var _binding: FragmentMapsBinding
    private val binding get() = _binding

    private val playbackViewModel by activityViewModels<PlayBackViewModel>()

    private val args by navArgs<MapsFragmentArgs>()
    private lateinit var onViewCreatedJob: Job

    private var currentMarker: Marker? = null

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

        onViewCreatedJob = lifecycleScope.launchWhenCreated {
            val googleMap = mapFragment.getMapSuspend()
            args.audioEntity.gpsDataList?.run { drawPolyLine(googleMap, this) }

            playbackViewModel.playbackState.collect { playbackState ->
                val nearestGpsIndex = calcNearestGpsIndex(playbackState)
                val nearestLatLng: LatLng? = nearestGpsIndex?.let { calculateNearest(it) }
                currentMarker?.remove()
                if (nearestLatLng !== null) refreshMapState(googleMap, nearestLatLng)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::onViewCreatedJob.isInitialized)
            onViewCreatedJob.cancel()
    }

    private fun calcNearestGpsIndex(playbackState: PlaybackStateCompat?): Int? {
        var minTimeDiff: Long? = null
        var nearestGpsIndex: Int? = null
        args.audioEntity.gpsDataList?.forEachIndexed { index, gpsData ->
            if (gpsData.originalIndex == null) return@forEachIndexed

            val minDiffInternal = minTimeDiff
            val startTime: Long = args.audioEntity.audioCreateDate.time.time
            val currentTime = startTime + (playbackState?.position ?: 0)
            val gpsTime: Long = gpsData.time
            val timeDiff = abs(gpsTime - currentTime)

            if (minDiffInternal == null || timeDiff < minDiffInternal) {
                minTimeDiff = timeDiff
                nearestGpsIndex = index
            }
        }
        return nearestGpsIndex
    }

    private fun calculateNearest(nearestGpsIndex: Int): LatLng? {
        val nearestGpsData = args.audioEntity.gpsDataList?.get(nearestGpsIndex)
        return if (nearestGpsData == null)
            null
        else
            LatLng(nearestGpsData.latitude, nearestGpsData.longitude)
    }

    private fun refreshMapState(googleMap: GoogleMap, nearestLatLng: LatLng) {
        googleMap.apply {
            /** 最も近いGps PositionにMarkerをたてる */
            currentMarker =
                addMarker(MarkerOptions().position(nearestLatLng).title("現在地"))
            /** カメラをMarkerに移動する */
            moveCamera(CameraUpdateFactory.newLatLng(nearestLatLng))
        }
    }


    private fun drawPolyLine(googleMap: GoogleMap, gpsDataList: List<GpsData>) {
        val latLngList = gpsDataToLatLng(gpsDataList)
        googleMap.addPolyline(PolylineOptions().addAll(latLngList))
        googleMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                latLngList.firstOrNull() ?: LatLng(0.0, 0.0), 15f
            )
        )
    }

    private fun gpsDataToLatLng(gpsDataList: List<GpsData>): List<LatLng> {
        val latLngList = mutableListOf<LatLng>()
        gpsDataList.forEach { gpsData ->
            latLngList.add(LatLng(gpsData.latitude, gpsData.longitude))
        }
        return latLngList
    }

    private suspend fun SupportMapFragment.getMapSuspend(): GoogleMap =
        suspendCoroutine { getMapAsync { googleMap -> it.resume(googleMap) } }

}
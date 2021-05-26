package ymse3p.app.voicelogger.adapter

import android.app.Application
import android.content.pm.ActivityInfo
import android.media.session.PlaybackState
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import ymse3p.app.voicelogger.R
import ymse3p.app.voicelogger.data.database.entities.AudioEntity
import ymse3p.app.voicelogger.databinding.AudioRowLayoutBinding
import ymse3p.app.voicelogger.models.GpsData
import ymse3p.app.voicelogger.ui.fragments.dialogs.DeleteAlertDialogFragment
import ymse3p.app.voicelogger.util.AudioDiffUtil
import ymse3p.app.voicelogger.util.Constants.Companion.MEDIA_METADATA_QUEUE
import ymse3p.app.voicelogger.util.ResourceUtil
import ymse3p.app.voicelogger.viewmodels.MainViewModel
import ymse3p.app.voicelogger.viewmodels.playbackViewModel.PlayBackViewModel
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class AudioAdapter(
    private val mainViewModel: MainViewModel,
    private val playBackViewModel: PlayBackViewModel,
    private val requireActivity: FragmentActivity
) : RecyclerView.Adapter<AudioAdapter.MyViewHolder>(),
    ActionMode.Callback, CoroutineScope, DeleteAlertDialogFragment.DeleteAlertDialogListener {

    override val coroutineContext: CoroutineContext
        get() = playBackViewModel.viewModelScope.coroutineContext


    private var audio = emptyList<AudioEntity>()

    /** 生成されたViewHolderを保持 */
    val viewHolders = mutableListOf<MyViewHolder>()

    /** ユーザーがシングルクリックした音源データ */
    val selectedAudioEntity = MutableSharedFlow<AudioEntity>()

    /** Contextual Action Mode */
    private lateinit var mActionMode: ActionMode
    private var multiSelection = false
    private var selectedAudioList = arrayListOf<AudioEntity>()

    inner class MyViewHolder(
        val binding: AudioRowLayoutBinding,
        private val playBackViewModel: PlayBackViewModel,
        var currentPosition: Int? = null,
        private val fragmentCoroutineScope: LifecycleCoroutineScope
    ) : RecyclerView.ViewHolder(binding.root) {

        private lateinit var googleMapStart: GoogleMap
        private lateinit var googleMapEnd: GoogleMap

        init {
            fragmentCoroutineScope.launchWhenCreated { withContext(Dispatchers.Default) { getGoogleMaps() } }
        }

        private suspend fun getGoogleMaps() {
            withContext(Dispatchers.Main) {
                googleMapStart = binding.rowMapViewStart.getMapSuspend().apply {
                    uiSettings.isMapToolbarEnabled = false
                }
                googleMapEnd = binding.rowMapViewEnd.getMapSuspend().apply {
                    uiSettings.isMapToolbarEnabled = false
                }

                binding.startMapBackground.setOnClickListener { moveToMapFragment() }
                binding.endMapBackground.setOnClickListener { moveToMapFragment() }

                /** map取得直後に実行する処理
                 * 取得時点でバインドされているAudioEntityの位置データをもとに描画
                 * */
                binding.audioEntity?.gpsDataList?.run {
                    val latLngList = gpsDataToLatLng(this)
                    addStartMarker(googleMapStart, latLngList)
                    addEndMarker(googleMapEnd, latLngList)
                }

                binding.startMapBackground.alpha = 0F
                binding.endMapBackground.alpha = 0F
            }

        }

        private fun moveToMapFragment(): Job =
            fragmentCoroutineScope.launchWhenResumed {
                currentPosition?.let { playBackViewModel.skipToQueueItem(it.toLong()) }
                binding.audioEntity?.let { selectedAudioEntity.emit(it) }
                cancel()
            }


        fun bind(audioEntity: AudioEntity, position: Int) {
            currentPosition = position

            /** 再生ボタンのクリックリスナー設定　*/
            binding.playFloatButton.setOnClickListener {
                val state = playBackViewModel.playbackState.replayCache.firstOrNull()?.state
                val playingId = playBackViewModel.metadata.replayCache.firstOrNull()
                    ?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)?.toInt()

                if (state == PlaybackState.STATE_PLAYING && playingId == audioEntity.id) {
                    playBackViewModel.pause()
                } else {
                    currentPosition?.let {
                        playBackViewModel.skipToQueueItem(it.toLong())
                    }
                }
            }

            /** バインド時に、「現在再生中の曲」と「バインドされた音声ID」が一致しているかどうか確認
            　　一致している場合は、一時停止アイコンに切り替える */
            val state = playBackViewModel.playbackState.replayCache.firstOrNull()?.state
            if (state != PlaybackStateCompat.STATE_PLAYING) {
                binding.playFloatButton.setImageResource(R.drawable.ic_baseline_play_arrow_24)
            } else {
                val playingId = playBackViewModel.metadata.replayCache.firstOrNull()
                    ?.getLong(MEDIA_METADATA_QUEUE)
                if (currentPosition == playingId?.toInt())
                    binding.playFloatButton.setImageResource(R.drawable.ic_baseline_pause_24)
                else
                    binding.playFloatButton.setImageResource(R.drawable.ic_baseline_play_arrow_24)
            }

            binding.audioEntity = audioEntity
            binding.initializeMapView()
            binding.executePendingBindings()
        }

        private fun AudioRowLayoutBinding.initializeMapView() {
            if (!::googleMapStart.isInitialized || !::googleMapEnd.isInitialized) return
            /** googleMapの描画処理 */
            googleMapStart.clear()
            googleMapEnd.clear()

            binding.rowMapViewStart.visibility = View.INVISIBLE
            binding.rowMapViewEnd.visibility = View.INVISIBLE

            binding.audioEntity?.gpsDataList?.run {
                val latLngList = gpsDataToLatLng(this)
                addStartMarker(googleMapStart, latLngList)
                addEndMarker(googleMapEnd, latLngList)
            }

            binding.rowMapViewStart.visibility = View.VISIBLE
            binding.rowMapViewEnd.visibility = View.VISIBLE
        }

        private suspend fun MapView.getMapSuspend(): GoogleMap =
            suspendCoroutine { continuation ->
                getMapAsync { googleMap -> continuation.resume(googleMap) }
            }

        private fun addStartMarker(googleMap: GoogleMap, latLngList: List<LatLng>) {
            val startPoint = latLngList.firstOrNull() ?: return
            googleMap.addMarker(
                MarkerOptions().position(startPoint).anchor(0.5F, 0.5F)
                    .icon(
                        BitmapDescriptorFactory.fromBitmap(
                            ResourceUtil.getBitmap(
                                playBackViewModel.getApplication(),
                                R.drawable.ic_baseline_fiber_manual_record_24
                            )
                        )
                    )
            )

            googleMap.addPolyline(
                PolylineOptions()
                    .addAll(latLngList)
                    .color(
                        ContextCompat.getColor(
                            playBackViewModel.getApplication(),
                            R.color.darkGray
                        )
                    )
            )

            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPoint, 15F))
        }


        private fun addEndMarker(googleMap: GoogleMap, latLngList: List<LatLng>) {
            val endPoint = latLngList.lastOrNull() ?: return
            googleMap.addMarker(
                MarkerOptions().position(endPoint).anchor(0.5F, 0.5F)
                    .icon(
                        BitmapDescriptorFactory.fromBitmap(
                            ResourceUtil.getBitmap(
                                playBackViewModel.getApplication(),
                                R.drawable.ic_baseline_stop_24
                            )
                        )
                    )
            )

            googleMap.addPolyline(
                PolylineOptions()
                    .addAll(latLngList)
                    .color(
                        ContextCompat.getColor(
                            playBackViewModel.getApplication(),
                            R.color.darkGray
                        )
                    )
            )
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(endPoint, 15F))
        }


        private fun gpsDataToLatLng(gpsDataList: List<GpsData>): List<LatLng> {
            val latLngList = mutableListOf<LatLng>()
            gpsDataList.forEach { gpsData ->
                latLngList.add(LatLng(gpsData.latitude, gpsData.longitude))
            }
            return latLngList
        }
    }

    private fun createDataBinding(parent: ViewGroup): AudioRowLayoutBinding {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = AudioRowLayoutBinding.inflate(layoutInflater, parent, false)
        binding.rowMapViewStart.onCreate(null)
        binding.rowMapViewEnd.onCreate(null)

        return binding
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = createDataBinding(parent)
        val viewHolder =
            MyViewHolder(
                binding,
                playBackViewModel,
                fragmentCoroutineScope = requireActivity.lifecycleScope
            )
        viewHolders.add(viewHolder)
        return viewHolder
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentAudio = audio[position]
        holder.bind(currentAudio, position)

        saveItemStateOnScroll(currentAudio, holder)

        /** 現在地をLiteModeで表示させる場合は、onResumeとonPauseの呼び出しが必要 */
//        holder.binding.rowMapView.onResume()

        holder.binding.audioRowLayout.setOnClickListener {
            when {
                multiSelection ->
                    applySelection(holder, currentAudio)
            }
        }

        holder.binding.audioRowLayout.setOnLongClickListener {
            if (!multiSelection) {
                multiSelection = true
                requireActivity.startActionMode(this)
                applySelection(holder, currentAudio)
                true

            } else {
                applySelection(holder, currentAudio)
                true
            }
        }
    }

    override fun getItemCount(): Int {
        return audio.size
    }

    fun setData(newData: List<AudioEntity>) {
        val audioDiffUtil = AudioDiffUtil(audio, newData)
        val diffUtilResult = DiffUtil.calculateDiff(audioDiffUtil)
        audio = newData
        diffUtilResult.dispatchUpdatesTo(this)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        /** 再生音源が変更されたら、すべてのViewHolderに対してアイコン変更有無確認を実行する */
        launch {
            playBackViewModel.metadata.collect { metadata ->
                viewHolders.forEach { viewHolder -> changePlaybackIcon(metadata, viewHolder) }
            }
        }
        launch {
            playBackViewModel.playbackState.collect { playbackState ->
                viewHolders.forEach { viewHolder ->
                    changePlaybackIcon(
                        playbackState,
                        viewHolder
                    )
                }
            }
        }
    }

    private fun changePlaybackIcon(metadata: MediaMetadataCompat?, viewHolder: MyViewHolder) {
        val playingId = metadata?.getLong(MEDIA_METADATA_QUEUE)?.toInt()
        viewHolder.binding.run {
            if (viewHolder.currentPosition == playingId)
                playFloatButton.setImageResource(R.drawable.ic_baseline_pause_24)
            else
                playFloatButton.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        }
    }

    private fun changePlaybackIcon(
        playbackState: PlaybackStateCompat?,
        viewHolder: MyViewHolder
    ) {
        viewHolder.binding.run {
            if (playbackState?.state != PlaybackStateCompat.STATE_PLAYING) {
                playFloatButton.setImageResource(R.drawable.ic_baseline_play_arrow_24)
            } else {
                val playingId =
                    playBackViewModel.metadata.replayCache.firstOrNull()
                        ?.getLong(MEDIA_METADATA_QUEUE)
                if (viewHolder.currentPosition == playingId?.toInt())
                    playFloatButton.setImageResource(R.drawable.ic_baseline_pause_24)
                else
                    playFloatButton.setImageResource(R.drawable.ic_baseline_play_arrow_24)
            }
        }
    }


    /** Contextual Action Mode */
    private fun saveItemStateOnScroll(currentAudio: AudioEntity, holder: MyViewHolder) {
        if (selectedAudioList.contains(currentAudio)) {
            changeAudioRowStyle(holder, R.color.transparent, R.color.colorPrimary)
        } else {
            changeAudioRowStyle(holder, R.color.transparent, R.color.strokeColor)
        }
    }

    override fun onCreateActionMode(actionMode: ActionMode?, menu: Menu?): Boolean {
        actionMode?.let {
            actionMode.menuInflater.inflate(R.menu.audio_list_contextual_menu, menu)
            mActionMode = actionMode
            applyStatusBarColor(R.color.contextualStatusBarColor)
            return true
        }
        return false
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return true
    }

    override fun onActionItemClicked(actionMode: ActionMode?, menu: MenuItem?): Boolean {
        if (menu?.itemId == R.id.delete_audio_menu) {
            val dialog = DeleteAlertDialogFragment.create(this)
            dialog.show(requireActivity.supportFragmentManager, "DeleteAlertDialogFragment")
            requireActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        }
        return true
    }

    override fun onDestroyActionMode(actionMode: ActionMode?) {
        viewHolders.forEach { holder ->
            changeAudioRowStyle(holder, R.color.transparent, R.color.strokeColor)
        }
        multiSelection = false
        selectedAudioList.clear()
        applyStatusBarColor(R.color.statusBarColor)
    }

    private fun applyStatusBarColor(color: Int) {
        requireActivity.window.statusBarColor =
            ContextCompat.getColor(requireActivity, color)
    }

    private fun applySelection(holder: MyViewHolder, currentAudio: AudioEntity) {
        if (selectedAudioList.contains(currentAudio)) {
            selectedAudioList.remove(currentAudio)
            changeAudioRowStyle(holder, R.color.transparent, R.color.strokeColor)
            applyActionModeTitle()
        } else {
            selectedAudioList.add(currentAudio)
            changeAudioRowStyle(holder, R.color.transparent, R.color.colorPrimary)
            applyActionModeTitle()
        }
    }

    private fun changeAudioRowStyle(
        holder: MyViewHolder,
        backgroundColor: Int, strokeColor: Int
    ) {
        holder.binding.audioRowLayout.setBackgroundColor(
            ContextCompat.getColor(requireActivity, backgroundColor)
        )
        holder.binding.rowCardView.strokeColor =
            ContextCompat.getColor(requireActivity, strokeColor)

    }

    private fun showSnackBar(message: String) {
        Snackbar.make(
            requireActivity.window.decorView.findViewById(R.id.main_activity_snack_bar),
            message,
            Snackbar.LENGTH_SHORT
        ).setAction("OK") {}.show()
    }


    private fun applyActionModeTitle() {
        when (selectedAudioList.size) {
            0 -> {
                mActionMode.finish()
                multiSelection = false
            }
            else -> {
                mActionMode.title = "${selectedAudioList.size}個選択中"
            }
        }
    }

    fun clearContextualActionMode() {
        if (this::mActionMode.isInitialized) {
            mActionMode.finish()
        }
    }

    /** DeleteAlertDialogFragmentListenerのメソッド */
    override fun onDialogPositiveClick(dialog: DialogFragment) {
        val currentPlayId =
            playBackViewModel.metadata.replayCache.firstOrNull()?.description?.mediaId?.toInt()
        val selectedAudioIdList =
            List(selectedAudioList.size) { i -> selectedAudioList[i].id }
        if (selectedAudioIdList.contains(currentPlayId))
            playBackViewModel.stop()

        selectedAudioList.forEach { audioEntity ->
            val deleteFilePath = audioEntity.audioUri.lastPathSegment
            if (deleteFilePath !== null)
                File(
                    mainViewModel.getApplication<Application>().filesDir,
                    deleteFilePath
                ).delete()
            mainViewModel.deleteAudio(audioEntity)
        }
        showSnackBar("${selectedAudioList.size}個削除されました")
        multiSelection = false
        selectedAudioList.clear()
        if (this::mActionMode.isInitialized) mActionMode.finish()

        /** クリック処理終了後、即座に回転を許容すると、dialogが閉じないためディレイを持たせる */
        launch(Dispatchers.Main) {
            delay(100)
            requireActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            cancel()
        }

    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
        /** クリック処理終了後、即座に回転を許容すると、dialogが閉じないためディレイを持たせる */
        launch(Dispatchers.Main) {
            delay(100)
            requireActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            cancel()
        }
    }
}

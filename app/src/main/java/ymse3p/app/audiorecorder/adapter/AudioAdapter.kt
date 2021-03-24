package ymse3p.app.audiorecorder.adapter

import android.app.Application
import android.view.*
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ymse3p.app.audiorecorder.R
import ymse3p.app.audiorecorder.data.database.entities.AudioEntity
import ymse3p.app.audiorecorder.databinding.AudioRowLayoutBinding
import ymse3p.app.audiorecorder.util.AudioDiffUtil
import ymse3p.app.audiorecorder.util.Constants.Companion.MEDIA_METADATA_QUEUE
import ymse3p.app.audiorecorder.viewmodels.MainViewModel
import ymse3p.app.audiorecorder.viewmodels.PlayBackViewModel
import java.io.File

class AudioAdapter(
    private val mainViewModel: MainViewModel,
    private val playBackViewModel: PlayBackViewModel,
    private val requireActivity: FragmentActivity
) : RecyclerView.Adapter<AudioAdapter.MyViewHolder>(),
    ActionMode.Callback {

    private var audio = emptyList<AudioEntity>()
    private val viewHolders = mutableListOf<MyViewHolder>()

    class MyViewHolder(
        val binding: AudioRowLayoutBinding,
        private val playBackViewModel: PlayBackViewModel,
        var currentPosition: Int? = null,
    ) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(audioEntity: AudioEntity, position: Int) {
            currentPosition = position
            binding.playFloatButton.setOnClickListener {
                playBackViewModel.viewModelScope.launch {
                    currentPosition?.let { playBackViewModel.requestPlayQueue.emit(it) }
                    cancel()
                }
            }

            if (playBackViewModel.metadata.replayCache.isNotEmpty()) {
                val metadata = playBackViewModel.metadata.replayCache[0]
                val playingId =
                    metadata?.getLong(MEDIA_METADATA_QUEUE)?.toInt()
                if (currentPosition == playingId)
                    binding.playFloatButton.setImageResource(
                        R.drawable.ic_baseline_pause_24
                    )
                else
                    binding.playFloatButton.setImageResource(
                        R.drawable.ic_baseline_play_arrow_24
                    )
            }

            binding.audioEntity = audioEntity
            binding.executePendingBindings()
        }

        companion object {
            fun factory(
                parent: ViewGroup,
                playBackViewModel: PlayBackViewModel,
            ): MyViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = AudioRowLayoutBinding.inflate(layoutInflater, parent, false)
                return MyViewHolder(binding, playBackViewModel)
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val viewHolder = MyViewHolder.factory(parent, playBackViewModel)
        viewHolders.add(viewHolder)
        return viewHolder
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentAudio = audio[position]
        holder.bind(currentAudio, position)
        rootView = holder.itemView.rootView

        saveItemStateOnScroll(currentAudio, holder)

        holder.binding.audioRowLayout.setOnClickListener {
            if (multiSelection) {
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

        playBackViewModel.viewModelScope.launch {
            playBackViewModel.metadata.collect { metadata ->
                viewHolders.forEach { viewHolder ->
                    val playingId =
                        metadata?.getLong(MEDIA_METADATA_QUEUE)?.toInt()
                    viewHolder.binding.run {
                        if (viewHolder.currentPosition == playingId)
                            playFloatButton.setImageResource(
                                R.drawable.ic_baseline_pause_24
                            )
                        else
                            playFloatButton.setImageResource(
                                R.drawable.ic_baseline_play_arrow_24
                            )
                    }
                }
            }
        }
    }

    /** Contextual Action Mode */
    private lateinit var mActionMode: ActionMode
    private var multiSelection = false
    private var selectedAudioList = arrayListOf<AudioEntity>()
    private lateinit var rootView: View

    private fun saveItemStateOnScroll(currentAudio: AudioEntity, holder: MyViewHolder) {
        if (selectedAudioList.contains(currentAudio)) {
            changeAudioRowStyle(holder, R.color.cardBackgroundLightColor, R.color.colorPrimary)
        } else {
            changeAudioRowStyle(holder, R.color.cardBackgroundColor, R.color.strokeColor)
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
            selectedAudioList.forEach { audioEntity ->
                File(
                    mainViewModel.getApplication<Application>().filesDir,
                    audioEntity.audioUri.lastPathSegment
                ).delete()
                mainViewModel.deleteAudio(audioEntity)
            }
            showSnackBar("${selectedAudioList.size}個削除されました")

            multiSelection = false
            selectedAudioList.clear()
            actionMode?.finish()
        }
        return true
    }

    override fun onDestroyActionMode(actionMode: ActionMode?) {
        viewHolders.forEach { holder ->
            changeAudioRowStyle(holder, R.color.cardBackgroundColor, R.color.strokeColor)
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
            changeAudioRowStyle(holder, R.color.cardBackgroundColor, R.color.strokeColor)
            applyActionModeTitle()
        } else {
            selectedAudioList.add(currentAudio)
            changeAudioRowStyle(holder, R.color.cardBackgroundLightColor, R.color.colorPrimary)
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
            requireActivity.window.decorView.rootView,
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
}

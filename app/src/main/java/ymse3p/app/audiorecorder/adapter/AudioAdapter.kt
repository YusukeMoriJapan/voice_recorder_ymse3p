package ymse3p.app.audiorecorder.adapter

import android.support.v4.media.MediaMetadataCompat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ymse3p.app.audiorecorder.R
import ymse3p.app.audiorecorder.data.database.entities.AudioEntity
import ymse3p.app.audiorecorder.databinding.AudioRowLayoutBinding
import ymse3p.app.audiorecorder.util.AudioDiffUtil
import ymse3p.app.audiorecorder.viewmodels.PlayBackViewModel

class AudioAdapter(private val playBackViewModel: PlayBackViewModel) :
    RecyclerView.Adapter<AudioAdapter.MyViewHolder>() {

    private var audio = emptyList<AudioEntity>()
    private val viewHolders = mutableListOf<MyViewHolder>()

    class MyViewHolder(
        val binding: AudioRowLayoutBinding,
        private val playBackViewModel: PlayBackViewModel
    ) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(audioEntity: AudioEntity) {
            binding.playFloatButton.setOnClickListener {
                playBackViewModel.viewModelScope.launch {
                    playBackViewModel.requestPlayNumber.emit(
                        audioEntity.id
                    )
                    cancel()
                }
            }

            val binnedId = audioEntity.id
            playBackViewModel.metadata.replayCache.forEach { metadata ->
                val playingId =
                    metadata?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)?.toInt()
                if (binnedId == playingId)
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
            fun factory(parent: ViewGroup, playBackViewModel: PlayBackViewModel): MyViewHolder {
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
        holder.bind(currentAudio)
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
                        metadata?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
                            ?.toInt()
                    viewHolder.binding.run {
                        if (audioEntity?.id == playingId)
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
}

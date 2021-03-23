package ymse3p.app.audiorecorder.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.findFragment
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ymse3p.app.audiorecorder.data.database.entities.AudioEntity
import ymse3p.app.audiorecorder.databinding.AudioRowLayoutBinding
import ymse3p.app.audiorecorder.util.AudioDiffUtil
import ymse3p.app.audiorecorder.viewmodels.MainViewModel

class AudioAdapter(private val mainViewModel: MainViewModel) :
    RecyclerView.Adapter<AudioAdapter.MyViewHolder>() {
    private var audio = emptyList<AudioEntity>()


    class MyViewHolder(
        private val binding: AudioRowLayoutBinding,
        private val mainViewModel: MainViewModel
    ) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(audioEntity: AudioEntity) {
            binding.playFloatButton.setOnClickListener {

                mainViewModel.viewModelScope.launch {
                    mainViewModel.requestPlayNumber.emit(
                        audioEntity.id
                    )
                    cancel()
                }

            }
            binding.audioEntity = audioEntity
            binding.executePendingBindings()
        }

        companion object {
            fun factory(parent: ViewGroup, mainViewModel: MainViewModel): MyViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = AudioRowLayoutBinding.inflate(layoutInflater, parent, false)
                return MyViewHolder(binding, mainViewModel)
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder.factory(parent, mainViewModel)
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

    }
}
package ymse3p.app.audiorecorder.adapter

import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ymse3p.app.audiorecorder.data.database.entities.AudioEntity
import ymse3p.app.audiorecorder.databinding.AudioRowLayoutBinding
import ymse3p.app.audiorecorder.util.AudioDiffUtil

class AudioAdapter : RecyclerView.Adapter<AudioAdapter.MyViewHolder>() {

    private var audio = emptyList<AudioEntity>()

    class MyViewHolder(private val binding: AudioRowLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(audioEntity: AudioEntity) {
            binding.audioEntity = audioEntity
            binding.executePendingBindings()
        }

        companion object {
            fun factory(parent: ViewGroup): MyViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = AudioRowLayoutBinding.inflate(layoutInflater, parent, false)
                return MyViewHolder(binding)
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder.factory(parent)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentAudio = audio[position]
        holder.bind(currentAudio)
    }

    override fun getItemCount(): Int {
        return audio.size
    }

    fun setData(newData: List<AudioEntity>) {
        val audioDiffUtil = AudioDiffUtil(audio,newData)
        val diffUtilResult = DiffUtil.calculateDiff(audioDiffUtil)
        audio = newData
        diffUtilResult.dispatchUpdatesTo(this)
    }
}
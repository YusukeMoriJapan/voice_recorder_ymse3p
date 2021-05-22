package ymse3p.app.voicelogger.util

import androidx.recyclerview.widget.DiffUtil
import ymse3p.app.voicelogger.data.database.entities.AudioEntity

class AudioDiffUtil(
    private val oldList: List<AudioEntity>,
    private val newList: List<AudioEntity>
) : DiffUtil.Callback() {
    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
package ymse3p.app.audiorecorder

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ymse3p.app.audiorecorder.databinding.FragmentAudioSaveBottomSheetBinding


class AudioSaveBottomSheet : BottomSheetDialogFragment() {

    private lateinit var _binding: FragmentAudioSaveBottomSheetBinding
    private val binding get() = _binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAudioSaveBottomSheetBinding.inflate(inflater, container, false)
        binding.audioSaveButton.setOnClickListener {
            if (validateInputTitle()) {
                Toast.makeText(requireContext(), "入力成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "失敗", Toast.LENGTH_SHORT).show()
            }

        }
        return binding.root
    }

    private fun validateInputTitle(): Boolean {
        val titleInput: String? = binding.textInputAudioTitle.editText?.run {
            text.toString().trim()
        }

        return if (titleInput.isNullOrEmpty()) {
            binding.textInputAudioTitle.error = "タイトルを入力してください"
            false
        } else {
            binding.textInputAudioTitle.apply {
                error = null
                isErrorEnabled = false
            }
            true
        }
    }
}
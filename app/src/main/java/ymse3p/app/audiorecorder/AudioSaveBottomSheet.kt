package ymse3p.app.audiorecorder

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ymse3p.app.audiorecorder.databinding.FragmentAudioSaveBottomSheetBinding
import ymse3p.app.audiorecorder.viewmodels.MainViewModel


class AudioSaveBottomSheet : BottomSheetDialogFragment() {

    private lateinit var _binding: FragmentAudioSaveBottomSheetBinding
    private val binding get() = _binding
    private val mainViewModel by activityViewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAudioSaveBottomSheetBinding.inflate(inflater, container, false)

        binding.audioSaveButton.setOnClickListener {
            validateInputTitle()?.let { validatedInputTitle ->
                mainViewModel.insertAudio(validatedInputTitle)
                Toast.makeText(requireContext(), "入力成功", Toast.LENGTH_SHORT).show()
                val action =
                    AudioSaveBottomSheetDirections.actionAudioSaveBottomSheetToFirstFragment()
                findNavController().navigate(action)
                return@setOnClickListener
            }
            Toast.makeText(requireContext(), "失敗", Toast.LENGTH_SHORT).show()
        }
        return binding.root
    }

    private fun validateInputTitle(): String? {
        val titleInput: String? = binding.textInputAudioTitle.editText?.run {
            text.toString().trim()
        }

        return if (titleInput.isNullOrEmpty()) {
            binding.textInputAudioTitle.error = "タイトルを入力してください"
            null
        } else {
            binding.textInputAudioTitle.apply {
                error = null
                isErrorEnabled = false
            }
            titleInput
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        object : BottomSheetDialog(requireContext(), theme) {
            override fun onBackPressed() {
                Toast.makeText(requireContext(), "「削除ボタン」か「保存ボタン」を押してください", Toast.LENGTH_SHORT)
                    .show()
            }
        }
}
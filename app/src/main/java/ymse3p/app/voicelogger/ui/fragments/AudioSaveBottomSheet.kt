package ymse3p.app.voicelogger.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import ymse3p.app.voicelogger.R
import ymse3p.app.voicelogger.databinding.FragmentAudioSaveBottomSheetBinding
import ymse3p.app.voicelogger.viewmodels.MainViewModel


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
            mainViewModel.insertAudio(validateInputTitle())

            Snackbar.make(
                requireActivity().window.decorView.findViewById(R.id.main_activity_snack_bar),
                "保存に成功しました",
                Snackbar.LENGTH_SHORT
            ).setAction("OK") {}.show()

            val action =
                AudioSaveBottomSheetDirections.actionAudioSaveBottomSheetToFirstFragment()
            findNavController().navigate(action)
            return@setOnClickListener
        }
        return binding.root
    }

    private fun validateInputTitle(): String {
        val titleInput: String? = binding.textInputAudioTitle.editText?.run {
            text.toString().trim()
        }

        return if (titleInput.isNullOrEmpty()) {
//            binding.textInputAudioTitle.error = "タイトルを入力してください"
            ""
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
                Snackbar.make(
                    binding.saveBottomSheetSnackBar,
                    "保存を行ってください",
                    Snackbar.LENGTH_SHORT
                ).setAction("OK") {}.show()
            }
        }
}
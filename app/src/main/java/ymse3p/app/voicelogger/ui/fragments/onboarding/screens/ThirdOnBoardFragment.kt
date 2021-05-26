package ymse3p.app.voicelogger.ui.fragments.onboarding.screens

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ymse3p.app.voicelogger.R
import ymse3p.app.voicelogger.viewmodels.MainViewModel

class ThirdOnBoardFragment : Fragment() {

    private val mainViewModel by activityViewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_third_on_board, container, false)

        val imageView = view.findViewById<ImageView>(R.id.onboard_image_3_1)
        Glide.with(this).load(R.drawable.onboarding_page_3).into(imageView)

        view.findViewById<Button>(R.id.third_onb_finish_button).setOnClickListener {
            mainViewModel.viewModelScope.launch {
                toAudioListFragment()
                cancel()
            }
        }
        return view
    }

    private suspend fun toAudioListFragment() {
        mainViewModel.dataStoreRepository.setIsFirstLaunch(false)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
        mainViewModel.showRecordButton.value = true

        findNavController().navigate(R.id.action_viewPagerFragment_to_FirstFragment)


    }

}
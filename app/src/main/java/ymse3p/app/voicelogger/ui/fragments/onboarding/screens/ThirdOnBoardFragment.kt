package ymse3p.app.voicelogger.ui.fragments.onboarding.screens

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
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

        view.findViewById<Button>(R.id.third_onb_finish_button).setOnClickListener {
            mainViewModel.viewModelScope.launch {
                mainViewModel.dataStoreRepository.setIsFirstLaunch(false)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                findNavController().navigate(R.id.action_viewPagerFragment_to_FirstFragment)
                (requireActivity() as AppCompatActivity).supportActionBar?.show()
                cancel()
            }
        }
        return view
    }

}
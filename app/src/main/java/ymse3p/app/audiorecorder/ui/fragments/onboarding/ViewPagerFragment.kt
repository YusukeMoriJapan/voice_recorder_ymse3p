package ymse3p.app.audiorecorder.ui.fragments.onboarding

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import ymse3p.app.audiorecorder.R
import ymse3p.app.audiorecorder.ui.fragments.onboarding.screens.FirstOnBoardFragment
import ymse3p.app.audiorecorder.ui.fragments.onboarding.screens.SecondOnBoardFragment
import ymse3p.app.audiorecorder.ui.fragments.onboarding.screens.ThirdOnBoardFragment

class ViewPagerFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val rootView = inflater.inflate(R.layout.fragment_view_pager, container, false)

        val fragmentList = listOf<Fragment>(
            FirstOnBoardFragment(),
            SecondOnBoardFragment(),
            ThirdOnBoardFragment()
        )

        val adapter =
            OnBoardPageAdapter(
                fragmentList,
                requireActivity().supportFragmentManager,
                lifecycle
            )

        rootView.findViewById<ViewPager2>(R.id.view_pager).adapter = adapter

        return rootView
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()
    }
}
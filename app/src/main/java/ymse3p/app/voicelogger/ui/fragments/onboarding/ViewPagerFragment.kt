package ymse3p.app.voicelogger.ui.fragments.onboarding

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.viewpager2.widget.ViewPager2
import ymse3p.app.voicelogger.R
import ymse3p.app.voicelogger.ui.fragments.onboarding.screens.FirstOnBoardFragment
import ymse3p.app.voicelogger.ui.fragments.onboarding.screens.SecondOnBoardFragment
import ymse3p.app.voicelogger.ui.fragments.onboarding.screens.ThirdOnBoardFragment

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

        /** Handlerを介さずにViewPagerにアクセスすると実行時例外発生する */
        Handler(Looper.getMainLooper()).post {
            rootView.findViewById<ViewPager2>(R.id.view_pager).adapter = adapter
        }

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        return rootView
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()
    }
}
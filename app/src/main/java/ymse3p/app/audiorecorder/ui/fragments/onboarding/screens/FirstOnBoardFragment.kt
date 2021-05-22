package ymse3p.app.audiorecorder.ui.fragments.onboarding.screens

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.viewpager2.widget.ViewPager2
import ymse3p.app.audiorecorder.R


class FirstOnBoardFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_first_on_board, container, false)
        val viewPager = requireActivity().findViewById<ViewPager2>(R.id.view_pager)

        view.findViewById<Button>(R.id.first_onb_next_button).setOnClickListener {
            viewPager.currentItem = 1
        }
        return view
    }

}
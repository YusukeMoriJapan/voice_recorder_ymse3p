package ymse3p.app.voicelogger.ui.fragments.onboarding.screens

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import ymse3p.app.voicelogger.R

class SecondOnBoardFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_second_on_board, container, false)
        val viewPager = requireActivity().findViewById<ViewPager2>(R.id.view_pager)

        view.findViewById<Button>(R.id.sec_onb_next_button).setOnClickListener {
            viewPager.currentItem = 2
        }

        val imageViewFirst = view.findViewById<ImageView>(R.id.onboard_image_2_1)
        val imageViewSecond = view.findViewById<ImageView>(R.id.onboard_image_2_2)
        val glide = Glide.with(this)

        glide.apply {
            load(R.drawable.onboarding_page_2_1).into(imageViewFirst)
            load(R.drawable.onboarding_page_2_2).into(imageViewSecond)
        }

        return view
    }

}
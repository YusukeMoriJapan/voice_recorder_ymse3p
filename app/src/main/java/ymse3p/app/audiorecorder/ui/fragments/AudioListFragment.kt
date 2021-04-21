package ymse3p.app.audiorecorder.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import ymse3p.app.audiorecorder.R
import ymse3p.app.audiorecorder.adapter.AudioAdapter
import ymse3p.app.audiorecorder.databinding.FragmentAudioListBinding
import ymse3p.app.audiorecorder.viewmodels.MainViewModel
import ymse3p.app.audiorecorder.viewmodels.playbackViewModel.PlayBackViewModel


@AndroidEntryPoint
class AudioListFragment : Fragment() {

    private val mainViewModel by activityViewModels<MainViewModel>()
    private val playBackViewModel by activityViewModels<PlayBackViewModel>()

    private var _binding: FragmentAudioListBinding? = null
    private val binding get() = _binding!!

    private val mAdapter by lazy {
        AudioAdapter(
            mainViewModel,
            playBackViewModel,
            requireActivity()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /** 「ユーザーが選択した音源」の位置情報を表示するフラグメントに遷移 */
        lifecycleScope.launchWhenCreated {
            mAdapter.selectedAudioEntity.collect { audioEntity ->
                if (findNavController().currentDestination?.id == R.id.FirstFragment) {
                    findNavController().navigate(
                        AudioListFragmentDirections.actionFirstFragmentToMapsFragment(audioEntity)
                    )
                }
            }
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAudioListBinding.inflate(layoutInflater, container, false)
        setupRecyclerView()
        readDatabase()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mAdapter.clearContextualActionMode()
        _binding = null
    }


    private fun setupRecyclerView() {
        binding.audioListRecyclerview.adapter = mAdapter
        binding.audioListRecyclerview.layoutManager = LinearLayoutManager(requireContext())
        showShimmerEffect()
    }

    private fun readDatabase() {
        mainViewModel.readAudio.observe(viewLifecycleOwner, { database ->
            mAdapter.setData(database)
            hideShimmerEffect()

        })

    }

    private fun hideShimmerEffect() {
        binding.audioListRecyclerview.hideShimmer()
    }

    private fun showShimmerEffect() {
        binding.audioListRecyclerview.showShimmer()
    }
}
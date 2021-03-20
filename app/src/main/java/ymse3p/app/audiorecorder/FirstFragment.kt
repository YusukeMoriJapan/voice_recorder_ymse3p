package ymse3p.app.audiorecorder

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ymse3p.app.audiorecorder.adapter.AudioAdapter
import ymse3p.app.audiorecorder.databinding.FragmentFirstBinding
import ymse3p.app.audiorecorder.viewmodels.MainViewModel


@AndroidEntryPoint
class FirstFragment : Fragment() {

    private val mainViewModel by activityViewModels<MainViewModel>()

    private lateinit var _binding: FragmentFirstBinding
    private val binding get() = _binding

    private val mAdapter by lazy { AudioAdapter() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(layoutInflater, container, false)
        setupRecyclerView()
        readDatabase()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.button_first).setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
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
package ymse3p.app.audiorecorder.ui.fragments

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import ymse3p.app.audiorecorder.R
import ymse3p.app.audiorecorder.adapter.AudioAdapter
import ymse3p.app.audiorecorder.databinding.FragmentAudioListBinding
import ymse3p.app.audiorecorder.util.FilteringMode
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

    private lateinit var onCreateViewJob: Job

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

        setHasOptionsMenu(true)

        onCreateViewJob = lifecycleScope.launchWhenCreated {
            mainViewModel.isInserting.collect { isInserting ->
                if (isInserting) showShimmerEffect()
                else hideShimmerEffect()
            }
        }

        mainViewModel.repository.localDataSource.submitQuery("%%")

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::onCreateViewJob.isInitialized) onCreateViewJob.cancel()
        mAdapter.clearContextualActionMode()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        mAdapter.viewHolders.forEach { viewHolder ->
            viewHolder.binding.rowMapViewStart.onDestroy()
            viewHolder.binding.rowMapViewEnd.onDestroy()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)

        val search = menu.findItem(R.id.menu_search)
        val searchView = (search.actionView as? SearchView)

        lifecycleScope.launchWhenResumed {
            mainViewModel.filteringMode.collect { mode ->
                when (mode) {
                    FilteringMode.ADDRESS -> {
                        searchView?.queryHint = "録音地点で絞込"
                    }
                    FilteringMode.TITLE -> {
                        searchView?.queryHint = "タイトルで絞込"
                    }
                    FilteringMode.DATE -> {
                        searchView?.queryHint = "日時で絞込"
                    }
                }
            }
        }

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(query: String?): Boolean {
                if (query != null)
                    mainViewModel.repository.localDataSource.submitQuery("%${query}%")
                return false
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sample_data_generate -> {
                mainViewModel.insertSampleAudio()
                true
            }
            R.id.all_sample_data_delete -> {
                mainViewModel.deleteAllSampleAudio()
                true
            }
            R.id.delete_all_data -> {
                mainViewModel.deleteAllAudio()
                true
            }
            R.id.search_filtering -> {
                val menuItemView = requireActivity().findViewById<View>(R.id.search_filtering)
                val popupMenu = PopupMenu(requireContext(), menuItemView).apply {
                    inflate(R.menu.search_filtering_menu)
                }

                popupMenu.setOnMenuItemClickListener { searchFiltItem ->
                    return@setOnMenuItemClickListener when (searchFiltItem.itemId) {
                        R.id.filter_address -> {
                            mainViewModel.filteringMode.value = FilteringMode.ADDRESS
                            true
                        }
                        R.id.filter_title -> {
                            mainViewModel.filteringMode.value = FilteringMode.TITLE
                            true
                        }
                        R.id.filter_date -> {
                            mainViewModel.filteringMode.value = FilteringMode.DATE
                            true
                        }
                        else -> true
                    }
                }
                popupMenu.show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun setupRecyclerView() {
        binding.audioListRecyclerview.apply {
            adapter = mAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
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
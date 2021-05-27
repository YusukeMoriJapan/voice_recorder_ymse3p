package ymse3p.app.voicelogger.ui.fragments

import android.app.DatePickerDialog
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import ymse3p.app.voicelogger.R
import ymse3p.app.voicelogger.adapter.AudioAdapter
import ymse3p.app.voicelogger.databinding.FragmentAudioListBinding
import ymse3p.app.voicelogger.ui.fragments.dialogs.DeleteAlertDialogFragment
import ymse3p.app.voicelogger.util.Constants.Companion.DATABASE_DATE_FORMAT
import ymse3p.app.voicelogger.util.FilteringMode
import ymse3p.app.voicelogger.viewmodels.MainViewModel
import ymse3p.app.voicelogger.viewmodels.playbackViewModel.PlayBackViewModel
import java.text.SimpleDateFormat
import java.util.*


@AndroidEntryPoint
class AudioListFragment : Fragment() {

    private val mainViewModel by activityViewModels<MainViewModel>()
    private val playBackViewModel by activityViewModels<PlayBackViewModel>()

    private var _binding: FragmentAudioListBinding? = null
    private val binding get() = _binding!!

    private var lowerDate: String? = null
    private var upperDate: String? = null
    private var queryText: String? = ""

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

        lifecycleScope.launchWhenCreated {
            mainViewModel.dataStoreRepository.isFirstLaunch.first {
                if (it == null || it == true)
                    if (findNavController().currentDestination?.id == R.id.FirstFragment) {
                        mainViewModel.showRecordButton.value = false
                        findNavController().navigate(R.id.action_FirstFragment_to_viewPagerFragment)
                    }
                true
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

            override fun onQueryTextChange(rawQuery: String?): Boolean {
                queryText = rawQuery?.trim()
                if (queryText != null) submitQuery()
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
                val dialog = DeleteAlertDialogFragment.create(object :
                    DeleteAlertDialogFragment.DeleteAlertDialogListener {
                    override fun onDialogPositiveClick(dialog: DialogFragment) {
                        mainViewModel.deleteAllSampleAudio()
                        acceptScreenRotate()
                    }

                    override fun onDialogNegativeClick(dialog: DialogFragment) {
                        acceptScreenRotate()
                    }
                })
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
                dialog.show(parentFragmentManager, null)
                true
            }
            R.id.delete_all_data -> {
                val dialog = DeleteAlertDialogFragment.create(object :
                    DeleteAlertDialogFragment.DeleteAlertDialogListener {
                    override fun onDialogPositiveClick(dialog: DialogFragment) {
                        mainViewModel.deleteAllAudio()
                        acceptScreenRotate()
                    }

                    override fun onDialogNegativeClick(dialog: DialogFragment) {
                        acceptScreenRotate()
                    }
                })
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
                dialog.show(parentFragmentManager, null)
                true
            }
            R.id.search_filtering -> {
                val menuItemView = requireActivity().findViewById<View>(R.id.search_filtering)
                val popupMenu = PopupMenu(requireContext(), menuItemView).apply {
                    inflate(R.menu.search_filtering_menu)
                }

                popupMenu.setOnMenuItemClickListener { searchFilterItem ->
                    return@setOnMenuItemClickListener when (searchFilterItem.itemId) {
                        R.id.filter_address -> {
                            mainViewModel.filteringMode.value = FilteringMode.ADDRESS
                            true
                        }
                        R.id.filter_title -> {
                            mainViewModel.filteringMode.value = FilteringMode.TITLE
                            true
                        }
                        R.id.filter_date_lower -> {
                            showDatePicker { date ->
                                lowerDate = date
                                submitQuery()
                            }
                            true
                        }
                        R.id.filter_date_upper -> {
                            showDatePicker { date ->
                                upperDate = date
                                submitQuery()
                            }
                            true
                        }
                        R.id.filter_date_reset -> {
                            lowerDate = null
                            upperDate = null
                            submitQuery()
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

    private fun submitQuery() {
        mainViewModel.repository.localDataSource.apply {
            when (mainViewModel.filteringMode.value) {
                FilteringMode.ADDRESS -> {
                    submitQuery(
                        address = "%$queryText%",
                        lowerDate = lowerDate,
                        upperDate = upperDate
                    )
                }
                FilteringMode.TITLE -> {
                    submitQuery(
                        title = "%$queryText%",
                        lowerDate = lowerDate,
                        upperDate = upperDate
                    )
                }
                FilteringMode.DATE -> {
                    /** 未実装 */
                }
            }
        }
    }

    private fun showDatePicker(setDateRangeAndSubmit: (String) -> Unit) {
        val now = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            requireContext(),
            { view, year, month, dayOfMonth ->
                val format = SimpleDateFormat(DATABASE_DATE_FORMAT)
                val selectedDate = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }

                setDateRangeAndSubmit(format.format(selectedDate.time))
            },
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()

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

    fun acceptScreenRotate() {
        lifecycleScope.launchWhenCreated {
            delay(100)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            cancel()
        }
    }
}

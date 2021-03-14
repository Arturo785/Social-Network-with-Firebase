package com.example.socialapp.ui.main.fragments

import android.os.Bundle
import android.text.Editable
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.socialapp.R
import com.example.socialapp.adapters.UserAdapter
import com.example.socialapp.other.EventObserver
import com.example.socialapp.other.SEARCH_TIME_DELAY
import com.example.socialapp.ui.main.viewmodels.SearchViewModel
import com.example.socialapp.ui.snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SearchFragment : Fragment(R.layout.fragment_search){

    @Inject
    lateinit var userAdapter : UserAdapter

    private val viewModel : SearchViewModel by viewModels()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        subscribeToObservers()

        var job : Job?  = null

        // to add delay when the a query is typed
        etSearch.addTextChangedListener { editable ->
            job?.cancel() // cancels the actual job
            job = lifecycleScope.launch {
                delay(SEARCH_TIME_DELAY)
                editable?.let {
                    viewModel.searchUser(it.toString())
                    // triggers the live data that will be observed
                }
            }
        }

        userAdapter.setOnUserClickListener {user ->
            findNavController().navigate(SearchFragmentDirections.globalActionToOthersProfileFragment(
                user.uid
            ))
        }
    }

    private fun subscribeToObservers() {
        viewModel.searchResults.observe(viewLifecycleOwner, EventObserver(
            onError = {
                searchProgressBar.isVisible = false
                snackbar(it)
            },
            onLoading = {
                searchProgressBar.isVisible = true
            }
        ){  users ->
            searchProgressBar.isVisible = false
            userAdapter.users = users
        })
    }

    private fun setupRecyclerView() = rvSearchResults.apply {
        layoutManager = LinearLayoutManager(requireContext())
        adapter = userAdapter
        itemAnimator = null
    }
}
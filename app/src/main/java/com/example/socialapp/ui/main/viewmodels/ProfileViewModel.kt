package com.example.socialapp.ui.main.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.socialapp.data.entities.Post
import com.example.socialapp.data.entities.User
import com.example.socialapp.data.pagingSource.ProfilePostsPagingSource
import com.example.socialapp.other.Event
import com.example.socialapp.other.PAGER_SIZE
import com.example.socialapp.other.Resource
import com.example.socialapp.repositories.main.MainRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ProfileViewModel @ViewModelInject constructor(
    private val repository: MainRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : BasePostViewModel(repository, dispatcher){


    private val _profileMeta = MutableLiveData<Event<Resource<User>>>()
    val profileMeta : LiveData<Event<Resource<User>>> = _profileMeta

    private val _followStatus = MutableLiveData<Event<Resource<Boolean>>>()
    val followStatus : LiveData<Event<Resource<Boolean>>> = _followStatus


    fun toggleFollowForUser(uid: String) {
        _followStatus.postValue(Event(Resource.Loading()))

        viewModelScope.launch(dispatcher) {
            // this is already a resource
            val result = repository.toggleFollowForUser(uid)
            _followStatus.postValue(Event(result))
        }
    }

    fun loadProfile(uid: String) {
        _profileMeta.postValue(Event(Resource.Loading()))

        viewModelScope.launch(dispatcher) {
            // this is already a resource
            val result = repository.getUser(uid)
            _profileMeta.postValue(Event(result))
        }
    }

    fun getPagingFlow(uid : String) : Flow<PagingData<Post>>{
        val pagingSource = ProfilePostsPagingSource(FirebaseFirestore.getInstance(),
            uid
        )
        return Pager(PagingConfig(PAGER_SIZE)){
            pagingSource
        }.flow.cachedIn(viewModelScope)
    }

}
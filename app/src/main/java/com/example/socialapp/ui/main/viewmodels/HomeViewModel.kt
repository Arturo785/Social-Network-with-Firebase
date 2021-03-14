package com.example.socialapp.ui.main.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.socialapp.data.entities.Post
import com.example.socialapp.other.Event
import com.example.socialapp.other.Resource
import com.example.socialapp.repositories.main.MainRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel @ViewModelInject constructor(
    private val repository : MainRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : BasePostViewModel(repository, dispatcher){

    private val _posts = MutableLiveData<Event<Resource<List<Post>>>>()

    init {
        getPosts()
    }

    // the one to be observed
    override val posts: LiveData<Event<Resource<List<Post>>>>
        get() = _posts

    override fun getPosts(uid: String) {
        _posts.postValue(Event(Resource.Loading()))

        viewModelScope.launch(dispatcher) {
            val result = repository.getPostForFollows()
            _posts.postValue(Event(result))
        }
    }

}
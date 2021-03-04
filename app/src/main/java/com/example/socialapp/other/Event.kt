package com.example.socialapp.other

import androidx.lifecycle.Observer

class Event<T>(private val content : T) {

    var hasBeenHandled = false
        private set // only be read from outside

    fun getContentIfNotHandled() : T? {
        return if(!hasBeenHandled){
            hasBeenHandled = true
            content
        }
        else{
            null
        }
    }

    fun peekContent() = content

}

// receives 3 lambadas
class EventObserver<T>(
    private inline val onError: ((String) -> Unit)? = null,
    private inline val onLoading: (() -> Unit)? = null,
    private inline val onSuccess: ((T) -> Unit)
) : Observer<Event<Resource<T>>> {

    // this is inherited from Observer
    override fun onChanged(t: Event<Resource<T>>?) {
        when(val content = t?.peekContent()) {

            is Resource.Success -> {
                content.data?.let {
                    onSuccess(it)
                }
            }

            is Resource.Error -> {
                // this one consumes the event
                t.getContentIfNotHandled()?.let {
                    onError?.let { error -> // the content is the actual lambda
                        error(it.message!!) //calls the fun
                    }
                }
            }

            is Resource.Loading -> {
                onLoading?.let { loading -> // the content is the actual lambda
                    loading() // calls the fun
                }
            }
        }
    }

}


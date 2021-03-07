package com.example.socialapp.ui.auth

import android.content.Context
import android.util.Patterns
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialapp.R
import com.example.socialapp.other.*
import com.example.socialapp.repositories.auth.AuthRepository
import com.google.firebase.auth.AuthResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// needed to inject the viewModel with our dependencies
class AuthViewModel @ViewModelInject constructor(
    private val repository: AuthRepository,
    private val applicationContext: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main // default if not provided
) : ViewModel(){

    // an event can only be handled once, the resource has 3 states, T type will be AuthResult
    private val _registerStatus = MutableLiveData<Event<Resource<AuthResult>>> () // private and can change

    private val _loginStatus = MutableLiveData<Event<Resource<AuthResult>>> () // private and can change

    // can not change and to be observed
    val registerStatus: LiveData<Event<Resource<AuthResult>>> = _registerStatus

    val loginStatus: LiveData<Event<Resource<AuthResult>>> = _loginStatus


    fun register(email : String, username : String, password : String, repeatedPassword : String){

        val error = if(email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            applicationContext.getString(R.string.error_input_empty)
        }
        else if(password != repeatedPassword) {
            applicationContext.getString(R.string.error_incorrectly_repeated_password)
        }
        else if(username.length < MIN_USERNAME_LENGTH) {
            applicationContext.getString(R.string.error_username_too_short, MIN_USERNAME_LENGTH)
        }
        else if(username.length > MAX_USERNAME_LENGTH) {
            applicationContext.getString(R.string.error_username_too_long, MAX_USERNAME_LENGTH)
        }
        else if(password.length < MIN_PASSWORD_LENGTH) {
            applicationContext.getString(R.string.error_password_too_short)
        }
        else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            applicationContext.getString(R.string.error_not_a_valid_email)
        }
        else null


        error?.let {
            // there was an error
            _registerStatus.postValue(Event(Resource.Error(message = it)))
            return
        }
        // no error
        _registerStatus.postValue(Event(Resource.Loading()))

        // this scope gets destroyed with the viewModel
        viewModelScope.launch(dispatcher) {
            val result = repository.register(email, username, password)
            // an event can only be handled once, the resource has 3 states, T type will be AuthResult
            _registerStatus.postValue(Event(result))
        }
    }

    fun login(email: String, password: String){
        if(email.isEmpty() || password.isEmpty()) {
            val error = applicationContext.getString(R.string.error_input_empty)
            _loginStatus.postValue(Event(Resource.Error(message = error)))
        }
        else {
            _loginStatus.postValue(Event(Resource.Loading()))
            viewModelScope.launch(dispatcher) {
                val result = repository.login(email, password)
                _loginStatus.postValue(Event(result))
            }
        }
    }

}